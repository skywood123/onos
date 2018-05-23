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
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_PROXY_MODE;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_ARP_MODE_STR;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC_STR;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_PORT_ID;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle ARP requests from gateway nodes.
 */
@Component(immediate = true)
public class OpenstackRoutingArpHandler {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_OWNER_ROUTER_GW = "network:router_gateway";
    private static final String DEVICE_OWNER_FLOATING_IP = "network:floatingip";
    private static final String ARP_MODE = "arpMode";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkAdminService osNetworkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    // TODO: need to find a way to unify aprMode and gatewayMac variables with
    // that in SwitchingArpHandler
    @Property(name = ARP_MODE, value = DEFAULT_ARP_MODE_STR,
            label = "ARP processing mode, proxy (default) | broadcast ")
    protected String arpMode = DEFAULT_ARP_MODE_STR;

    protected String gatewayMac = DEFAULT_GATEWAY_MAC_STR;

    private final OpenstackRouterListener osRouterListener = new InternalRouterEventListener();
    private final HostListener hostListener = new InternalHostListener();

    private ApplicationId appId;
    private NodeId localNodeId;
    private Map<String, String> floatingIpMacMap = Maps.newConcurrentMap();

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        localNodeId = clusterService.getLocalNode().id();
        osRouterService.addListener(osRouterListener);
        hostService.addListener(hostListener);
        leadershipService.runForLeadership(appId.name());
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        hostService.removeListener(hostListener);
        osRouterService.removeListener(osRouterListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();
        configService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    // TODO: need to find a way to unify aprMode and gatewayMac variables with
    // that in SwitchingArpHandler
    @Modified
    void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        String updateArpMode;

        updateArpMode = Tools.get(properties, ARP_MODE);
        if (!Strings.isNullOrEmpty(updateArpMode) && !updateArpMode.equals(arpMode)) {
            arpMode = updateArpMode;
        }

        log.info("Modified");
    }

    private void processArpPacket(PacketContext context, Ethernet ethernet) {
        ARP arp = (ARP) ethernet.getPayload();

        if (arp.getOpCode() == ARP.OP_REQUEST && arpMode.equals(ARP_PROXY_MODE)) {
            if (log.isTraceEnabled()) {
                log.trace("ARP request received from {} for {}",
                        Ip4Address.valueOf(arp.getSenderProtocolAddress()).toString(),
                        Ip4Address.valueOf(arp.getTargetProtocolAddress()).toString());
            }

            IpAddress targetIp = Ip4Address.valueOf(arp.getTargetProtocolAddress());

            MacAddress targetMac = null;

            NetFloatingIP floatingIP = osRouterService.floatingIps().stream()
                    .filter(ip -> ip.getFloatingIpAddress().equals(targetIp.toString()))
                    .findAny().orElse(null);

            //In case target ip is for associated floating ip, sets target mac to vm's.
            if (floatingIP != null && floatingIP.getPortId() != null) {
                targetMac = MacAddress.valueOf(osNetworkAdminService.port(
                                        floatingIP.getPortId()).getMacAddress());
            }

            if (isExternalGatewaySourceIp(targetIp.getIp4Address())) {
                targetMac = Constants.DEFAULT_GATEWAY_MAC;
            }

            if (targetMac == null) {
                log.trace("Unknown target ARP request for {}, ignore it", targetIp);
                return;
            }

            Ethernet ethReply = ARP.buildArpReply(targetIp.getIp4Address(),
                    targetMac, ethernet);


            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(context.inPacket().receivedFrom().port()).build();

            packetService.emit(new DefaultOutboundPacket(
                    context.inPacket().receivedFrom().deviceId(),
                    treatment,
                    ByteBuffer.wrap(ethReply.serialize())));

            context.block();
        }

        if (arp.getOpCode() == ARP.OP_REPLY) {
            PortNumber receivedPortNum = context.inPacket().receivedFrom().port();
            log.debug("ARP reply ip: {}, mac: {}",
                    Ip4Address.valueOf(arp.getSenderProtocolAddress()),
                    MacAddress.valueOf(arp.getSenderHardwareAddress()));
            try {
                if (receivedPortNum.equals(
                        osNodeService.node(context.inPacket().receivedFrom()
                                .deviceId()).uplinkPortNum())) {
                    osNetworkAdminService.updateExternalPeerRouterMac(
                            Ip4Address.valueOf(arp.getSenderProtocolAddress()),
                            MacAddress.valueOf(arp.getSenderHardwareAddress()));
                }
            } catch (Exception e) {
                log.error("Exception occurred because of {}", e.toString());
            }
        }

    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Set<DeviceId> gateways = osNodeService.completeNodes(GATEWAY)
                    .stream().map(OpenstackNode::intgBridge)
                    .collect(Collectors.toSet());

            if (!gateways.contains(context.inPacket().receivedFrom().deviceId())) {
                // return if the packet is not from gateway nodes
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if (ethernet != null &&
                    ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                eventExecutor.execute(() -> processArpPacket(context, ethernet));
            }
        }
    }

