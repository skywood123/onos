package org.onosproject.meterconfiguration;

import org.onlab.packet.MplsLabel;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

public interface p4meterservice {

    public void set_bandwidth(NetworkId networkId, int bandwidth);

    public void match_mpls_p4meter(DeviceId deviceId, MplsLabel label,PortNumber outport, NetworkId networkId);
}
