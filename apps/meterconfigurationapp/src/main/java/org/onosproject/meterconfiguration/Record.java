package org.onosproject.meterconfiguration;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

public class Record {
    private RecordType type;
    private NetworkId networkId;
    private long bandwidth;
    private Set<ConnectPoint> sourcedest;
    private Set<ConnectPoint> connectPoints;

    Record(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints){
        this.type = type;
        this.networkId = networkId;
        this.bandwidth = bandwidth;
        this.connectPoints = connectPoints;
        this.sourcedest = null;
    }
    Record(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints, Set<ConnectPoint> sourcedest){
        this.type = type;
        this.networkId = networkId;
        this.bandwidth = bandwidth;
        this.connectPoints = connectPoints;
        this.sourcedest = sourcedest;
    }

    @Override
    public String toString(){
        String sourceanddestination=" ";
        String netconnectPoints=" ";
        if(sourcedest!= null){
            for(ConnectPoint connectPoint : sourcedest)
                sourceanddestination = sourceanddestination + " " + connectPoint.toString();
        }
        if(connectPoints!=null){
            for(ConnectPoint connectPoint : connectPoints)
                netconnectPoints = netconnectPoints + " " + connectPoint.toString();
        }
        String recordInfo = "Record : Network Type : " + type + "  ,   NetworkId: " + networkId.id() + "   ,   Bandwidth: "+ bandwidth +
                "  ,   Source and Destination : "+sourceanddestination+" ,   ConnectPoints: " + netconnectPoints;
        return recordInfo;
    }



    public long getBandwidth(){
        return bandwidth;
    }

    public RecordType getType(){
        return type;
    }

    public boolean compare(RecordType type){
        if(type.equals(this.type)) {
            return true;
        } else {
            return false;
        }
    }
    public boolean compare(NetworkId networkId){
        if(networkId.equals(this.networkId)) {
            return true;
        } else {
            return false;
        }
    }
    public boolean compare(long bandwidth){
        if(bandwidth == this.bandwidth) {
            return true;
        } else {
            return false;
        }
    }
    public boolean comparenetwork(Set<ConnectPoint> connectPoints){
        if(connectPoints.equals(this.connectPoints)) {
            return true;
        } else {
            return false;
        }
    }
    public boolean comparesourcedest(Set<ConnectPoint> sourcedest){
        if(sourcedest.equals(this.sourcedest)) {
            return true;
        } else {
            return false;
        }
    }

}
