/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.p4runtime;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.util.SharedExecutors;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeActionProfileGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeActionProfileMemberMirror;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileGroupHandle;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberHandle;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.service.PiGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Implementation of GroupProgrammable to handle action profile groups in
 * P4Runtime.
 */
public class P4RuntimeActionGroupProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements GroupProgrammable {

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    private static final String READ_ACTION_GROUPS_FROM_MIRROR = "actionGroupReadFromMirror";
    private static final boolean DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR = false;

    protected GroupStore groupStore;
    private P4RuntimeActionProfileGroupMirror groupMirror;
    private P4RuntimeActionProfileMemberMirror memberMirror;
    private PiGroupTranslator groupTranslator;

    @Override
    protected boolean setupBehaviour() {
        if (!super.setupBehaviour()) {
            return false;
        }
        groupMirror = this.handler().get(P4RuntimeActionProfileGroupMirror.class);
        memberMirror = this.handler().get(P4RuntimeActionProfileMemberMirror.class);
        groupStore = handler().get(GroupStore.class);
        groupTranslator = translationService.groupTranslator();
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId,
                                      GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }

        groupOps.operations().stream()
                .filter(op -> !op.groupType().equals(GroupDescription.Type.ALL))
                .forEach(op -> {
                    // ONOS-7785 We need the group app cookie (which includes
                    // the action profile ID) but this is not part of the
                    // GroupDescription.
                    Group groupOnStore = groupStore.getGroup(deviceId, op.groupId());
                    if (groupOnStore == null) {
                        log.warn("Unable to find group {} in store, aborting {} operation",
                                 op.groupId(), op.opType());
                        return;
                    }
                    GroupDescription groupDesc = new DefaultGroupDescription(
                            deviceId, op.groupType(), op.buckets(), groupOnStore.appCookie(),
                            op.groupId().id(), groupOnStore.appId());
                    DefaultGroup groupToApply = new DefaultGroup(op.groupId(), groupDesc);
                    processPdGroup(groupToApply, op.opType());
                });
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        if (driverBoolProperty(READ_ACTION_GROUPS_FROM_MIRROR,
                               DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR)) {
            return getGroupsFromMirror();
        }

        // Dump groups and members from device for all action profiles.
        final P4RuntimeReadClient.ReadRequest request = client.read(pipeconf);
        pipeconf.pipelineModel().actionProfiles()
                .stream().map(PiActionProfileModel::id)
                .forEach(id -> request.actionProfileGroups(id)
                        .actionProfileMembers(id));
        final P4RuntimeReadClient.ReadResponse response = request.submitSync();

        if (!response.isSuccess()) {
            // Error at client level.
            return Collections.emptyList();
        }

        final Collection<PiActionProfileGroup> groupsOnDevice = response.all(
                PiActionProfileGroup.class);
        final Map<PiActionProfileMemberHandle, PiActionProfileMember> membersOnDevice =
                response.all(PiActionProfileMember.class).stream()
                        .collect(toMap(m -> m.handle(deviceId), m -> m));

        // Sync mirrors.
        groupMirror.sync(deviceId, groupsOnDevice);
        memberMirror.sync(deviceId, membersOnDevice.values());

        // Retrieve the original PD group before translation.
        final List<Group> result = Lists.newArrayList();
        final List<PiActionProfileGroup> groupsToRemove = Lists.newArrayList();
        final Set<PiActionProfileMemberHandle> memberHandlesToKeep = Sets.newHashSet();
        for (PiActionProfileGroup piGroup : groupsOnDevice) {
            final Group pdGroup = checkAndForgeGroupEntry(piGroup, membersOnDevice);
            if (pdGroup == null) {
                // Entry is on device but unknown to translation service or
                // device mirror. Inconsistent. Mark for removal.
                groupsToRemove.add(piGroup);
            } else {
                result.add(pdGroup);
                // Keep track of member handles used in groups.
                piGroup.members().stream()
                        .map(m -> PiActionProfileMemberHandle.of(
                                deviceId, piGroup.actionProfile(), m.id()))
                        .forEach(memberHandlesToKeep::add);
            }
        }

