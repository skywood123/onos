package org.onosproject.meterconfiguration;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

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
        deviceService.addListener(internalDeviceListener);
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
        List<Port> ports = deviceService.getPorts(deviceid);
        ports.forEach(port -> cpb.put(new ConnectPoint(port.element().id(),port.number()),new BandwidthInformation(port)));
    }

    /**
     *
     * @param networkId
     * @param sourcedest consists of source and destination connect point only
     * @return fixme record . In meterring, need to know the record type and bandwidth information
     */
    @Override
    public Record findRecord(NetworkId networkId, Set<ConnectPoint> sourcedest){
        Record recordEntry=null;
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
                return false;
            }
        }
        record.addRecord(type,networkId,bandwidth, connectPoints, sourcedest);

        for(ConnectPoint connectPoint : connectPoints){
            reduceAvailable(connectPoint,bandwidth);
        }



        return decision;

    }



    /**
     * Check if it is able to fulfil the request at this connect point
     * @param connectPoint
     * @param bandwidth
     * @return
     */
    private boolean comparator(ConnectPoint connectPoint, long bandwidth){

            return (cpb.get(connectPoint).getAvailableBandwidth() - bandwidth) > 0;

    }

    private void reduceAvailable(ConnectPoint connectPoint, long bandwidth){
        cpb.get(connectPoint).availableBandwidth -= bandwidth;
    }

    /**
     * Release the requested bandwidth from whoever requested it
     * FIXME release based on requested bandwidth
     */
    public void releaseBandwidth(Set<ConnectPoint> connectPoints){

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

    private class InternalDeviceListener implements DeviceListener {
    //TODO Handling event to the cpb set
        @Override
        public void event(DeviceEvent event) {
                switch(event.type()){
                    case PORT_ADDED:
                        break;
                    case PORT_REMOVED:
                        break;
                    case DEVICE_ADDED:
                        log.warn("event hit, type device_added");
                        log.warn(event.toString());
                        updateCpb(event.subject().id());
                        break;
                    case DEVICE_UPDATED:
                        break;
                    default:
                        break;

                }
        }
    }





}
