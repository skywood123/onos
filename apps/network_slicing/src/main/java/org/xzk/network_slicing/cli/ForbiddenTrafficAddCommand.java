package org.xzk.network_slicing.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRuleService;
import org.xzk.network_slicing.NetworkSlicing;
import org.xzk.network_slicing.models.FlowPair;
import org.xzk.network_slicing.models.FlowRuleInformation;

import java.util.LinkedList;
import java.util.List;

@Service
@Command(scope = "onos", name = "ns-add-forbidden-Traffic",
        description = "Adds a forbidden flow to be blocked")
public class ForbiddenTrafficAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "host1", description = "Host1",
            required = true, multiValued = false)
    String host1Ip = null;

    @Argument(index = 2, name = "host2", description = "Host2",
            required = true, multiValued = false)
    String host2Ip = null;


    @Override
    protected void doExecute() throws Exception {

        FlowRuleService flowRuleService = getService(FlowRuleService.class);

        NetworkId netId = NetworkId.networkId(networkId);
        IpAddress host1 = IpAddress.valueOf(host1Ip);
        IpAddress host2 = IpAddress.valueOf(host2Ip);

        FlowPair flowPair1 = new FlowPair(host1, host2);
        FlowPair flowPair2 = new FlowPair(host2, host1);

        if (!NetworkSlicing.forbiddenTraffic.containsKey(netId)) {
            NetworkSlicing.forbiddenTraffic.put(netId, new LinkedList<>());
        }
        NetworkSlicing.forbiddenTraffic.get(netId).add(flowPair1);
        NetworkSlicing.forbiddenTraffic.get(netId).add(flowPair2);

        print("Forbidden traffic entry added successfully!");

        try {
            List<FlowRuleInformation> flowRules1 = NetworkSlicing.flowRuleStorage.getFlowRules(netId, flowPair1);
            for (FlowRuleInformation f : flowRules1) {
                flowRuleService.removeFlowRules(f.getFlowRule());

                DeviceId currentDeviceId = f.getFlowRuleDeviceId();
                // Return MPLS label if any
                if (f.getMplsLabel() != null) {
                    NetworkSlicing.mplsLabelPool.get(currentDeviceId).returnLabel(f.getMplsLabel().toInt());
                }
            }

            List<FlowRuleInformation> flowRules2 = NetworkSlicing.flowRuleStorage.getFlowRules(netId, flowPair2);
            for (FlowRuleInformation f : flowRules2) {
                flowRuleService.removeFlowRules(f.getFlowRule());

                DeviceId currentDeviceId = f.getFlowRuleDeviceId();
                // Return MPLS label if any
                if (f.getMplsLabel() != null) {
                    NetworkSlicing.mplsLabelPool.get(currentDeviceId).returnLabel(f.getMplsLabel().toInt());
                }
            }
            NetworkSlicing.flowRuleStorage.deleteFlowRules(netId, flowPair1);
            NetworkSlicing.flowRuleStorage.deleteFlowRules(netId, flowPair2);
            print("Flows invalidated!");
        } catch (NullPointerException e) {
            // Do nothing
        }
    }
}
