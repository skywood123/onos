package org.onosproject.meterconfiguration;



import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.MplsLabel;
import org.onosproject.component.ComponentService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component(immediate = true, service = MeteringService.class)
public class Metering implements MeteringService{


    private final Logger log = LoggerFactory.getLogger(getClass());
    //  private final DeviceListener deviceListener = new NewdeviceListerner();
    //private final PiPipeconfWatchdogListener watchdogListener = new Meterconfig.NewdeviceListerner();

    private final Integer NUM_OF_METERCELL = 255;
    private final long DEFAULT_P4DEVICEID = 1;
    private ApplicationId appId;
    Map<DeviceId,MeterMetadata> deviceMeterCellMetadata = new HashMap<>();
    Map<DeviceId,ArrayList<MeterCellConfig>> deviceMeterCellConfig = new HashMap<>();

    //  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    //  private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfWatchdogService watchdogService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private VirtualNetworkService virtualNetworkService;



 //   @Reference(cardinality = ReferenceCardinality.MANDATORY)
  //  private BandwidthInventory bandwidthInventory;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentService componentService;

    private InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private final String BANDWIDTH_INVENTORY = org.onosproject.meterconfiguration.BandwidthInventory.class.getName();

    @Activate
    public void activate() {

        log.info("Registering listener to device service");
        //     deviceService.addListener(deviceListener);
        appId= coreService.registerApplication("org.onosproject.meterconfig");
        componentService.activate(appId,BANDWIDTH_INVENTORY);
        deviceService.addListener(internalDeviceListener);
        deviceService.getAvailableDevices().forEach(deviceId -> initializeDeviceInfo(deviceId.id()) );
        //    watchdogService.addListener(watchdogListener);
        log.info("merterconfig started");
        //   getmeterconfig();
        //setmeterconfig();
        //insert_metering_rule();
        //    set_bandwidth(NetworkId.networkId(1),5);
     //   deviceMeterCellMetadata=
     //   deviceMeterCellConfig = new HashMap<>();
    }

    @Deactivate
    public void deactivate(){
        log.info("Removing meterconfig flow rules...");
        flowRuleService.removeFlowRulesById(appId);
        log.info("Stopped");
    }

    private void initializeDeviceInfo(DeviceId deviceId) {
        deviceMeterCellConfig.put(deviceId,new ArrayList<>());
        deviceMeterCellMetadata.put(deviceId,new MeterMetadata());
    }

    /**
     * Configure meter cell bandwidth and insert flow rule to direct mpls traffic to the meter cell
     * @param networkId
     * @param recordEntry
     * @param connectPoint the switch to switch port
     * @param sourcedest source host connectpoint and destination host connectpoint
     * @param mplsLabel to direct interesting traffic to the meter
     */
    @Override
    public void compute(NetworkId networkId, Record recordEntry, ConnectPoint connectPoint, Set<ConnectPoint> sourcedest, MplsLabel mplsLabel){

        MeterMetadata meterMetadata;
        Long bandwidth = recordEntry.getBandwidth();
        RecordType recordType = recordEntry.getType();
        int index=-1;

            DeviceId deviceId = connectPoint.deviceId();
            if(deviceMeterCellMetadata.containsKey(deviceId)) {
                meterMetadata = deviceMeterCellMetadata.get(deviceId);
            } else {
                log.warn("Error when getting meter configuration for {}", deviceId);
        }
        //recordEntry = bandwidthInventory.findRecord(networkId,sourcedest);
        //supply from netslicing to avoid multiple look up
/*        if(recordEntry == null){
            log.warn("record not found for flow between network {}, endPoints: {}",networkId,sourcedest);
            return;
        }*/
        ArrayList<MeterCellConfig> existingMeterConfig;
        MeterCellConfig configForThis;

            if(!deviceMeterCellConfig.containsKey(deviceId)){
                //TODO
            }
                existingMeterConfig = deviceMeterCellConfig.get(deviceId);
                for(MeterCellConfig meterCellConfig : existingMeterConfig){
                    if(meterCellConfig.findMeterCellConfig(networkId,recordType,connectPoint,sourcedest)) {
                        index = meterCellConfig.index;
                        configForThis = meterCellConfig;
                        break;
                    }
                }
                //metercellconfig not found,create a new one and attach
                if(index== -1){
                    configForThis = new MeterCellConfig(networkId,recordType,connectPoint,deviceMeterCellMetadata.get(deviceId).getavailableIndex(networkId));
                    deviceMeterCellConfig.get(deviceId).add(configForThis);
                    index= configForThis.index;
                    setmeterconfig(bandwidth,deviceId,index);
                }

                //direct the traffic to that index
        //for verifying purpose logging
                log.info("Configuring Device : {}   ,   Bandwidth allowed : {}  ,   MeterCellIndex: {}",deviceId,bandwidth,index);
                //If end point flow, give higher priority
                insert_metering_rule(recordType,connectPoint.port(),deviceId,index,mplsLabel);
                log.info("Inserting Metering Rule : Device: {}  ,   UplinkPort: {}  ,   Index: {}   ,   MatchingMplsLabel: {}", deviceId,connectPoint.port(),index,mplsLabel.toString());



    }
    private void setmeterconfig(long bandwidth,DeviceId deviceId,int metercellindex){
        long rate = (bandwidth*1000000)/8;
        PiMeterBand meterband=new PiMeterBand(rate, 3000);
        //     PiMeterBand meterband2=new PiMeterBand(625000,3000);
        PiMeterId meterId = PiMeterId.of("ingress_control.tenant_meter_ingress_control.tenant_port_meter");
        PiMeterCellId cellId=PiMeterCellId.ofIndirect(meterId, metercellindex);
        PiMeterCellConfig meter1=PiMeterCellConfig.builder()
                .withMeterBand(meterband)
                .withMeterBand(meterband)
                .withMeterCellId(cellId)
                .build();
            /*
        PiMeterCellConfig meter2=PiMeterCellConfig.builder()
                .withMeterBand(meterband)
                .withMeterBand(meterband)
                .withMeterCellId(PiMeterCellId.ofIndirect(meterId,2))
                .build();

             */
        //  DeviceId deviceId = DeviceId.deviceId("device:bmv2:s1");
        DriverHandler handler=driverService.createHandler(deviceId);
        P4RuntimeController controller = handler.get(P4RuntimeController.class);
        P4RuntimeClient client = controller.get(deviceId);

        PiPipeconfService piPipeconfService = handler.get(PiPipeconfService.class);
        PiPipeconf pipeconf = piPipeconfService.getPipeconf(deviceId).get();

        P4RuntimeWriteClient.WriteResponse response = client.write(DEFAULT_P4DEVICEID, pipeconf).entity(meter1, P4RuntimeWriteClient.UpdateType.MODIFY).submitSync();
        //    P4RuntimeWriteClient.WriteResponse response2 = client.write(pipeconf).entity(meter2,P4RuntimeWriteClient.UpdateType.MODIFY).submitSync();
        log.warn("write result is " + response.isSuccess());
        //  log.warn("write2 result is " + response.isSuccess());
        //    if (response.isSuccess())
        //      getmeterconfig();
        //    else
        //       log.warn("dk what is happening :(");
    }

