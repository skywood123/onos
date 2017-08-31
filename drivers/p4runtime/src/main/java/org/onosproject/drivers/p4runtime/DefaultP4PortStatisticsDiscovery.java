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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiCounterId;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of a PortStatisticsBehaviour that can be used for any P4 program based on default.p4 (i.e. those
 * under onos/tools/test/p4src).
 */
public class DefaultP4PortStatisticsDiscovery extends AbstractP4RuntimeHandlerBehaviour
        implements PortStatisticsDiscovery {

    private static final PiCounterId INGRESS_COUNTER_ID = PiCounterId.of("ingress_port_counter");
    private static final PiCounterId EGRESS_COUNTER_ID = PiCounterId.of("egress_port_counter");

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {

        if (!super.setupBehaviour()) {
            return Collections.emptyList();
        }

        Map<Long, DefaultPortStatistics.Builder> portStatBuilders = Maps.newHashMap();

        deviceService.getPorts(deviceId)
                .forEach(p -> portStatBuilders.put(p.number().toLong(),
                                                   DefaultPortStatistics.builder()
                                                           .setPort((int) p.number().toLong())
                                                           .setDeviceId(deviceId)));

        Set<PiCounterCellId> counterCellIds = Sets.newHashSet();
        portStatBuilders.keySet().forEach(p -> {
            // Counter cell/index = port number.
            counterCellIds.add(PiCounterCellId.of(INGRESS_COUNTER_ID, p));
            counterCellIds.add(PiCounterCellId.of(EGRESS_COUNTER_ID, p));
        });

        Collection<PiCounterCellData> counterEntryResponse;
        try {
            counterEntryResponse = client.readCounterCells(counterCellIds, pipeconf).get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception while reading port counters from {}: {}", deviceId, e.toString());
            log.debug("", e);
            return Collections.emptyList();
        }

        counterEntryResponse.forEach(counterEntry -> {
            if (!portStatBuilders.containsKey(counterEntry.cellId().index())) {
                log.warn("Unrecognized counter index {}, skipping", counterEntry);
                return;
            }
            DefaultPortStatistics.Builder statsBuilder = portStatBuilders.get(counterEntry.cellId().index());
            if (counterEntry.cellId().counterId().equals(INGRESS_COUNTER_ID)) {
                statsBuilder.setPacketsReceived(counterEntry.packets());
                statsBuilder.setBytesReceived(counterEntry.bytes());
            } else if (counterEntry.cellId().counterId().equals(EGRESS_COUNTER_ID)) {
                statsBuilder.setPacketsSent(counterEntry.packets());
                statsBuilder.setBytesSent(counterEntry.bytes());
            } else {
                log.warn("Unrecognized counter ID {}, skipping", counterEntry);
            }
        });

        return portStatBuilders
                .values()
                .stream()
                .map(DefaultPortStatistics.Builder::build)
                .collect(Collectors.toList());
    }
}