    private boolean isExternalGatewaySourceIp(IpAddress targetIp) {
        return osNetworkAdminService.ports().stream()
                .filter(osPort -> Objects.equals(osPort.getDeviceOwner(),
                        DEVICE_OWNER_ROUTER_GW))
                .flatMap(osPort -> osPort.getFixedIps().stream())
                .anyMatch(ip -> IpAddress.valueOf(ip.getIpAddress()).equals(targetIp));
    }

    // FIXME: need to find a way to invoke this method during node initialization
    private void initFloatingIpMacMap() {
        osRouterService.floatingIps().forEach(f -> {
            if (f.getPortId() != null && f.getFloatingIpAddress() != null) {
                Port port = osNetworkAdminService.port(f.getPortId());
                if (port != null && port.getMacAddress() != null) {
                    floatingIpMacMap.put(f.getFloatingIpAddress(), port.getMacAddress());
                }
            }
        });
    }

    /**
     * Installs static ARP rules used in ARP BROAD_CAST mode.
     * Note that, those rules will be only matched ARP_REQUEST packets,
     * used for telling gateway node the mapped MAC address of requested IP,
     * without the helps from controller.
     *
     * @param fip       floating IP address
     * @param install   flow rule installation flag
     */
    private void setFloatingIpArpRule(NetFloatingIP fip, boolean install) {
        if (arpMode.equals(ARP_BROADCAST_MODE)) {

            if (fip == null) {
                log.warn("Failed to set ARP broadcast rule for floating IP");
                return;
            }

            String macString;

            if (install) {
                if (fip.getPortId() != null) {
                    macString = osNetworkAdminService.port(fip.getPortId()).getMacAddress();
                    floatingIpMacMap.put(fip.getFloatingIpAddress(), macString);
                } else {
                    log.trace("Unknown target ARP request for {}, ignore it",
                            fip.getFloatingIpAddress());
                    return;
                }
            } else {
                macString = floatingIpMacMap.get(fip.getFloatingIpAddress());
            }

            MacAddress targetMac = MacAddress.valueOf(macString);

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpOp(ARP.OP_REQUEST)
                    .matchArpTpa(Ip4Address.valueOf(fip.getFloatingIpAddress()))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setArpOp(ARP.OP_REPLY)
                    .setArpSha(targetMac)
                    .setArpSpa(Ip4Address.valueOf(fip.getFloatingIpAddress()))
                    .setOutput(PortNumber.IN_PORT)
                    .build();

            osNodeService.completeNodes(GATEWAY).forEach(n ->
                    osFlowRuleService.setRule(
                            appId,
                            n.intgBridge(),
                            selector,
                            treatment,
                            PRIORITY_ARP_GATEWAY_RULE,
                            GW_COMMON_TABLE,
                            install
                    )
            );

            if (install) {
                log.info("Install ARP Rule for Floating IP {}",
                        fip.getFloatingIpAddress());
            } else {
                log.info("Uninstall ARP Rule for Floating IP {}",
                        fip.getFloatingIpAddress());
            }
        }
    }

    /**
     * An internal router event listener, intended to install/uninstall
     * ARP rules for forwarding packets created from floating IPs.
     */
    private class InternalRouterEventListener implements OpenstackRouterListener {

