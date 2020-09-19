package org.onosproject.meterconfiguration;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A bandwidth inventory of all the connectpoint known to the controller.
 *
 */

@Component(immediate = true , enabled = false, service = BandwidthInventoryService.class)
public class BandwidthInventory implements BandwidthInventoryService{

    //Set of ConnectPoints known to controller with bandwidth(port speed)
    private Map<ConnectPoint,BandwidthInformation> cpb;
    private BandwidthTransactionRecord record;
    private InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private InternalVirtualNetworkListener internalVirtualNetworkListener = new InternalVirtualNetworkListener();
    private InternalLinkListener linkListener = new InternalLinkListener();
    private InternalEdgeportListener edgeportListener = new InternalEdgeportListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private LinkService linkService;

    @Reference(cardinality =  ReferenceCardinality.MANDATORY)
    private VirtualNetworkService virtualNetworkService;

    @Reference(cardinality =  ReferenceCardinality.MANDATORY)
    private EdgePortService edgePortService;
    /*
    public BandwidthInventory(){
        cpb = new HashMap<>();
        record = new BandwidthTransactionRecord();
        initialization();
    }
    */

    @Activate
    public void activate(){
        cpb = new HashMap<>();
        record = new BandwidthTransactionRecord();
        //deviceService.addListener(internalDeviceListener);
        virtualNetworkService.addListener(internalVirtualNetworkListener);
        linkService.addListener(linkListener);
        edgePortService.addListener(edgeportListener);

        initialization();
        log.info("Bandwidth Inventory started");
    }
    @Deactivate
    public void deactivate(){
        log.info("BandwidthInventory deactivated.");
    }




    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get the available devices and put all the connectPoints and bandwidth(port speed) in cpb
     */
    private void initialization(){
        deviceService.getAvailableDevices().forEach(device -> updateCpb(device.id()));
    }

    private void updateCpb(DeviceId deviceid){

        //if a new device comes up
        if(deviceService.isAvailable(deviceid)){
            List<Port> ports = deviceService.getPorts(deviceid);
            ports.forEach(port -> cpb.put(new ConnectPoint(port.element().id(),port.number()),new BandwidthInformation(port)));

        }
    }

    private void updateCpbLinkUp(Link link){

        cpb.put(link.src(),new BandwidthInformation(deviceService.getPort(link.src().deviceId(),link.src().port())));
        cpb.put(link.dst(),new BandwidthInformation(deviceService.getPort(link.dst().deviceId(),link.dst().port())));
    }

    private void updateCpbLinkDown(Link link){
            cpb.remove(link.src());
            cpb.remove(link.dst());

    }
    private void updateCpbEdgePortUp(ConnectPoint connectPoint){
        cpb.put(connectPoint,new BandwidthInformation(deviceService.getPort(connectPoint.deviceId(),connectPoint.port())));
    }

    private void updateCpbEdgePortDown(ConnectPoint connectPoint){
        cpb.remove(connectPoint);
    }

    //Physical port down event
    private void actOnRecordForConnectPointDown(ConnectPoint problemConnectPoint){
        Set<Record> currentRecords = record.getRecords();
        Set<Record> deletingRecords = new HashSet<>();
        //Actions toward records when connectPoint down
        for(Record record : currentRecords){
            if(record.getType()==RecordType.NETWORK && record.getConnectPoints().contains(problemConnectPoint)){
                //if network record
                // if contain this connectpoint as in the set of connectpoints
                // remove this connectpoints from the record
                record.removeConnectPoint(problemConnectPoint);
            } else if (record.getType() == RecordType.END_POINTS && record.getSourcedest().contains(problemConnectPoint)){
                deletingRecords.add(record);
            } else if (record.getType() == RecordType.END_POINTS && record.getConnectPoints().contains(problemConnectPoint)){
                //if endpoints record
                // remove all connectpoints in the set of connectpoints
                // release all the bandwidth for the connectpoints recorded in the record
                // update the connectpoints when new path established for it
                //
                releaseBandwidth(record.getConnectPoints(),record.getBandwidth());
                record.setConnectPoints(null);
            }
        }

        for(Record deletingRecord : deletingRecords){
            record.deleteFlowRecord(deletingRecord.getNetworkId(),deletingRecord.getSourcedest());
            releaseBandwidth(deletingRecord.getConnectPoints(),deletingRecord.getBandwidth());
        }


    }

