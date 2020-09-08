package org.xzk.network_slicing.cli;


import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualPort;

import org.onosproject.meterconfiguration.BandwidthInventory;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.RecordType;
//import org.onosproject.meterconfiguration.p4meterservice;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.HashSet;
import java.util.Set;

@Service
@Command(scope = "onos", name = "setnetworkbandwidth",description = "set bandwidth for a network")
public class setBandwidth extends AbstractShellCommand {

    @Argument(index = 0,name="networkId", description = "Network ID", required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1,name="bandwidth-limit(Mbits)", description = "bandwidth-limit(Mbits)", required = true, multiValued = false)
    Long bandwidth = null;



    //private p4meterservice p4meter=get(p4meterservice.class);


    private VirtualNetworkAdminService virtualNetworkAdminService = getService(VirtualNetworkAdminService.class);

    private BandwidthInventoryService bandwidthInventory = getService(BandwidthInventoryService.class);

    @Override
    protected void doExecute() throws Exception {
        NetworkId netID= NetworkId.networkId(networkId);
      //  p4meter.set_bandwidth(netID,bandwidth);
        requestNetworkBandwidth(netID,bandwidth);

    }


    public void requestNetworkBandwidth(NetworkId networkId, long bandwidth){


       bandwidthInventory.requestBandwidth(RecordType.NETWORK, networkId, bandwidth, getNetworkConnectPoints(networkId));

    }

    private Set<ConnectPoint> getNetworkConnectPoints(NetworkId networkId){
        Set<ConnectPoint> connectPoints = new HashSet<>();

        Set<VirtualDevice> virtualDevices = virtualNetworkAdminService.getVirtualDevices(networkId);

        Set<VirtualPort> virtualPorts = new HashSet<>();

        virtualDevices.forEach(device->virtualPorts.addAll(getVirtualPort(networkId,device.id())));

        virtualPorts.forEach(port-> connectPoints.add(port.realizedBy()));

        return connectPoints;
    }
    private Set<VirtualPort> getVirtualPort(NetworkId networkId, DeviceId deviceId){
        return virtualNetworkAdminService.getVirtualPorts(networkId,deviceId);
    }




}
