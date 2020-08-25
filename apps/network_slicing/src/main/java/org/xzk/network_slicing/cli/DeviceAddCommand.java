package org.xzk.network_slicing.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

@Service
@Command(scope = "onos", name = "ns-add-device",
        description = "Adds a device to a virtual network")
public class DeviceAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    String deviceId = null;

    @Override
    protected void doExecute() throws Exception {
        VirtualNetworkAdminService virtualNetworkAdminService = get(VirtualNetworkAdminService.class);
        DeviceService deviceService = getService(DeviceService.class);

        if (deviceService.isAvailable(DeviceId.deviceId(deviceId))) {
            virtualNetworkAdminService.createVirtualDevice(NetworkId.networkId(networkId), DeviceId.deviceId(deviceId));
            print("Device " + deviceId + " successfully added to the virtual network (ID=" + networkId + ")");
        } else {
            error("Device does not exists in underlying network.");
        }
    }

}
