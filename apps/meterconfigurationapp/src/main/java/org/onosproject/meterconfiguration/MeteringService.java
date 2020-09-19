package org.onosproject.meterconfiguration;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.MplsLabel;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Set;


public interface MeteringService {

    public void compute(NetworkId networkId, Record recordEntry, ConnectPoint connectPoint, Set<ConnectPoint> sourcedest, MplsLabel mplsLabel);

    public boolean deletingFlowRuleAndRedirectToNewMeterConfig(DeviceId deviceId, MplsLabel mplsLabel, Long uplinkport, Set<ConnectPoint> sourcedest, NetworkId networkId, Record record);

    public void printall();

    public void bandwidthRecordUpdated(NetworkId networkId, Record updatedRecord);

    public void deletingFlowRuleRelatedToRecord(NetworkId networkId, Record deletingRecord, Record networkRecord);

    public boolean deletingFlowRule(DeviceId deviceId, MplsLabel mplsLabel, Long uplinkport);
}