    /**
     *
     * @param networkId
     * @param sourcedest consists of source and destination connect point only
     * @return fixme record . In meterring, need to know the record type and bandwidth information
     */
    @Override
    public Record findRecord(NetworkId networkId, Set<ConnectPoint> sourcedest){
        Record recordEntry = null;
        recordEntry = record.getConnectPointsBandwidth(networkId, sourcedest);
        if(recordEntry != null){
            return recordEntry;
        }
        recordEntry = record.getBandwidth(networkId);
        if(recordEntry == null){
            log.warn("Bandwidth record for networkId = {} not found" , networkId);
        }
        return recordEntry;


    }

    public Record updateRecord(NetworkId networkId, RecordType recordType, Set<ConnectPoint> sourcedest, long newbandwidth){
        Record finding = null;
        if(recordType==RecordType.NETWORK) {

            finding = record.getBandwidth(networkId);

        } else if(recordType == RecordType.END_POINTS) {
            finding = record.getConnectPointsBandwidth(networkId,sourcedest);
        }
        if(finding!=null){
            //check if bandwidth compliant
            long difference = newbandwidth - finding.getBandwidth();
            boolean compliant = false;
            for(ConnectPoint connectPoint : finding.getConnectPoints()){
                compliant = comparator(connectPoint,difference);

                if(!compliant) {
                    log.warn("ConnectPoint {} failed to provide enough bandwidth", connectPoint.toString());
                    return null;
                }
            }
            log.info("Connect Points bandwidth checking compliance : {} ",compliant);
            if(compliant){
                for(ConnectPoint connectPoint : finding.getConnectPoints()){
                    reduceAvailable(connectPoint,difference);
                }

                finding.setBandwidth(newbandwidth);
            }
            log.info("New record status");
            log.info(finding.toString());
        }

        return finding;

    }

    //remove a record and do the neccessary things in this class
    public Record removingRecord(NetworkId networkId, Set<ConnectPoint> sourcedest){
        Record deleting  = record.deleteFlowRecord(networkId, sourcedest);

        if(deleting == null){
            log.warn("No record found to delete.");
            return null;
        }
        //release the bandwidth hold by this record
        releaseBandwidth(deleting.getConnectPoints(),deleting.getBandwidth());
        //increase back the cpb bandwidth

        //returning Record for metering in directing the traffic to the network Record
        return deleting;
    }

    /**
     * Allow request to bandwidth allocation depend on current available bandwidth
     * Allow bandwidth request for end points
     * Type of request = NETWORK,END_POINTS
     * FIXME validate the connectpoints before taking the input
     */
    @Override
    public boolean requestBandwidth(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints){
        boolean decision = false;
        log.warn("debugging list of connectpoints");
        for (ConnectPoint cp : connectPoints){
            log.info(cp.deviceId().toString()+cp.port().toString());
        }
        log.warn("debugging cpb");
        for (ConnectPoint cp : cpb.keySet()){
            log.info(cp.deviceId().toString()+cp.port().toString() + " availablebandwidth = " + cpb.get(cp).getAvailableBandwidth());
        }
        for(ConnectPoint connectPoint : connectPoints){
            decision = comparator(connectPoint,bandwidth);
            if (!decision) {
                return false;
            }
        }
        record.addRecord(type,networkId,bandwidth,connectPoints);
        for(ConnectPoint connectPoint : connectPoints){
            reduceAvailable(connectPoint,bandwidth);
        }



        return decision;

    }
    /**
     * Allow request to bandwidth allocation depend on current available bandwidth
     * Allow bandwidth request for end points
     * Type of request = NETWORK,END_POINTS
     * FIXME validate the connectpoints before taking the input
     */
    @Override
    public boolean requestBandwidth(RecordType type, NetworkId networkId, long bandwidth, Set<ConnectPoint> connectPoints, Set<ConnectPoint> sourcedest){
        boolean decision = false;

        for(ConnectPoint connectPoint : connectPoints){
            decision = comparator(connectPoint,bandwidth);
            if (!decision) {
                log.warn("Request is not satisfied. ConnectPoint {} cannot satisfy the requested bandwidth.", connectPoint.toString());
                return false;
            }
        }
        record.addRecord(type,networkId,bandwidth, connectPoints, sourcedest);

        for(ConnectPoint connectPoint : connectPoints){
            reduceAvailable(connectPoint,bandwidth);
        }



        return decision;

    }

