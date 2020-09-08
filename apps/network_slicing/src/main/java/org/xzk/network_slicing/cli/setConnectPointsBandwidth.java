package org.xzk.network_slicing.cli;


import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;

import org.onosproject.meterconfiguration.BandwidthInventory;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.RecordType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.xzk.network_slicing.NetworkSlicing;

import java.util.HashSet;
import java.util.Set;

@Service
@Command(scope = "onos", name = "setflowbandwidth",description = "set bandwidth for 2 connect points")

public class setConnectPointsBandwidth extends AbstractShellCommand {

    @Argument(index = 0,name="networkId", description = "Network ID", required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1,name="bandwidth-limit(Mbits)", description = "bandwidth-limit(Mbits)", required = true, multiValued = false)
    Long bandwidth = null;

    @Argument(index = 2,name="device1", description = "devicename", required = true, multiValued = false)
    String device1 = null;

    @Argument(index = 3,name="port1", description = "portnumber", required = true, multiValued = false)
    Long port1 = null;

    @Argument(index = 4,name="device2", description = "devicename", required = true, multiValued = false)
    String device2 = null;

    @Argument(index = 5,name="port2", description = "portnumber", required = true, multiValued = false)
    Long port2 = null;

    private NetworkSlicing validation = getService(NetworkSlicing.class);
    private VirtualNetworkAdminService virtualNetworkAdminService = getService(VirtualNetworkAdminService.class);

    private BandwidthInventoryService bandwidthInventory = getService(BandwidthInventoryService.class);

    @Override
    protected void doExecute() throws Exception {
        NetworkId netID= NetworkId.networkId(networkId);
        requestConnectPointsBandwidth(netID,bandwidth);
    }

    private void requestConnectPointsBandwidth(NetworkId networkId, long bandwidth){
        ConnectPoint cpsource = new ConnectPoint(DeviceId.deviceId(device1), PortNumber.portNumber(port1));
        ConnectPoint cpdest = new ConnectPoint(DeviceId.deviceId(device2), PortNumber.portNumber(port2));
        Set<ConnectPoint> sourcedest = new HashSet<>();
        sourcedest.add(cpsource);
        sourcedest.add(cpdest);

        bandwidthInventory.requestBandwidth(
               RecordType.END_POINTS, networkId, bandwidth, validation.pathcalculation(networkId, cpsource, cpdest), sourcedest
        );

    }





}
