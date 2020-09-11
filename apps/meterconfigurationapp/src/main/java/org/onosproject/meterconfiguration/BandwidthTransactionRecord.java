package org.onosproject.meterconfiguration;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;

import java.util.HashSet;
import java.util.Set;

/**
 * Record the past and current request / release of bandwidth transaction by virtual networks
 */
public class BandwidthTransactionRecord {

    private static Set<Record> records;

    BandwidthTransactionRecord(){
        records = new HashSet<>();
    }

    public static Set<Record> getRecords(){

        return records;
    }

    public void addRecord(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints){
        records.add(new Record(type,networkId,bandwidth,connectPoints));
    }

    public void addRecord(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints, Set<ConnectPoint> sourcedest){
        records.add(new Record(type, networkId, bandwidth, connectPoints, sourcedest));
    }
    public String dumpRecords() {

        String recordsInformation= "";
        for(Record record : records){
            recordsInformation = recordsInformation + "\n" + record.toString();
        }

        return recordsInformation;
    }

    /**
     * get bandwidth allocated for the virtual network
     * @param networkId
     * @return -1 if record not found
     */
    public Record getBandwidth(NetworkId networkId){
        for(Record record : records){
            if(record.compare(networkId) && record.compare(RecordType.NETWORK)) {
                return record;
            }
        }
        return null;
    }

    /**
     * get bandwidth allocated for the set of connectPoints
     * @param networkId
     * @param sourcedest
     * @return -1 if record not found
     * FIXME put a method to see if any end points flow in the records for this networkid
     */
    public Record getConnectPointsBandwidth(NetworkId networkId, Set<ConnectPoint> sourcedest){
        for(Record record : records){
            if(record.compare(networkId) && record.comparesourcedest(sourcedest) && record.compare(RecordType.END_POINTS)) {
                return record;
            }
        }
        return null;
    }





/*    public class Record {
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



        public long getBandwidth(){
            return bandwidth;
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

    }*/




}
