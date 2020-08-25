package org.xzk.network_slicing.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.xzk.network_slicing.NetworkSlicing;
import org.xzk.network_slicing.models.RoutedNetworks;

import java.util.Map;

@Service
@Command(scope = "onos", name = "ns-list-routed-network",
        description = "Adds a routed network to a virtual network")
public class RoutedNetworkListCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;


    @Override
    protected void doExecute() throws Exception {
        NetworkId _networkId = NetworkId.networkId(networkId);
        if (NetworkSlicing.tenantRoutedNetworks.containsKey(_networkId)) {
            RoutedNetworks routedNetworks = NetworkSlicing.tenantRoutedNetworks.get(_networkId);
            if (routedNetworks.networkGateway != null) {
                for (Map.Entry<IpPrefix, IpAddress> networks : routedNetworks.networkGateway.entrySet()) {
                    print("Network Address: " + networks.getKey().toString() + " Gateway: " + networks.getValue().toString());
                }
            }
        }
    }
}
