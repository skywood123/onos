/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mcast.api;

import com.google.common.annotations.Beta;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Entity responsible for storing multicast state information.
 */
@Beta
public interface McastStore extends Store<McastEvent, McastStoreDelegate> {

    /**
     * Updates the store with the route information.
     *
     * @param route a multicast route
     */
    void storeRoute(McastRoute route);

    /**
     * Updates the store with the route information.
     *
     * @param route a multicast route
     */
    void removeRoute(McastRoute route);

    /**
     * Add to the store with source information for the given route.
     *
     * @param route   a multicast route
     * @param sources a set of sources
     */
    void storeSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Removes from the store all the sources information for a given route.
     *
     * @param route a multicast route
     */
    void removeSources(McastRoute route);

    /**
     * Removes from the store the source information for the given route.
     * value.
     *
     * @param route   a multicast route
     * @param sources a set of sources
     */
    void removeSources(McastRoute route, Set<ConnectPoint> sources);

    /**
     * Updates the store with a host based sink information for a given route. There may be
     * multiple sink connect points for the given host.
     *
     * @param route  a multicast route
     * @param hostId the host sink
     * @param sinks  the sinks
     */
    void addSink(McastRoute route, HostId hostId, Set<ConnectPoint> sinks);

    /**
     * Updates the store with sinks information for a given route.
     * The sinks stored with this method are not tied with any host.
     * Traffic will be sent to all of them.
     *
     * @param route a multicast route
     * @param sinks set of specific connect points
     */
    void addSinks(McastRoute route, Set<ConnectPoint> sinks);

    /**
     * Removes from the store all the sink information for a given route.
     *
     * @param route a multicast route
     */
    void removeSinks(McastRoute route);

    /**
     * Removes from the store the complete set of sink information for a given host for a given route.
     *
     * @param route  a multicast route
     * @param hostId a specific host
     */
    void removeSink(McastRoute route, HostId hostId);

    /**
     * Removes from the store the given set of sink information for a given host for a given route.
     *
     * @param route  a multicast route
     * @param hostId the host
     * @param sinks  a set of multicast sink connect points
     */
    void removeSinks(McastRoute route, HostId hostId, Set<ConnectPoint> sinks);

    /**
     * Removes from the store the set of non host bind sink information for a given route.
     *
     * @param route a multicast route
     * @param sinks a set of multicast sinks
     */
    void removeSinks(McastRoute route, Set<ConnectPoint> sinks);

    /**
     * Obtains the sources for a multicast route.
     *
     * @param route a multicast route
     * @return a connect point
     */
    Set<ConnectPoint> sourcesFor(McastRoute route);

    /**
     * Obtains the sinks for a multicast route.
     *
     * @param route a multicast route
     * @return a set of sinks
     */
    Set<ConnectPoint> sinksFor(McastRoute route);

    /**
     * Obtains the sinks for a given host for a given multicast route.
     *
     * @param route  a multicast route
     * @param hostId the host
     * @return a set of sinks
     */
    Set<ConnectPoint> sinksFor(McastRoute route, HostId hostId);


    /**
     * Gets the set of all known multicast routes.
     *
     * @return set of multicast routes.
     */
    Set<McastRoute> getRoutes();

    /**
     * Gets the multicast data for a given route.
     *
     * @param route the route
     * @return set of multicast routes.
     */
    McastRouteData getRouteData(McastRoute route);
}
