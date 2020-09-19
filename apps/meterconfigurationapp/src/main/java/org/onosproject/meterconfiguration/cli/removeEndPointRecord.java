package org.onosproject.meterconfiguration.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.MeteringService;
import org.onosproject.meterconfiguration.Record;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.HashSet;
import java.util.Set;

@Service
@Command(scope = "onos", name = "deleteflowrecord",description = "set bandwidth for a network")
public class removeEndPointRecord extends AbstractShellCommand {

    @Argument(index = 0,name="networkId", description = "Network ID", required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "recordType", description = "Type of record : NETWORK | END_POINTS", required = true, multiValued = false)
    String recordTypeInString;

    @Argument(index = 2,name="device1", description = "devicename", required = true, multiValued = false)
    String device1 = null;

    @Argument(index = 3,name="port1", description = "portnumber", required = true, multiValued = false)
    Long port1 = null;

    @Argument(index = 4,name="device2", description = "devicename", required = true, multiValued = false)
    String device2 = null;

    @Argument(index = 5,name="port2", description = "portnumber", required = true, multiValued = false)
    Long port2 = null;

    private BandwidthInventoryService service = get(BandwidthInventoryService.class);
    private MeteringService meterservice = get(MeteringService.class);

    @Override
    protected void doExecute() throws Exception {

        if(!recordTypeInString.equalsIgnoreCase("endpoints")){
            log.warn("Only records for endpoints flow are able to delete");
            return;
        }

        Set<ConnectPoint> sourcedest = new HashSet<>();
            sourcedest.add(new ConnectPoint(DeviceId.deviceId(device1), PortNumber.portNumber(port1)));
            sourcedest.add(new ConnectPoint(DeviceId.deviceId(device2),PortNumber.portNumber(port2)));
            NetworkId currentNetworkId = NetworkId.networkId(networkId);

       Record deleting =  service.removingRecord(currentNetworkId, sourcedest);
       if(deleting == null){
           log.warn("Error when deleting the record");
           return;
       }
       Record networkRecord = service.findRecord(currentNetworkId,sourcedest);
       meterservice.deletingFlowRuleRelatedToRecord(currentNetworkId, deleting, networkRecord);
    }
}