        @Override
        public boolean isRelevant(OpenstackRouterEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader);
        }

        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_ROUTER_CREATED:
                    eventExecutor.execute(() ->
                        // add a router with external gateway
                        setFakeGatewayArpRule(event.subject(), true)
                    );
                    break;
                case OPENSTACK_ROUTER_REMOVED:
                    eventExecutor.execute(() ->
                        // remove a router with external gateway
                        setFakeGatewayArpRule(event.subject(), false)
                    );
                    break;
                case OPENSTACK_ROUTER_GATEWAY_ADDED:
                    eventExecutor.execute(() ->
                        // add a gateway manually after adding a router
                        setFakeGatewayArpRule(event.externalGateway(), true)
                    );
                    break;
                case OPENSTACK_ROUTER_GATEWAY_REMOVED:
                    eventExecutor.execute(() ->
                        // remove a gateway from an existing router
                        setFakeGatewayArpRule(event.externalGateway(), false)
                    );
                    break;
                case OPENSTACK_FLOATING_IP_ASSOCIATED:
                    eventExecutor.execute(() ->
                        // associate a floating IP with an existing VM
                        setFloatingIpArpRule(event.floatingIp(), true)
                    );
                    break;
                case OPENSTACK_FLOATING_IP_DISASSOCIATED:
                    eventExecutor.execute(() ->
                        // disassociate a floating IP with the existing VM
                        setFloatingIpArpRule(event.floatingIp(), false)
                    );
                    break;
                case OPENSTACK_FLOATING_IP_CREATED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();

                        // during floating IP creation, if the floating IP is
                        // associated with any port of VM, then we will set
                        // floating IP related ARP rules to gateway node
                        if (!Strings.isNullOrEmpty(osFip.getPortId())) {
                            setFloatingIpArpRule(osFip, true);
                        }
                    });
                    break;
                case OPENSTACK_FLOATING_IP_REMOVED:
                    eventExecutor.execute(() -> {
                        NetFloatingIP osFip = event.floatingIp();

                        // during floating IP deletion, if the floating IP is
                        // still associated with any port of VM, then we will
                        // remove floating IP related ARP rules from gateway node
                        if (!Strings.isNullOrEmpty(osFip.getPortId())) {
                            setFloatingIpArpRule(event.floatingIp(), false);
                        }
                    });
                    break;
                default:
                    // do nothing for the other events
                    break;
            }
        }

        private void setFakeGatewayArpRule(ExternalGateway extGw, boolean install) {
            if (arpMode.equals(ARP_BROADCAST_MODE)) {

                if (extGw == null) {
                    return;
                }

                Optional<Subnet> subnet = osNetworkAdminService.subnets(
                                    extGw.getNetworkId()).stream().findFirst();
                if (!subnet.isPresent()) {
                    return;
                }

                String gateway = subnet.get().getGateway();

                TrafficSelector selector = DefaultTrafficSelector.builder()
                        .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                        .matchArpOp(ARP.OP_REQUEST)
                        .matchArpTpa(Ip4Address.valueOf(gateway))
                        .build();

                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .setArpOp(ARP.OP_REPLY)
                        .setArpSha(MacAddress.valueOf(gatewayMac))
                        .setArpSpa(Ip4Address.valueOf(gateway))
                        .setOutput(PortNumber.IN_PORT)
                        .build();

                osNodeService.completeNodes(GATEWAY).forEach(n ->
                        osFlowRuleService.setRule(
                                appId,
                                n.intgBridge(),
                                selector,
                                treatment,
                                PRIORITY_ARP_GATEWAY_RULE,
                                GW_COMMON_TABLE,
                                install
                        )
                );

                if (install) {
                    log.info("Install ARP Rule for Gateway {}", gateway);
                } else {
                    log.info("Uninstall ARP Rule for Gateway {}", gateway);
                }
            }
        }

        private void setFakeGatewayArpRule(Router router, boolean install) {
            setFakeGatewayArpRule(router.getExternalGatewayInfo(), install);
        }
    }

    /**
     * An internal host event listener, intended to uninstall
     * ARP rules during host removal. Note that this is only valid when users
     * remove host without disassociating floating IP with existing VM.
     */
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
                    removeArpRuleByInstancePort(instPort);
                    break;
                case HOST_UPDATED:
                case HOST_ADDED:
                default:
                    break;
            }
        }

        private void removeArpRuleByInstancePort(InstancePort port) {
            Set<NetFloatingIP> ips = osRouterService.floatingIps();
            for (NetFloatingIP fip : ips) {
                if (Strings.isNullOrEmpty(fip.getFixedIpAddress())) {
                    continue;
                }
                if (Strings.isNullOrEmpty(fip.getFloatingIpAddress())) {
                    continue;
                }
                if (fip.getFixedIpAddress().equals(port.ipAddress().toString())) {
                    eventExecutor.execute(() ->
                        setFloatingIpArpRule(fip, false)
                    );
                }
            }
        }

        // TODO: should be extracted as an utility helper method sooner
        private boolean isValidHost(Host host) {
            return !host.ipAddresses().isEmpty() &&
                    host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                    host.annotations().value(ANNOTATION_PORT_ID) != null;
        }
    }
}