    private void insert_metering_rule(RecordType recordType, PortNumber uplinkport, DeviceId deviceId, int metercellindex, MplsLabel mplsLabel){
        String TENANT_METER_CONTROL="ingress_control.tenant_meter_ingress_control.";
        PiTableId TENANT_UPLINK_TABLE= PiTableId.of(TENANT_METER_CONTROL+ "tenant_uplink_meter_table");


        PiMatchFieldId EGRESS_PORT = PiMatchFieldId.of("standard_metadata.egress_spec");
        // PiCriterion matchegress = PiCriterion.builder().matchExact(EGRESS_PORT,uplinkport.toLong()).build();
        //    PiCriterion match2 = PiCriterion.builder().matchExact(EGRESS_PORT,2).build();

        //PiMatchFieldId INGRESS_PORT = PiMatchFieldId.of("standard_metadata.ingress_port");

        // PiCriterion matchinout = PiCriterion.builder().matchTernary(INGRESS_PORT,inport.toLong(),0xff).matchTernary(EGRESS_PORT,uplinkport.toLong(),0xff).build();
        PiMatchFieldId LABEL=PiMatchFieldId.of("hdr.mpls.label");
        PiCriterion matchlabelout=PiCriterion.builder().matchExact(LABEL, mplsLabel.toInt()).matchExact(EGRESS_PORT, uplinkport.toLong()).build();
        PiActionId READ_METER_AND_TAG  = PiActionId.of(TENANT_METER_CONTROL + "read_meter_and_tag");

        PiActionParamId TENANT_OUTPORT = PiActionParamId.of("tenant_outport");
        PiActionParamId TENANT_ID = PiActionParamId.of("tenant_id");
        PiActionParam meterindex = new PiActionParam(TENANT_OUTPORT, metercellindex);
        //    PiActionParam meterindex2 = new PiActionParam(TENANT_OUTPORT,metercellindex);

        PiAction execute_meter= PiAction.builder().withId(READ_METER_AND_TAG).withParameter(meterindex).build();
        //    PiAction execute_meter2= PiAction.builder().withId(READ_METER_AND_TAG).withParameter(meterindex2).build();

        if(recordType == RecordType.NETWORK) {
            insertPiFlowRule(deviceId, TENANT_UPLINK_TABLE, matchlabelout, execute_meter);
        } else if (recordType == RecordType.END_POINTS) {
            insertPiFlowRuleHigherPriority(deviceId, TENANT_UPLINK_TABLE, matchlabelout, execute_meter);
        }
        //   insertPiFlowRule(DeviceId.deviceId("device:bmv2:s1"),TENANT_UPLINK_TABLE,match2,execute_meter2);

        PiTableId TENANT_FILTERING_TABLE = PiTableId.of(TENANT_METER_CONTROL + "tenant_uplink_meter_filtering_table");

        PiMatchFieldId PACKET_COLOUR = PiMatchFieldId.of("my_metadata.packet_colour");
        PiCriterion match_packet_colour = PiCriterion.builder().matchExact(PACKET_COLOUR,2).build();

        PiActionId _DROP = PiActionId.of(TENANT_METER_CONTROL + "_drop");
        PiAction droppacket= PiAction.builder().withId(_DROP).build();

        insertPiFlowRule(deviceId,TENANT_FILTERING_TABLE,match_packet_colour,droppacket);

//private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction)
    }

