/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.k8snetworking.impl;

import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodStore;
import org.onosproject.k8snetworking.api.K8sPodStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_CREATED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_REMOVED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes pod store using consistent map.
 */
@Component(immediate = true, service = K8sPodStore.class)
public class DistributedK8sPodStore
        extends AbstractStore<K8sPodEvent, K8sPodStoreDelegate>
        implements K8sPodStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_POD = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Pod.class)
            .register(ObjectMeta.class)
            .register(PodSpec.class)
            .register(PodStatus.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Pod> podMapListener = new K8sPodMapListener();

    private ConsistentMap<String, Pod> podStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        podStore = storageService.<String, Pod>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_POD))
                .withName("k8s-pod-store")
                .withApplicationId(appId)
                .build();

        podStore.addListener(podMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        podStore.removeListener(podMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createPod(Pod pod) {
        podStore.compute(pod.getMetadata().getUid(), (uid, existing) -> {
            final String error = pod.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return pod;
        });
    }

    @Override
    public void updatePod(Pod pod) {
        podStore.compute(pod.getMetadata().getUid(), (uid, existing) -> {
            final String error = pod.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return pod;
        });
    }

    @Override
    public Pod removePod(String uid) {
        Versioned<Pod> pod = podStore.remove(uid);
        if (pod == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return pod.value();
    }

    @Override
    public Pod pod(String uid) {
        return podStore.asJavaMap().get(uid);
    }

    @Override
    public Set<Pod> pods() {
        return ImmutableSet.copyOf(podStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        podStore.clear();
    }

    private class K8sPodMapListener implements MapEventListener<String, Pod> {

        @Override
        public void event(MapEvent<String, Pod> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes pod created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sPodEvent(
                                    K8S_POD_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes pod updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sPodEvent(
                                    K8S_POD_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes pod removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sPodEvent(
                                    K8S_POD_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
