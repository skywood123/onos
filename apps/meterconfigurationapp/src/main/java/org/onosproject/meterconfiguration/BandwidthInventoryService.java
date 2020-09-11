package org.onosproject.meterconfiguration;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

public interface BandwidthInventoryService {

    public Record findRecord(NetworkId networkId, Set<ConnectPoint> sourcedest);

    public boolean requestBandwidth(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints);

    public boolean requestBandwidth(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints, Set<ConnectPoint> sourcedest);

    public void printall();


}