    //debugging method to verify items
    public void printall(){

        log.info("-------------------------------------------------DEBUGGING-------------------------------------------------------");
        log.info("-----------------------------------------------------------------------------------------------------------------");
        log.info("CONNECTPOINT BANDWIDTH INFORMATION");
        for(ConnectPoint connectPoint : cpb.keySet()){
            log.info("ConnectPoint: {}  ;   Bandwidth: {}Mbps  ;   AvailableBandwidth: {}", connectPoint , cpb.get(connectPoint).getBandwidth() , cpb.get(connectPoint).getAvailableBandwidth() );
        }
        log.info("-----------------------------------------------------------------------------------------------------------------");
        log.info("BANDWIDTH TRANSACTION RECORD");
        log.info(record.dumpRecords());


    }



    /**
     * Check if it is able to fulfil the request at this connect point
     * @param connectPoint
     * @param bandwidth
     * @return
     */
    private boolean comparator(ConnectPoint connectPoint, long bandwidth){

            return (cpb.get(connectPoint).getAvailableBandwidth() - bandwidth) >= 0;

    }

    private void reduceAvailable(ConnectPoint connectPoint, long bandwidth){
        cpb.get(connectPoint).availableBandwidth -= bandwidth;
    }

    /**
     * Release the requested bandwidth from whoever requested it
     * FIXME release based on requested bandwidth
     */
    public void releaseBandwidth(Set<ConnectPoint> connectPoints, long bandwidth){
        if(connectPoints != null)
        for(ConnectPoint connectPoint : connectPoints){
            cpb.get(connectPoint).availableBandwidth += bandwidth;
        }
    }



    //device, port, connectpoint
    //connectpoint bandwidth
    class BandwidthInformation {
        //private ConnectPoint connectPoint;
        private long bandwidth;
        private long availableBandwidth;

        BandwidthInformation (Port port){
          //  connectPoint = new ConnectPoint(port.element().id(),port.number());

            //in Mbps
            bandwidth = port.portSpeed()/1000;
            availableBandwidth = bandwidth;
        }

        public long getBandwidth(){
            return bandwidth;
        }
        public long getAvailableBandwidth(){
            return availableBandwidth;
        }

       /* public DeviceId getDeviceId(){
            return connectPoint.deviceId();
        }

        public PortNumber getPortNum(){
            return connectPoint.port();
        }

        public ConnectPoint getConnectPoint(){
            return connectPoint;
        }*/



    }

    //only link between devices are captured
    //not link to host
    //events related to links between devices
    private class InternalLinkListener implements LinkListener{

        @Override
        public void event(LinkEvent event) {
            switch(event.type()){
                case LINK_ADDED:
                    log.info("Link added:");
                    log.info(event.subject().toString());
                    updateCpbLinkUp(event.subject());
                case LINK_REMOVED:
                    log.info("Link removed:");
                    log.info(event.subject().toString());
                    updateCpbLinkDown(event.subject());
                    actOnRecordForConnectPointDown(event.subject().src());
                    actOnRecordForConnectPointDown(event.subject().dst());
                case LINK_UPDATED:
                    log.info("Link update:");
                    log.info(event.subject().toString());
                default:
                    log.info("Default");
                    log.info(event.subject().toString());
                    break;
            }
        }
    }

    //Events related to port connecting to end host
    private class InternalEdgeportListener implements EdgePortListener{

        @Override
        public void event(EdgePortEvent event) {
            switch(event.type()){
                case EDGE_PORT_ADDED:
                    log.info("Edge port added");
                    log.info(event.toString());
                    updateCpbEdgePortUp(event.subject());
                    break;
                case EDGE_PORT_REMOVED:
                    log.info("Edge port removed");
                    log.info(event.toString());
                    updateCpbEdgePortDown(event.subject());
                    actOnRecordForConnectPointDown(event.subject());
                    break;
                default:
                    break;

            }
        }
    }

