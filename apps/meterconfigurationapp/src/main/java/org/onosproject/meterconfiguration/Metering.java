package org.onosproject.meterconfiguration;


import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.MplsLabel;
import org.onosproject.component.ComponentService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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

    //Having knowledge of flowrule inserted
    Map<DeviceId,Set<ExtraInfoFlowRule>> deviceFlowRule = new HashMap<>();

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

    @Override
    public void printall(){
        log.info("------------------------------------Debuggin Metering lists-----------------------------------");
        log.info("----------------------------------------------------------------------------------------------");
        log.info("----------------------------------------------------------------------------------------------");
        log.info("-------------------------------------DeviceMeterCellMetadata----------------------------------");
        for(DeviceId deviceId : deviceMeterCellMetadata.keySet()){
            log.info("DeviceId : {} ; MeterCellMetadata Occupied Index: {}",deviceId,deviceMeterCellMetadata.get(deviceId).occupiedIndex);
        }
        log.info("----------------------------------------------------------------------------------------------");
        log.info("-------------------------------------DeviceMeterCellConfig----------------------------------");
        for(DeviceId deviceId : deviceMeterCellConfig.keySet()){
            log.info("Device : {} " , deviceId);
            log.info("-------------------------------------------------------");
            log.info("MeterCellConfigs");
            log.info("-------------------------------------------------------");
            for(MeterCellConfig meterCellConfig : deviceMeterCellConfig.get(deviceId)) {
                log.info("Index : {}", meterCellConfig.getIndex());
                log.info("Record Type : {} , UplinkPort : {} , SourceDestConnectPoint : {}",meterCellConfig.recordtype, meterCellConfig.connectPoint.toString(), meterCellConfig.endPoints);
                log.info("------------------------------------------------------------------------");
            }
        }
        log.info("----------------------------------------------------------------------------------------------");
        log.info("-------------------------------------DeviceFlowRules------------------------------------------");
        for(DeviceId deviceId : deviceFlowRule.keySet()){
            log.info("Device : {}",deviceId);
            log.info("-------------------------------------------------------");
            log.info("FlowRules");
            log.info("-------------------------------------------------------");
            for(ExtraInfoFlowRule extraInfoFlowRule : deviceFlowRule.get(deviceId)){
                log.info("Redirecting to meter index {} ; MatchingLabel : {} ; MatchingUplinkPort : {}",extraInfoFlowRule.metercellindex,extraInfoFlowRule.mplsLabel,extraInfoFlowRule.uplinkport);
                log.info("----------------------------------------------------------------------");
            }

        }

    }

    private void initializeDeviceInfo(DeviceId deviceId) {
        deviceMeterCellConfig.put(deviceId,new ArrayList<>());
        deviceMeterCellMetadata.put(deviceId,new MeterMetadata());
        deviceFlowRule.put(deviceId, new HashSet<>());
    }

    //As a action response to bandwidth record updates
    //Update to network bandwidth amount; update to flow bandwidth amount
    //find out the meterCellIndexs and change the amount
    //no need to delete flow rules
    @Override
    public void bandwidthRecordUpdated(NetworkId networkId, Record updatedRecord){
        for(DeviceId deviceId : deviceMeterCellConfig.keySet()){
            for(MeterCellConfig meterCellConfig : deviceMeterCellConfig.get(deviceId)){
                if(meterCellConfig.compareRecord(networkId,updatedRecord.getType(),updatedRecord.getSourcedest())) {
                    int index = meterCellConfig.index;
                    long newBandwidth = updatedRecord.getBandwidth();
                    setmeterconfig(newBandwidth,deviceId,index);
                    log.info("Changing bandwidth {} meter index {} to new allocated bandwidth {}",deviceId,index,newBandwidth);
                }
            }
        }
    }

    //As a action response to bandwidth record deletion(Can't remove a network bandwidth record but a flow bw record)
    //let say deleting a flow bandwidth record
    //remove the flows inserted for this two connect points path
    //maintain the mplslabel and insert flow rules to the meterCell for network
    @Override
    public void deletingFlowRuleRelatedToRecord(NetworkId networkId, Record deletingRecord, Record networkRecord){
        Set<MeterCellConfig> deletingMeterCellConfigList = new HashSet<>();
        for(DeviceId deviceId: deviceMeterCellConfig.keySet()){

            Set<MeterCellConfig> temporarydeletingMeterCellConfig = new HashSet<>();

            for(MeterCellConfig meterCellConfig : deviceMeterCellConfig.get(deviceId)){
                if(meterCellConfig.compareRecord(networkId, deletingRecord.getType(),deletingRecord.getSourcedest())){
                    deletingMeterCellConfigList.add(meterCellConfig);
                    temporarydeletingMeterCellConfig.add(meterCellConfig);

                }
            }
            //to avoid concurrent modification error for the arraylist
            for(MeterCellConfig meterCellConfig: temporarydeletingMeterCellConfig){
                deviceMeterCellConfig.get(deviceId).remove(meterCellConfig);
            }
        }

        //return the index to the metadata
        deletingMeterCellConfigList.forEach(
                meterCellConfig -> deviceMeterCellMetadata.get(meterCellConfig.connectPoint.deviceId()).returnavailableIndex(meterCellConfig.getIndex()));


        Set<ExtraInfoFlowRule> deletingflowRuleSet = new HashSet<>();

        for(MeterCellConfig meterCellConfig : deletingMeterCellConfigList){
            DeviceId deviceId = meterCellConfig.connectPoint.deviceId();
            int index = meterCellConfig.getIndex();

            //Find out flow rules pointing to this meter index and delete the flow rules
            Set<ExtraInfoFlowRule> flowRuleSet = new CopyOnWriteArraySet<>();
            flowRuleSet.addAll(deviceFlowRule.get(deviceId));
            //to avoid concurrent modification error
            for(ExtraInfoFlowRule extraInfoFlowRule : flowRuleSet) {
                if(extraInfoFlowRule.getMeterIndex()==index){
                    flowRuleService.removeFlowRules(extraInfoFlowRule.flowRule);
                    deletingflowRuleSet.add(extraInfoFlowRule);

                    //insert new flow rule here; need use the mplslabel of the previous label
                    //find out the network meterconfig and send flow rules to it
                    compute(networkId,networkRecord,meterCellConfig.connectPoint,null,extraInfoFlowRule.mplsLabel);
                }
            }
        }
            deletingflowRuleSet.forEach(extraInfoFlowRule -> deviceFlowRule.get(extraInfoFlowRule.deviceId).remove(extraInfoFlowRule));


        deletingMeterCellConfigList = null;
        deletingflowRuleSet = null;
    }

    //TODO
    //Having a way listen to bandwidth Inventory events to trigger the above two methods response
    //Workaround : use cli to invoke the methods necessary in here :)

    //TODO
    private void removeDevice(){
        //an action/response to the device down
        //remove the related thing in the list
    }


    //There is already a flow pair established
    //Allow calling when creating ENDPOINTS record
    //Find out the flow rules that used and pointing to network meterCellIndex and delete it
    //Insert new flow rules and point to new meter cell index
    public boolean deletingFlowRuleAndRedirectToNewMeterConfig(DeviceId deviceId, MplsLabel mplsLabel, Long uplinkport, Set<ConnectPoint> sourcedest, NetworkId networkId, Record configuring){
        if(deviceFlowRule.containsKey(deviceId)) {
            Set<ExtraInfoFlowRule> flowRulesSet = deviceFlowRule.get(deviceId);
            FlowRule toDelete;
            for(ExtraInfoFlowRule extraInfoFlowRule : flowRulesSet) {
                toDelete = extraInfoFlowRule.compareFlowRule(deviceId,mplsLabel,uplinkport);
                if (toDelete != null) {
                    flowRuleService.removeFlowRules(toDelete);
                    if(!meterIsReferenced(deviceId,extraInfoFlowRule, extraInfoFlowRule.getMeterIndex())) {
                        returningIndex(deviceId,extraInfoFlowRule.getMeterIndex());
                    }
                    //FIXME
                    // will this get error
                    deviceFlowRule.get(deviceId).remove(extraInfoFlowRule);
                    //TODO TO BE verified
                    ConnectPoint connectPoint = new ConnectPoint(deviceId,PortNumber.portNumber(uplinkport));
                    if(configuring == null) {
                        log.warn("No end point record found!");
                        return false;
                    }
                     MeterCellConfig configForThis = new MeterCellConfig(networkId,RecordType.END_POINTS,connectPoint,deviceMeterCellMetadata.get(deviceId).getavailableIndex(networkId),sourcedest);
                        deviceMeterCellConfig.get(deviceId).add(configForThis);
                        int index = configForThis.index;
                        long bandwidth = configuring.getBandwidth();
                        setmeterconfig(bandwidth,deviceId,index);

                    //direct the traffic to that index
                    //for verifying purpose logging
                    log.info("Configuring Device : {}   ,   Bandwidth allowed : {}  ,   MeterCellIndex: {}",deviceId,bandwidth,index);
                    //If end point flow, give higher priority
                    insert_metering_rule(RecordType.END_POINTS,connectPoint.port(),deviceId,index,mplsLabel);
                    log.info("Inserting Metering Rule : Device: {}  ,   UplinkPort: {}  ,   Index: {}   ,   MatchingMplsLabel: {}", deviceId,connectPoint.port(),index,mplsLabel.toString());



                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Delete flow rules in response to device down
     * Deletion stick with routing service together to delete the flow rules affected
     * @param deviceId
     * @param mplsLabel
     * @param uplinkport
     * @return
     */
    public boolean deletingFlowRule(DeviceId deviceId, MplsLabel mplsLabel, Long uplinkport){
        if(deviceFlowRule.containsKey(deviceId)) {
            Set<ExtraInfoFlowRule> flowRulesSet = deviceFlowRule.get(deviceId);
            FlowRule toDelete;
            for(ExtraInfoFlowRule extraInfoFlowRule : flowRulesSet) {
                toDelete = extraInfoFlowRule.compareFlowRule(deviceId,mplsLabel,uplinkport);
                if (toDelete != null) {
                    flowRuleService.removeFlowRules(toDelete);
                    if(!meterIsReferenced(deviceId,extraInfoFlowRule, extraInfoFlowRule.getMeterIndex())) {
                        returningIndex(deviceId,extraInfoFlowRule.getMeterIndex());
                    }

                    deviceFlowRule.get(deviceId).remove(extraInfoFlowRule);

                    return true;
                }
            }
        }
        return false;
    }

    //return availableIndex and delete the metercellconfig using that index
    private void returningIndex(DeviceId deviceId, int index) {
        deviceMeterCellMetadata.get(deviceId).returnavailableIndex(index);

        ArrayList<MeterCellConfig> meterCellConfigs = deviceMeterCellConfig.get(deviceId);
        //to overcome concurrent modification error
        ArrayList<MeterCellConfig> deleting = new ArrayList<>();
        for(MeterCellConfig meterCellConfig : meterCellConfigs) {
            if (meterCellConfig.getIndex() == index) {
                deleting.add(meterCellConfig);
            }
        }
        for(MeterCellConfig meterCellConfig : deleting){
            deviceMeterCellConfig.get(deviceId).remove(meterCellConfig);
        }
    }

    //check if this meter index still referenced by any flow rule
    private boolean meterIsReferenced(DeviceId deviceId, ExtraInfoFlowRule exceptThisRule, int index){
            Set<ExtraInfoFlowRule> flowRuleSet = deviceFlowRule.get(deviceId);
            for(ExtraInfoFlowRule extraInfoFlowRule : flowRuleSet) {
                if(extraInfoFlowRule == exceptThisRule) {
                    continue;
                }
                if(extraInfoFlowRule.getMeterIndex() == index) {
                    return true;
                }
            }
            return false;
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
                    configForThis = new MeterCellConfig(networkId,recordType,connectPoint,deviceMeterCellMetadata.get(deviceId).getavailableIndex(networkId),sourcedest);
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
        PiMeterId meterId = PiMeterId.of("ingress.tenant_meter_ingress_control.tenant_port_meter");
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
        String TENANT_METER_CONTROL="ingress.tenant_meter_ingress_control.";
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


        insertPiFlowRule(deviceId, TENANT_UPLINK_TABLE, matchlabelout, execute_meter, mplsLabel,uplinkport.toLong(),metercellindex);

        //   insertPiFlowRule(DeviceId.deviceId("device:bmv2:s1"),TENANT_UPLINK_TABLE,match2,execute_meter2);

        PiTableId TENANT_FILTERING_TABLE = PiTableId.of(TENANT_METER_CONTROL + "tenant_uplink_meter_filtering_table");

        PiMatchFieldId PACKET_COLOUR = PiMatchFieldId.of("local_metadata.packet_colour");
        PiCriterion match_packet_colour = PiCriterion.builder().matchExact(PACKET_COLOUR,2).build();

        PiActionId _DROP = PiActionId.of(TENANT_METER_CONTROL + "_drop");
        PiAction droppacket= PiAction.builder().withId(_DROP).build();

        insertPiFlowRule(deviceId,TENANT_FILTERING_TABLE,match_packet_colour,droppacket);

//private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction)
    }

    private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction, MplsLabel mplsLabel, Long uplinkport, int metercellindex){
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

        //FIXME having a subclass to include the mpls label and uplink and meter index would be nice
        deviceFlowRule.get(deviceId).add(new ExtraInfoFlowRule(rule,deviceId,mplsLabel,uplinkport,metercellindex));
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

    private class ExtraInfoFlowRule {
        private FlowRule flowRule;
        private DeviceId deviceId;
        private MplsLabel mplsLabel;
        private Long uplinkport;
        private int metercellindex;

        public ExtraInfoFlowRule (FlowRule flowRule, DeviceId deviceId, MplsLabel mplsLabel, Long uplinkport, int metercellindex){
            this.flowRule = flowRule;
            this.deviceId = deviceId;
            this.mplsLabel = mplsLabel;
            this.uplinkport = uplinkport;
            this.metercellindex = metercellindex;
        }

        public FlowRule compareFlowRule(DeviceId deviceId, MplsLabel mplsLabel, Long uplinkport){
            if((this.deviceId == deviceId) && (this.mplsLabel == mplsLabel) && (this.uplinkport == uplinkport)) {
                return this.flowRule;
            } else {
                return null;
            }
        }
        public Integer getMeterIndex(){
            return this.metercellindex;
        }

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
                    if(!deviceMeterCellMetadata.containsKey(event.subject().id())) {
                        deviceMeterCellMetadata.put(event.subject().id(),new MeterMetadata());
                    }
                    if(!deviceMeterCellConfig.containsKey(event.subject().id())) {
                        deviceMeterCellConfig.put(event.subject().id(),new ArrayList<>());
                    }
                    if(!deviceFlowRule.containsKey(event.subject().id())) {
                        deviceFlowRule.put(event.subject().id(),new HashSet<>());
                    }
                    break;
                case DEVICE_UPDATED:
                    break;
                default:
                    break;

            }
        }
    }

    //config for Network : Uniquely identified by ; networkId, recordtype=NETWORK, connectPoint
    //config for flows : Uniquely identified by ; networkId, recordtype=END_POINTS, connectPoint, endPoints(source&destination for the flow)
    class MeterCellConfig{
        NetworkId networkId;
        RecordType recordtype;
        //connectpoint or port number for this metercell
        ConnectPoint connectPoint;
        Integer index;
        //if this meter is for endpoints
        Set<ConnectPoint> endPoints;
        //TODO
        //Add in bandwidth limit for this meter

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
        public boolean compareRecord(NetworkId networkId, RecordType recordType, Set<ConnectPoint> endPoints){
            if(recordType == RecordType.END_POINTS) {
                if(this.networkId.equals(networkId) && this.recordtype.equals(recordType) && this.endPoints.equals(endPoints)) {
                    return true;
                }
            } else {
                if(this.networkId.equals(networkId) && this.recordtype.equals(recordType) ) {
                    return true;
                }
            }
            return false;
        }


        public Integer getIndex() {
            return index;
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

        public void returnavailableIndex(Integer index) {
            if(occupiedIndex.contains(index)){
                occupiedIndex.remove(index);
                availableIndex.add(index);
            }
        }

        public Boolean anyEndPointMeter(NetworkId networkId){
            return availableEndPointsMeter.get(networkId);
        }
    }

    //fixme device listener class for device event if port down

}
