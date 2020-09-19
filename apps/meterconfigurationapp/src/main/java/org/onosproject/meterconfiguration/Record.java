package org.onosproject.meterconfiguration;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

public class Record {
    private RecordType type;
    private NetworkId networkId;
    private long bandwidth;
    //identify source and destination of a end point record; null in network record
    private Set<ConnectPoint> sourcedest;

    //identify the connectPoints cover by this record and the bandwidth is compliant
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

    public void setConnectPoints(Set<ConnectPoint> newConnectPoints){
        this.connectPoints = newConnectPoints;
    }
    public void addConnectPoint(ConnectPoint connectPoint){
        connectPoints.add(connectPoint);
    }
    public void removeConnectPoint(ConnectPoint connectPoint){
        connectPoints.remove(connectPoint);
    }
    public Set<ConnectPoint> getSourcedest(){
        return this.sourcedest;
    }

    public long getBandwidth(){
        return bandwidth;
    }

    public RecordType getType(){
        return type;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Set<ConnectPoint> getConnectPoints(){
        return this.connectPoints;
    }

    public NetworkId getNetworkId(){
        return this.networkId;
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