    //Handling events for : port down
    //change to link event better?
    //FIXME not to use this first
    private class InternalDeviceListener implements DeviceListener {
    //TODO Handling event to the cpb set
        @Override
        public void event(DeviceEvent event) {
            //link up down not detected as event
            //only can detect device added / availability change, port stats update
                switch(event.type()){
                    case PORT_ADDED:
                        log.info("DeviceEvent: Port added");
                        log.info(event.toString());
                        break;
                    case PORT_REMOVED:
                        log.info("DeviceEvent: Port removed");
                        log.info(event.toString());
                        break;
                        //commented below
                  //      actOnRecordForConnectPointDown(new ConnectPoint(event.subject().id(),event.port().number()));
                   //     break;
                    case DEVICE_AVAILABILITY_CHANGED:

                        log.info("DeviceEvent: Availability changes");
                        if(event.port()!=null)
                        log.info(event.port().toString());
                        log.info(event.toString());
                        break;
                    case PORT_UPDATED:
                        log.info("DeviceEvent: Port updated");
                        if(event.port()!=null)
                        log.info(event.port().toString());
                        log.info(event.toString());
                     //   log.warn("event hit, type device_added");

                    //commented below
             //           log.warn(event.toString());


                        //update connectpoint bandwidth information
               //Commented below
                    //         updateCpb(event.subject().id());
               //         break;
                    default:
                        if(event.type()== DeviceEvent.Type.PORT_STATS_UPDATED)
                            break;

                        log.info("Default Case");
                        if(event.port()!=null)
                        log.info(event.port().toString());
                        log.info(event.toString());
                        break;
                }
        }
    }

    //React to virtual network events in related to record only
    //Assume if a device down, all ports will trigger one event message
    //Handling events for : new virtual port added , virtual port deleted
    private class InternalVirtualNetworkListener implements VirtualNetworkListener{

        @Override
        public void event(VirtualNetworkEvent event) {

            NetworkId networkId = event.subject();
            Record relevantRecord = null;
            switch(event.type()){
                case NETWORK_REMOVED:
                    //remove the network records and flow records
                    //release all bandwidth
                    break;
                    //as long as there is changes in port, verify with the virtual network service
                    // to check the availability?

                //FIXME
                //need to handle device removed? or handle the ports are enough
                case VIRTUAL_PORT_ADDED:
                   // log.info("Event: Virtual Port added");
                //    break;

                    relevantRecord = record.getBandwidth(networkId);
                    if (relevantRecord == null) {
                        return;
                    }
                    if(comparator(event.virtualPort().realizedBy(),relevantRecord.getBandwidth())){
                        reduceAvailable(event.virtualPort().realizedBy(),relevantRecord.getBandwidth());
                        relevantRecord.addConnectPoint(event.virtualPort().realizedBy());
                        log.info("Bandwidth for newly added port check passed.");
                    } else{
                        //FIXME add into record or not if bandiwdth cannot fulfil ?
                        log.warn("Bandwidth for newly added port check failed, requested bandwidth = {} , available bandwidth = {}",
                                 relevantRecord.getBandwidth(),cpb.get(event.virtualPort().realizedBy()).getAvailableBandwidth());
                    }
                    break;

                    //a virtual port get removed
                case VIRTUAL_PORT_REMOVED:
                //    log.info("Event: Virtual Port removed");
                //    break;
                    ConnectPoint affected = event.virtualPort().realizedBy();
                    //end point record links should clear off;  it depend on the routing decision to update the new set of connectpoints
                    //remove the connectpoint in network record and release the bandwidth
                    Set<Record> networkRecords = record.getRecordsByNetworkId(networkId);
                    for(Record currentRecord : networkRecords){
                        //if affecting record
                        if(currentRecord.getType() == RecordType.END_POINTS) {
                            if (currentRecord.getSourcedest().contains(affected)) {
                                record.deleteFlowRecord(networkId, relevantRecord.getSourcedest());
                                releaseBandwidth(currentRecord.getConnectPoints(), currentRecord.getBandwidth());
                                continue;
                            } else if (currentRecord.getConnectPoints().contains(affected)) {
                                currentRecord.setConnectPoints(null);
                                releaseBandwidth(currentRecord.getConnectPoints(), currentRecord.getBandwidth());
                            }

                        } else if (currentRecord.getType() == RecordType.NETWORK) {
                            relevantRecord.removeConnectPoint(event.virtualPort().realizedBy());
                            Set<ConnectPoint> affectedConnectPoint = new HashSet<>();
                            affectedConnectPoint.add(event.virtualPort().realizedBy());
                            releaseBandwidth(affectedConnectPoint,relevantRecord.getBandwidth());
                        }
                    }
                    break;
                default:
                    log.warn("Virtual Event: {} occur.",event.type());
                    break;
            }
        }
    }





}
