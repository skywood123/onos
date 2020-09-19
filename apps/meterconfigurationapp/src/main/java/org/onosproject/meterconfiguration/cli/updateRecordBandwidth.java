package org.onosproject.meterconfiguration.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.MeteringService;
import org.onosproject.meterconfiguration.Record;
import org.onosproject.meterconfiguration.RecordType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.HashSet;
import java.util.Set;

@Service
@Command(scope = "onos", name = "updatebandwidth",description = "set bandwidth for a network")
public class updateRecordBandwidth extends AbstractShellCommand {

    @Argument(index = 0,name="networkId", description = "Network ID", required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "recordType", description = "Type of record : NETWORK | END_POINTS", required = true, multiValued = false)
    String recordTypeInString;

    @Argument(index = 2,name="new Bandwidth(Mbits)", description = "updated bandwidth amount", required = true, multiValued = false)
    Long newbandwidth = null;

    @Argument(index = 3,name="device1", description = "devicename", required = false, multiValued = false)
    String device1 = null;

    @Argument(index = 4,name="port1", description = "portnumber", required = false, multiValued = false)
    Long port1 = null;

    @Argument(index = 5,name="device2", description = "devicename", required = false, multiValued = false)
    String device2 = null;

    @Argument(index = 6,name="port2", description = "portnumber", required = false, multiValued = false)
    Long port2 = null;

    private BandwidthInventoryService service = get(BandwidthInventoryService.class);
    private MeteringService meterservice = get(MeteringService.class);

    @Override
    protected void doExecute() throws Exception {
        //check all the connectpoints bandwidth can acoomodate this request first
        //FIXME
        //need to check if compliant
        NetworkId currentnetworkId = NetworkId.networkId(networkId);
        RecordType recordType = null;
        if (recordTypeInString.equalsIgnoreCase("network")){
            recordType = RecordType.NETWORK;
        } else if ( recordTypeInString.equalsIgnoreCase("endpoints")){
            recordType = RecordType.END_POINTS;
        } else {
            log.warn("Undetermined record type : {}",recordTypeInString);
            return;
        }
        Set<ConnectPoint> sourcedest=null;
        if(device1 != null && port1 != null && device2 != null && port2 != null){
            sourcedest = new HashSet<>();
            sourcedest.add(new ConnectPoint(DeviceId.deviceId(device1), PortNumber.portNumber(port1)));
            sourcedest.add(new ConnectPoint(DeviceId.deviceId(device2),PortNumber.portNumber(port2)));
        }

       Record updatedRecord =  service.updateRecord(currentnetworkId, recordType, sourcedest, newbandwidth);
       if(updatedRecord != null) {
           meterservice.bandwidthRecordUpdated(currentnetworkId,updatedRecord);
       } else{
           log.warn("No updated record found; Not updating the meter cell bandwidth");
       }
    }
}