        // Trigger clean up of inconsistent groups and members (if any). This
        // process takes care of removing any orphan member, e.g. from a
        // partial/unsuccessful group insertion.
        // This will update the mirror accordingly.
        final Set<PiActionProfileMemberHandle> memberHandlesToRemove = Sets.difference(
                membersOnDevice.keySet(), memberHandlesToKeep);
        final Set<PiActionProfileGroupHandle> groupHandlesToRemove = groupsToRemove
                .stream().map(g -> g.handle(deviceId)).collect(toSet());
        if (groupHandlesToRemove.size() + memberHandlesToRemove.size() > 0) {
            log.warn("Cleaning up {} action profile groups and " +
                             "{} members on {}...",
                     groupHandlesToRemove.size(), memberHandlesToRemove.size(), deviceId);
            SharedExecutors.getSingleThreadExecutor().execute(
                    () -> submitWriteRequestAndUpdateMirror(
                            client.write(pipeconf)
                                    .delete(groupHandlesToRemove)
                                    .delete(memberHandlesToRemove)));
        }

        // Done.
        return result;
    }

    private Collection<Group> getGroupsFromMirror() {
        final Map<PiActionProfileMemberHandle, PiActionProfileMember> members =
                memberMirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .collect(toMap(e -> e.handle(deviceId), e -> e));
        return groupMirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .map(g -> checkAndForgeGroupEntry(
                        g, members))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Group checkAndForgeGroupEntry(
            PiActionProfileGroup piGroupOnDevice,
            Map<PiActionProfileMemberHandle, PiActionProfileMember> membersOnDevice) {
        final PiActionProfileGroupHandle handle = PiActionProfileGroupHandle.of(
                deviceId, piGroupOnDevice);
        final Optional<PiTranslatedEntity<Group, PiActionProfileGroup>>
                translatedEntity = groupTranslator.lookup(handle);
        final TimedEntry<PiActionProfileGroup> mirrorEntry = groupMirror.get(handle);
        // Check that entry obtained from device is consistent with what is known
        // by the translation store.
        if (!translatedEntity.isPresent()) {
            log.warn("Group not found in translation store: {}", handle);
            return null;
        }
        final PiActionProfileGroup piGroupFromStore = translatedEntity.get().translated();
        if (!piGroupFromStore.equals(piGroupOnDevice)) {
            log.warn("Group on device {} is different from the one in " +
                             "translation store: {} [device={}, store={}]",
                     deviceId, handle, piGroupOnDevice, piGroupFromStore);
            return null;
        }
        // Groups in P4Runtime contains only a reference to members. Check that
        // the actual member instances in the translation store are the same
        // found on the device.
        if (!validateGroupMembers(piGroupFromStore, membersOnDevice)) {
            log.warn("Group on device {} refers to members that are different " +
                             "than those found in translation store: {}", handle);
            return null;
        }
        if (mirrorEntry == null) {
            log.warn("Group handle not found in device mirror: {}", handle);
            return null;
        }
        // Check that members from device are the same as in the translated group.
        return addedGroup(translatedEntity.get().original(), mirrorEntry.lifeSec());
    }

    private boolean validateGroupMembers(
            PiActionProfileGroup piGroupFromStore,
            Map<PiActionProfileMemberHandle, PiActionProfileMember> membersOnDevice) {
        final Collection<PiActionProfileMember> groupMembers =
                extractAllMemberInstancesOrNull(piGroupFromStore);
        if (groupMembers == null) {
            return false;
        }
        return groupMembers.stream().allMatch(
                memberFromStore -> memberFromStore.equals(membersOnDevice.get(
                        memberFromStore.handle(deviceId))));
    }

    private Group addedGroup(Group original, long life) {
        final DefaultGroup forgedGroup = new DefaultGroup(original.id(), original);
        forgedGroup.setState(Group.GroupState.ADDED);
        forgedGroup.setLife(life);
        return forgedGroup;
    }

    private void processPdGroup(Group pdGroup, GroupOperation.Type opType) {
        final PiActionProfileGroup piGroup;
        try {
            piGroup = groupTranslator.translate(pdGroup, pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable to translate group, aborting {} operation: {} [{}]",
                     opType, e.getMessage(), pdGroup);
            return;
        }
        final Operation operation = opType.equals(GroupOperation.Type.DELETE)
                ? Operation.REMOVE : Operation.APPLY;
        final PiActionProfileGroupHandle handle = piGroup.handle(deviceId);
        if (writePiGroupOnDevice(piGroup, handle, operation)) {
            if (operation.equals(Operation.APPLY)) {
                groupTranslator.learn(handle, new PiTranslatedEntity<>(
                        pdGroup, piGroup, handle));
            } else {
                groupTranslator.forget(handle);
            }
        }
    }

    private boolean writePiGroupOnDevice(
            PiActionProfileGroup group,
            PiActionProfileGroupHandle groupHandle,
            Operation operation) {
        // Generate a write request to write both members and groups. Return
        // true if request is successful or if there's no need to write on
        // device (according to mirror state), otherwise, return false.
        final Collection<PiActionProfileMember> members = extractAllMemberInstancesOrNull(group);
        if (members == null) {
            return false;
        }
        final P4RuntimeWriteClient.WriteRequest request = client.write(pipeconf);
        // FIXME: when operation is remove, should we remove members first? Same
        //  thing when modifying a group, should we first modify the group then
        //  remove the member?
        final boolean allMembersSkipped = members.stream()
                .allMatch(m -> appendEntityToWriteRequestOrSkip(
                        request, m.handle(deviceId), m, memberMirror, operation));
        final boolean groupSkipped = appendEntityToWriteRequestOrSkip(
                request, groupHandle, group, groupMirror, operation);
        if (allMembersSkipped && groupSkipped) {
            return true;
        } else {
            // True if all entities in the request (groups and members) where
            // written successfully.
            return submitWriteRequestAndUpdateMirror(request).isSuccess();
        }
    }

    private <H extends PiHandle, E extends PiEntity> boolean appendEntityToWriteRequestOrSkip(
            P4RuntimeWriteClient.WriteRequest writeRequest,
            H handle,
            E entityToApply,
            P4RuntimeMirror<H, E> mirror,
            Operation operation) {
        // Should return true if there's no need to write entity on device,
        // false if the write request is modified or an error occurs.
        final TimedEntry<E> entityOnDevice = mirror.get(handle);
        switch (operation) {
            case APPLY:
                if (entityOnDevice == null) {
                    writeRequest.insert(entityToApply);
                } else if (entityToApply.equals(entityOnDevice.entry())) {
                    // Skip writing if group is unchanged.
                    return true;
                } else {
                    writeRequest.modify(entityToApply);
                }
                break;
            case REMOVE:
                if (entityOnDevice == null) {
                    // Skip deleting if group does not exist on device.
                    return true;
                } else {
                    writeRequest.delete(handle);
                }
                break;
            default:
                log.error("Unrecognized operation {}", operation);
                break;
        }
        return false;
    }

    private Collection<PiActionProfileMember> extractAllMemberInstancesOrNull(
            PiActionProfileGroup group) {
        final Collection<PiActionProfileMember> instances = group.members().stream()
                .map(PiActionProfileGroup.WeightedMember::instance)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (instances.size() != group.members().size()) {
            log.error("PiActionProfileGroup has {} member references, " +
                              "but only {} instances were found",
                      group.members().size(), instances.size());
            return null;
        }
        return instances;
    }

    private P4RuntimeWriteClient.WriteResponse submitWriteRequestAndUpdateMirror(
            P4RuntimeWriteClient.WriteRequest request) {
        final P4RuntimeWriteClient.WriteResponse response = request.submitSync();
        groupMirror.replayWriteResponse(response);
        memberMirror.replayWriteResponse(response);
        return response;
    }

    enum Operation {
        APPLY, REMOVE
    }
}
