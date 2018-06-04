/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_EXTERNAL_FLOATING_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLOATING_EXTERNAL;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLOATING_INTERNAL;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_PORT_ID;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGwByComputeDevId;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Handles OpenStack floating IP events.
 */
@Component(immediate = true)
public class OpenstackRoutingFloatingIpHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ERR_FLOW = "Failed set flows for floating IP %s: ";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type %s";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final OpenstackRouterListener floatingIpLisener = new InternalFloatingIpListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeListener();
    private final HostListener hostListener = new InternalHostListener();
    private Map<MacAddress, InstancePort> removedPorts = Maps.newConcurrentMap();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        hostService.addListener(hostListener);
        osRouterService.addListener(floatingIpLisener);
        osNodeService.addListener(osNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        osNodeService.removeListener(osNodeListener);
        osRouterService.removeListener(floatingIpLisener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setFloatingIpRules(NetFloatingIP floatingIp, Port osPort,
                                    OpenstackNode gateway, boolean install) {
        Network osNet = osNetworkService.network(osPort.getNetworkId());
        if (osNet == null) {
            final String errorFormat = ERR_FLOW + "no network(%s) exists";
            final String error = String.format(errorFormat,
                    floatingIp.getFloatingIpAddress(),
                    osPort.getNetworkId());
            throw new IllegalStateException(error);
        }

        MacAddress srcMac = MacAddress.valueOf(osPort.getMacAddress());
        log.trace("Mac address of openstack port: {}", srcMac);
        InstancePort instPort = instancePortService.instancePort(srcMac);

        // sweep through removed port map
        if (instPort == null) {
            instPort = removedPorts.get(srcMac);
            removedPorts.remove(srcMac);
        }

        if (instPort == null) {
            final String errorFormat = ERR_FLOW + "no host(MAC:%s) found";
            final String error = String.format(errorFormat,
                    floatingIp.getFloatingIpAddress(), srcMac);
            throw new IllegalStateException(error);
        }


        ExternalPeerRouter externalPeerRouter = externalPeerRouter(osNet);
        if (externalPeerRouter == null) {
            final String errorFormat = ERR_FLOW + "no external peer router found";
            throw new IllegalStateException(errorFormat);
        }

        updateComputeNodeRules(instPort, osNet, gateway, install);
        updateGatewayNodeRules(floatingIp, instPort, osNet, externalPeerRouter, gateway, install);

        // FIXME: downstream internal rules are still duplicated in all gateway nodes
        // need to make the internal rules de-duplicated sooner or later
        setDownstreamInternalRules(floatingIp, osNet, instPort, install);

        // TODO: need to refactor setUpstreamRules if possible
        setUpstreamRules(floatingIp, osNet, instPort, externalPeerRouter, install);
        log.trace("Succeeded to set flow rules for floating ip {}:{} and install: {}",
                floatingIp.getFloatingIpAddress(),
                floatingIp.getFixedIpAddress(),
                install);
    }

    private synchronized void updateGatewayNodeRules(NetFloatingIP fip,
                                                     InstancePort instPort,
                                                     Network osNet,
                                                     ExternalPeerRouter router,
                                                     OpenstackNode gateway,
                                                     boolean install) {

        Set<OpenstackNode> completedGws = osNodeService.completeNodes(GATEWAY);
        Set<OpenstackNode> finalGws = Sets.newConcurrentHashSet();
        finalGws.addAll(ImmutableSet.copyOf(completedGws));

        if (install) {
            if (completedGws.contains(gateway)) {
                if (completedGws.size() > 1) {
                    finalGws.remove(gateway);
                    if (fip.getPortId() != null) {
                        setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                                ImmutableSet.copyOf(finalGws), false);
                        finalGws.add(gateway);
                    }
                }
                if (fip.getPortId() != null) {
                    setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                            ImmutableSet.copyOf(finalGws), true);
                }
            } else {
                log.warn("Detected node should be included in completed gateway set");
            }
        } else {
            if (!completedGws.contains(gateway)) {
                if (completedGws.size() >= 1) {
                    if (fip.getPortId() != null) {
                        setDownstreamExternalRulesHelper(fip, osNet, instPort, router,
                                ImmutableSet.copyOf(finalGws), true);
                    }
                }
            } else {
                log.warn("Detected node should NOT be included in completed gateway set");
            }
        }
    }

    private synchronized void updateComputeNodeRules(InstancePort instPort,
                                                     Network osNet,
                                                     OpenstackNode gateway,
                                                     boolean install) {

        Set<OpenstackNode> completedGws = osNodeService.completeNodes(GATEWAY);
        Set<OpenstackNode> finalGws = Sets.newConcurrentHashSet();
        finalGws.addAll(ImmutableSet.copyOf(completedGws));

        if (gateway == null) {
            // these are floating IP related cases...
            setComputeNodeToGatewayHelper(instPort, osNet,
                    ImmutableSet.copyOf(finalGws), install);

        } else {
            // these are openstack node related cases...
            if (install) {
                if (completedGws.contains(gateway)) {
                    if (completedGws.size() > 1) {
                        finalGws.remove(gateway);
                        setComputeNodeToGatewayHelper(instPort, osNet,
                                ImmutableSet.copyOf(finalGws), false);
                        finalGws.add(gateway);
                    }

                    setComputeNodeToGatewayHelper(instPort, osNet,
                            ImmutableSet.copyOf(finalGws), true);
                } else {
                    log.warn("Detected node should be included in completed gateway set");
                }
            } else {
                if (!completedGws.contains(gateway)) {
                    finalGws.add(gateway);
                    setComputeNodeToGatewayHelper(instPort, osNet,
                            ImmutableSet.copyOf(finalGws), false);
                    finalGws.remove(gateway);
                    if (completedGws.size() >= 1) {
                        setComputeNodeToGatewayHelper(instPort, osNet,
                                ImmutableSet.copyOf(finalGws), true);
                    }
                } else {
                    log.warn("Detected node should NOT be included in completed gateway set");
                }
            }
        }
    }

    // a helper method
    private void setComputeNodeToGatewayHelper(InstancePort instPort,
                                               Network osNet,
                                               Set<OpenstackNode> gateways,
                                               boolean install) {
        TrafficTreatment treatment;

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(instPort.ipAddress().toIpPrefix())
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        switch (osNet.getNetworkType()) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(osNet.getProviderSegID()));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(osNet.getProviderSegID()));
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE,
                        osNet.getNetworkType().toString());
                throw new IllegalStateException(error);
        }

        OpenstackNode selectedGatewayNode = getGwByComputeDevId(gateways, instPort.deviceId());

        if (selectedGatewayNode == null) {
            final String errorFormat = ERR_FLOW + "no gateway node selected";
            throw new IllegalStateException(errorFormat);
        }
        treatment = DefaultTrafficTreatment.builder()
                .extension(buildExtension(
                        deviceService,
                        instPort.deviceId(),
                        selectedGatewayNode.dataIp().getIp4Address()),
                        instPort.deviceId())
                .setOutput(osNodeService.node(instPort.deviceId()).tunnelPortNum())
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                sBuilder.build(),
                treatment,
                PRIORITY_EXTERNAL_FLOATING_ROUTING_RULE,
                ROUTING_TABLE,
                install);
        log.trace("Succeeded to set flow rules from compute node to gateway on compute node");
    }

    private void setDownstreamInternalRules(NetFloatingIP floatingIp,
                                            Network osNet,
                                            InstancePort instPort,
                                            boolean install) {
        OpenstackNode cNode = osNodeService.node(instPort.deviceId());
        if (cNode == null) {
            final String error = String.format("Cannot find openstack node for device %s",
                    instPort.deviceId());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VXLAN && cNode.dataIp() == null) {
            final String errorFormat = ERR_FLOW + "VXLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VLAN && cNode.vlanIntf() == null) {
            final String errorFormat = ERR_FLOW + "VLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }

        IpAddress floating = IpAddress.valueOf(floatingIp.getFloatingIpAddress());

        // TODO: following code snippet will be refactored sooner or later
        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            // access from one VM to the others via floating IP
            TrafficSelector internalSelector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(floating.toIpPrefix())
                    .matchInPort(gNode.tunnelPortNum())
                    .build();

            TrafficTreatment.Builder internalBuilder = DefaultTrafficTreatment.builder()
                    .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                    .setEthDst(instPort.macAddress())
                    .setIpDst(instPort.ipAddress().getIp4Address());

            switch (osNet.getNetworkType()) {
                case VXLAN:
                    internalBuilder.setTunnelId(Long.valueOf(osNet.getProviderSegID()))
                            .extension(buildExtension(
                                    deviceService,
                                    gNode.intgBridge(),
                                    cNode.dataIp().getIp4Address()),
                                    gNode.intgBridge())
                            .setOutput(PortNumber.IN_PORT);
                    break;
                case VLAN:
                    internalBuilder.pushVlan()
                            .setVlanId(VlanId.vlanId(osNet.getProviderSegID()))
                            .setOutput(PortNumber.IN_PORT);
                    break;
                default:
                    final String error = String.format(ERR_UNSUPPORTED_NET_TYPE,
                            osNet.getNetworkType());
                    throw new IllegalStateException(error);
            }

            osFlowRuleService.setRule(
                    appId,
                    gNode.intgBridge(),
                    internalSelector,
                    internalBuilder.build(),
                    PRIORITY_FLOATING_INTERNAL,
                    GW_COMMON_TABLE,
                    install);
        });
        log.trace("Succeeded to set flow rules for downstream on gateway nodes");
    }

    private void setDownstreamExternalRulesHelper(NetFloatingIP floatingIp,
                                                  Network osNet,
                                                  InstancePort instPort,
                                                  ExternalPeerRouter externalPeerRouter,
                                                  Set<OpenstackNode> gateways, boolean install) {
        OpenstackNode cNode = osNodeService.node(instPort.deviceId());
        if (cNode == null) {
            final String error = String.format("Cannot find openstack node for device %s",
                    instPort.deviceId());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VXLAN && cNode.dataIp() == null) {
            final String errorFormat = ERR_FLOW + "VXLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }
        if (osNet.getNetworkType() == NetworkType.VLAN && cNode.vlanIntf() == null) {
            final String errorFormat = ERR_FLOW + "VLAN mode is not ready for %s";
            final String error = String.format(errorFormat, floatingIp, cNode.hostname());
            throw new IllegalStateException(error);
        }

        IpAddress floating = IpAddress.valueOf(floatingIp.getFloatingIpAddress());

        OpenstackNode selectedGatewayNode = getGwByComputeDevId(gateways, instPort.deviceId());

        if (selectedGatewayNode == null) {
            final String errorFormat = ERR_FLOW + "no gateway node selected";
            throw new IllegalStateException(errorFormat);
        }

        TrafficSelector.Builder externalSelectorBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(floating.toIpPrefix());

        TrafficTreatment.Builder externalTreatmentBuilder = DefaultTrafficTreatment.builder()
                .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                .setEthDst(instPort.macAddress())
                .setIpDst(instPort.ipAddress().getIp4Address());

        if (!externalPeerRouter.externalPeerRouterVlanId().equals(VlanId.NONE)) {
            externalSelectorBuilder.matchVlanId(externalPeerRouter.externalPeerRouterVlanId()).build();
            externalTreatmentBuilder.popVlan();
        }

        switch (osNet.getNetworkType()) {
            case VXLAN:
                externalTreatmentBuilder.setTunnelId(Long.valueOf(osNet.getProviderSegID()))
                        .extension(buildExtension(
                                deviceService,
                                selectedGatewayNode.intgBridge(),
                                cNode.dataIp().getIp4Address()),
                                selectedGatewayNode.intgBridge())
                        .setOutput(selectedGatewayNode.tunnelPortNum());
                break;
            case VLAN:
                externalTreatmentBuilder.pushVlan()
                        .setVlanId(VlanId.vlanId(osNet.getProviderSegID()))
                        .setOutput(selectedGatewayNode.vlanPortNum());
                break;
            default:
                final String error = String.format(ERR_UNSUPPORTED_NET_TYPE,
                        osNet.getNetworkType());
                throw new IllegalStateException(error);
        }

        osFlowRuleService.setRule(
                appId,
                selectedGatewayNode.intgBridge(),
                externalSelectorBuilder.build(),
                externalTreatmentBuilder.build(),
                PRIORITY_FLOATING_EXTERNAL,
                GW_COMMON_TABLE,
                install);
    }

    private void setUpstreamRules(NetFloatingIP floatingIp, Network osNet,
                                  InstancePort instPort, ExternalPeerRouter externalPeerRouter,
                                  boolean install) {
        IpAddress floating = IpAddress.valueOf(floatingIp.getFloatingIpAddress());
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(instPort.ipAddress().toIpPrefix());

        switch (osNet.getNetworkType()) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.valueOf(osNet.getProviderSegID()));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(osNet.getProviderSegID()));
                break;
            default:
                final String error = String.format(ERR_UNSUPPORTED_NET_TYPE,
                        osNet.getNetworkType());
                throw new IllegalStateException(error);
        }

        TrafficSelector selector = sBuilder.build();

        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setIpSrc(floating.getIp4Address())
                    .setEthSrc(instPort.macAddress())
                    .setEthDst(externalPeerRouter.externalPeerRouterMac());

            if (osNet.getNetworkType().equals(NetworkType.VLAN)) {
                tBuilder.popVlan();
            }

            if (!externalPeerRouter.externalPeerRouterVlanId().equals(VlanId.NONE)) {
                tBuilder.pushVlan().setVlanId(externalPeerRouter.externalPeerRouterVlanId());
            }
            osFlowRuleService.setRule(
                    appId,
                    gNode.intgBridge(),
                    selector,
                    tBuilder.setOutput(gNode.uplinkPortNum()).build(),
                    PRIORITY_FLOATING_EXTERNAL,
                    GW_COMMON_TABLE,
                    install);
            });
        log.trace("Succeeded to set flow rules for upstream on gateway nodes");
    }

    private ExternalPeerRouter externalPeerRouter(Network network) {
        if (network == null) {
            return null;
        }

        Subnet subnet = osNetworkService.subnets(network.getId()).stream().findAny().orElse(null);

        if (subnet == null) {
            return null;
        }

        RouterInterface osRouterIface = osRouterService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), subnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            return null;
        }

        Router osRouter = osRouterService.router(osRouterIface.getId());
        if (osRouter == null) {
            return null;
        }
        if (osRouter.getExternalGatewayInfo() == null) {
            return null;
        }

        ExternalGateway exGatewayInfo = osRouter.getExternalGatewayInfo();
        return osNetworkService.externalPeerRouter(exGatewayInfo);
    }


    private void associateFloatingIp(NetFloatingIP osFip) {
        Port osPort = osNetworkService.port(osFip.getPortId());
        if (osPort == null) {
            final String errorFormat = ERR_FLOW + "port(%s) not found";
            final String error = String.format(errorFormat,
                    osFip.getFloatingIpAddress(), osFip.getPortId());
            throw new IllegalStateException(error);
        }
        // set floating IP rules only if the port is associated to a VM
        if (!Strings.isNullOrEmpty(osPort.getDeviceId())) {
            setFloatingIpRules(osFip, osPort, null, true);
        }
    }

    private void disassociateFloatingIp(NetFloatingIP osFip, String portId) {
        Port osPort = osNetworkService.port(portId);
        if (osPort == null) {
            // FIXME when a port with floating IP removed without
            // disassociation step, it can reach here
            return;
        }
        // set floating IP rules only if the port is associated to a VM
        if (!Strings.isNullOrEmpty(osPort.getDeviceId())) {
            setFloatingIpRules(osFip, osPort, null, false);
        }
    }

    private class InternalFloatingIpListener implements OpenstackRouterListener {

        @Override
        public boolean isRelevant(OpenstackRouterEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }
            return event.floatingIp() != null;
        }

        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_FLOATING_IP_ASSOCIATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        associateFloatingIp(osFip);
                        log.info("Associated floating IP {}:{}",
                                osFip.getFloatingIpAddress(), osFip.getFixedIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_DISASSOCIATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        disassociateFloatingIp(osFip, event.portId());
                        log.info("Disassociated floating IP {}:{}",
                                osFip.getFloatingIpAddress(), osFip.getFixedIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_REMOVED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        if (!Strings.isNullOrEmpty(osFip.getPortId())) {
                            disassociateFloatingIp(osFip, osFip.getPortId());
                        }
                        log.info("Removed floating IP {}", osFip.getFloatingIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_CREATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();
                        if (!Strings.isNullOrEmpty(osFip.getPortId())) {
                            associateFloatingIp(event.floatingIp());
                        }
                        log.info("Created floating IP {}", osFip.getFloatingIpAddress());
                    });
                    break;
                case OPENSTACK_FLOATING_IP_UPDATED:
                case OPENSTACK_ROUTER_CREATED:
                case OPENSTACK_ROUTER_UPDATED:
                case OPENSTACK_ROUTER_REMOVED:
                case OPENSTACK_ROUTER_INTERFACE_ADDED:
                case OPENSTACK_ROUTER_INTERFACE_UPDATED:
                case OPENSTACK_ROUTER_INTERFACE_REMOVED:
                default:
                    // do nothing for the other events
                    break;
            }
        }
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }
            return event.subject().type() == GATEWAY;
        }

        @Override
        public void event(OpenstackNodeEvent event) {

            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> {
                        for (NetFloatingIP fip : osRouterService.floatingIps()) {
                            if (Strings.isNullOrEmpty(fip.getPortId())) {
                                continue;
                            }
                            Port osPort = osNetworkService.port(fip.getPortId());
                            if (osPort == null) {
                                log.warn("Failed to set floating IP {}", fip.getId());
                                continue;
                            }
                            setFloatingIpRules(fip, osPort, event.subject(), true);
                        }
                    });
                    break;
                case OPENSTACK_NODE_INCOMPLETE:

                    // we only purge the routing related rules stored in each
                    // compute node when gateway node becomes unavailable
                    if (!event.subject().type().equals(GATEWAY))  {
                        return;
                    }

                    eventExecutor.execute(() -> {
                        for (NetFloatingIP fip : osRouterService.floatingIps()) {
                            if (Strings.isNullOrEmpty(fip.getPortId())) {
                                continue;
                            }
                            Port osPort = osNetworkService.port(fip.getPortId());
                            if (osPort == null) {
                                log.warn("Failed to set floating IP {}", fip.getId());
                                continue;
                            }
                            Network osNet = osNetworkService.network(osPort.getNetworkId());
                            if (osNet == null) {
                                final String errorFormat = ERR_FLOW + "no network(%s) exists";
                                final String error = String.format(errorFormat,
                                        fip.getFloatingIpAddress(),
                                        osPort.getNetworkId());
                                throw new IllegalStateException(error);
                            }
                            MacAddress srcMac = MacAddress.valueOf(osPort.getMacAddress());
                            log.trace("Mac address of openstack port: {}", srcMac);
                            InstancePort instPort = instancePortService.instancePort(srcMac);

                            if (instPort == null) {
                                final String errorFormat = ERR_FLOW + "no host(MAC:%s) found";
                                final String error = String.format(errorFormat,
                                        fip.getFloatingIpAddress(), srcMac);
                                throw new IllegalStateException(error);
                            }

                            ExternalPeerRouter externalPeerRouter = externalPeerRouter(osNet);
                            if (externalPeerRouter == null) {
                                final String errorFormat = ERR_FLOW + "no external peer router found";
                                throw new IllegalStateException(errorFormat);
                            }

                            updateComputeNodeRules(instPort, osNet, event.subject(), false);
                            updateGatewayNodeRules(fip, instPort, osNet,
                                    externalPeerRouter, event.subject(), false);
                        }
                    });
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalHostListener implements HostListener {

        @Override
        public boolean isRelevant(HostEvent event) {
            Host host = event.subject();
            if (!isValidHost(host)) {
                log.debug("Invalid host detected, ignore it {}", host);
                return false;
            }
            return true;
        }

        @Override
        public void event(HostEvent event) {
            InstancePort instPort = HostBasedInstancePort.of(event.subject());
            switch (event.type()) {
                case HOST_REMOVED:
                    storeTempInstPort(instPort);
                    break;
                case HOST_UPDATED:
                case HOST_ADDED:
                default:
                    break;
            }
        }

        private void storeTempInstPort(InstancePort port) {
            Set<NetFloatingIP> ips = osRouterService.floatingIps();
            for (NetFloatingIP fip : ips) {
                if (Strings.isNullOrEmpty(fip.getFixedIpAddress())) {
                    continue;
                }
                if (Strings.isNullOrEmpty(fip.getFloatingIpAddress())) {
                    continue;
                }
                if (fip.getFixedIpAddress().equals(port.ipAddress().toString())) {
                    removedPorts.put(port.macAddress(), port);
                    eventExecutor.execute(() -> {
                        disassociateFloatingIp(fip, port.portId());
                        log.info("Disassociated floating IP {}:{}",
                                fip.getFloatingIpAddress(), fip.getFixedIpAddress());
                    });
                }
            }
        }

        private boolean isValidHost(Host host) {
            return !host.ipAddresses().isEmpty() &&
                    host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                    host.annotations().value(ANNOTATION_PORT_ID) != null;
        }
    }
}
