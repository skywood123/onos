package org.xzk.network_slicing;


import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

public interface netinfo {

    public void getinfo();
    public Set<ConnectPoint> pathcalculation(NetworkId networkId, ConnectPoint cpsource, ConnectPoint cpdest);
}