    private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction){
        FlowRule rule = DefaultFlowRule.builder().forDevice(deviceId)
                .forTable(tableId)
                .withPriority(1000)
                .fromApp(appId)
                //   .withIdleTimeout(60)
                .makePermanent()
                .withSelector(DefaultTrafficSelector.builder().matchPi(piCriterion).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(piAction).build())
                .build();
        flowRuleService.applyFlowRules(rule);
    }
    private void insertPiFlowRuleHigherPriority(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction){
        FlowRule rule = DefaultFlowRule.builder().forDevice(deviceId)
                .forTable(tableId)
                .withPriority(2000)
                .fromApp(appId)
                //   .withIdleTimeout(60)
                .makePermanent()
                .withSelector(DefaultTrafficSelector.builder().matchPi(piCriterion).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(piAction).build())
                .build();
        flowRuleService.applyFlowRules(rule);
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
                    if(deviceMeterCellMetadata.containsKey(event.subject().id())) {
                        deviceMeterCellMetadata.put(event.subject().id(),new MeterMetadata());
                    }
                    if(deviceMeterCellConfig.containsKey(event.subject().id())) {
                        deviceMeterCellConfig.put(event.subject().id(),new ArrayList<>());
                    }
                    break;
                case DEVICE_UPDATED:
                    break;
                default:
                    break;

            }
        }
    }


    class MeterCellConfig{
        NetworkId networkId;
        RecordType recordtype;
        //connectpoint or port number for this metercell
        ConnectPoint connectPoint;
        Integer index;
        //if this meter is for endpoints
        Set<ConnectPoint> endPoints;

        MeterCellConfig(NetworkId networkId, RecordType recordType, ConnectPoint connectPoint, Integer index) {
            this(networkId,recordType,connectPoint,index,null);
        }


        MeterCellConfig(NetworkId networkId, RecordType recordType, ConnectPoint connectPoint, Integer index, Set<ConnectPoint> endPoints) {
            this.networkId = networkId;
            this.recordtype = recordType;
            this.connectPoint = connectPoint;
            this.index = index;
            if(recordType == RecordType.END_POINTS) {
                this.endPoints = endPoints;
            }
        }

        public boolean findMeterCellConfig(NetworkId networkId, RecordType recordType, ConnectPoint uplink, Set<ConnectPoint> endPoints){

            if(recordType == RecordType.END_POINTS) {
                if(this.networkId.equals(networkId) && this.recordtype.equals(recordType) && this.connectPoint.equals(uplink) && this.endPoints.equals(endPoints)) {
                    return true;
                }
            } else {
                if(this.networkId.equals(networkId) && this.recordtype.equals(recordType) && this.connectPoint.equals(uplink)) {
                    return true;
                }
            }
            return false;
        }

    }

    class MeterMetadata{
        ArrayList<Integer> availableIndex;
        ArrayList<Integer> occupiedIndex;
        Map<NetworkId,Boolean> availableEndPointsMeter;

        MeterMetadata(){
            availableIndex = new ArrayList<>();
            for(int i=0;i<NUM_OF_METERCELL;i++){
                availableIndex.add(i);
            }
            occupiedIndex = new ArrayList<>();
            availableEndPointsMeter = new HashMap<>();
        }

        public Integer getavailableIndex(NetworkId networkId){
            int index=-1;

            if(!availableIndex.isEmpty()) {
         //       if(recordType == RecordType.END_POINTS) {
                    index=availableIndex.remove(0);
                    occupiedIndex.add(index);
         //           availableEndPointsMeter.put(networkId,true);
         //       } else {
         //           index=availableIndex.remove(0);
         //           occupiedIndex.add(index);
         //       }

            } else {
                log.warn("All meter cell are occupied.");
            }
            return index;
        }
        public Boolean anyEndPointMeter(NetworkId networkId){
            return availableEndPointsMeter.get(networkId);
        }
    }

    //fixme device listener class for device event if port down

}
