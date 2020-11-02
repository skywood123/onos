package org.onosproject.meterconfiguration;


//import org.apache.felix.scr.annotations.Activate;
import org.onosproject.net.TenantId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
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
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

//@Component(immediate = true)
//@Service
public class Meterconfig implements p4meterservice {


    private final Logger log = LoggerFactory.getLogger(getClass());
  //  private final DeviceListener deviceListener = new NewdeviceListerner();
    private final PiPipeconfWatchdogListener watchdogListener = new NewdeviceListerner();

    private final long DEFAULT_P4DEVICEID = 1;
    private ApplicationId appId;

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


   // @Activate
  //  public void activate() {

  //      log.info("Registering listener to device service");
   //     deviceService.addListener(deviceListener);
    //    appId= coreService.registerApplication("org.onosproject.meterconfig");
    //    watchdogService.addListener(watchdogListener);
   //     log.info("merterconfig started");
     //   getmeterconfig();
        //setmeterconfig();
        //insert_metering_rule();
    //    set_bandwidth(NetworkId.networkId(1),5);
  //  }

   // @Deactivate
  //  public void deactivate(){
  //      log.info("Removing meterconfig flow rules...");
    //    flowRuleService.removeFlowRulesById(appId);
  //      log.info("Stopped");
   // }

    private static final List<String> tenantList = new ArrayList<String>();

    //private static HashMap<String,HashMap<ConnectPoint,Integer>> TENANT_PORT_METER_MAPPING = new HashMap<>();
    //List of HashMap
    private static HashMap<DeviceId, ArrayList<PortNumber>> DEVICE_PORT_MAPPING;
    private static List<HashMap<DeviceId,ArrayList<PortNumber>>> tenantInfo = new ArrayList<>();

    private static List<Integer> tenantbandwidth = new ArrayList<>();

    //from cli then call this to set bandwidth for that network
    public void set_bandwidth(NetworkId networkId, int bandwidth)  {
        String tenantId = update_tenant_list(networkId).toString();
        int tenantbwlocator = get_tenantnum(tenantId);
        tenantbandwidth.add(tenantbwlocator,bandwidth);
            // TODO set bandwidth
         //       get_Devices_uplink(networkId, bandwidth);

        //FIXME tenant-wide device meter should get updated. now only on flow rule time
        //make use of checkdeviceportmapping to get meterindex, port looking into tenantinfo for the ports

        if(!tenantInfo.isEmpty() && tenantInfo.size()>tenantbwlocator){
            DEVICE_PORT_MAPPING=tenantInfo.get(tenantbwlocator);
            for(DeviceId deviceId : DEVICE_PORT_MAPPING.keySet()){
                for(int i =0;i<DEVICE_PORT_MAPPING.get(deviceId).size();i++){
                    setmeterconfig(tenantbandwidth.get(tenantbwlocator),deviceId,tenantbwlocator*5+i);
                }

            }

        }


    }




    private int checkdeviceportmapping(int tenantnum,DeviceId deviceId,PortNumber outport){
      //  if(tenantInfo.isEmpty())
      //      tenantInfo.add(tenantnum,new HashMap<>());

      //  if(tenantInfo)

        DEVICE_PORT_MAPPING=tenantInfo.get(tenantnum);

        if(!DEVICE_PORT_MAPPING.containsKey(deviceId))
            DEVICE_PORT_MAPPING.put(deviceId,new ArrayList<>());

        if (DEVICE_PORT_MAPPING.get(deviceId).contains(outport)) {
            return DEVICE_PORT_MAPPING.get(deviceId).indexOf(outport);
        } else {
            DEVICE_PORT_MAPPING.get(deviceId).add(outport);
            return DEVICE_PORT_MAPPING.get(deviceId).indexOf(outport);
        }
    }
    //call from network slicing app when creating mpls flow
    public void match_mpls_p4meter(DeviceId deviceId, MplsLabel label,PortNumber outport, NetworkId networkId) {

        //insert_metering_rule(PortNumber uplinkport, DeviceId deviceId, int metercellindex, int tenantid, MplsLabel mplsLabel)
        TenantId tenantId = update_tenant_list(networkId);
        int tenantnum = get_tenantnum(tenantId.id());

        int var = checkdeviceportmapping(tenantnum,deviceId,outport);

        //assume max 5 , but not enforced here

    //setmeterconfig(int bandwidth,DeviceId deviceId,int metercellindex)
        setmeterconfig(tenantbandwidth.get(tenantnum),deviceId,tenantnum*5+var);

      /*  String connectionpoint = deviceId+":"+outport;

        if(PORT_METER_MAPPING.get(tenantnum).isEmpty())
            PORT_METER_MAPPING.add(tenantnum,new HashMap<>());
        PORT_METER_MAPPING.get(tenantnum).put(connectionpoint ,);

        */
        //TODO get device uplink ports?
        //matching mpls label?
        //create a hashmap to store per tenant (outport and metercell) linking
        //metercell is per device
        insert_metering_rule(outport , deviceId , tenantnum*5+var ,label);
    }

    private int get_tenantnum(String tenantId){
        for(int i=0;i<tenantList.size();i++){
            if(tenantList.get(i).equals(tenantId))
                return i;
        }
        return -1;
    }

    private TenantId update_tenant_list(NetworkId networkId){
        TenantId tenantId=virtualNetworkService.getTenantId(networkId);

        boolean isTenantexist=false;

        for(int i=0;i<tenantList.size();i++){
            if (tenantList.get(i).equals(tenantId.id())) {
                isTenantexist = true;
                break;
            }
        }
        if(isTenantexist==false){
            tenantList.add(tenantId.id());
            tenantInfo.add(new HashMap<>());


        }
     //   TENANT_PORT_METER_MAPPING.put(tenantId.id(),null);
        return tenantId;
    }

/* *//*
    //deprecated method ( this is previous implementation to insert flow rule for the entire network at once)
    private void get_Devices_uplink(NetworkId networkId, int bandwidth){
        TenantId tenantId=update_tenant_list(networkId);
      /*
      //  if (!tenantList.contains(tenantId.id()))
      //      tenantList.add(tenantId.id());
        boolean isTenantexist=false;

        for(int i=0;i<tenantList.size();i++){
            if (tenantList.get(i).equals(tenantId.id())) {
                isTenantexist = true;
                break;
            }
        }
        if(isTenantexist==false)
            tenantList.add(tenantId.id());
       *//*

        // list of links in this network, contain links from multiple device
        // onos> onos:vnet-links 1
        // src=device:bmv2:s4/1, dst=device:bmv2:s3/3, state=ACTIVE, tunnelId=null
        //this part will get
        List<VirtualLink> virtualLinks = new ArrayList<>();
        virtualLinks.addAll(virtualNetworkService.getVirtualLinks(networkId));
        ConnectPoint abc= virtualLinks.get(0).src();
        int tenantindex=get_tenantnum(tenantId.id());
        //list of connection point for sw-sw
        //egress port is here
        List<ConnectPoint> uplink_connectionPoints = new ArrayList<>();
        List<DeviceId> deviceInvolved = new ArrayList<>();
        for(VirtualLink link : virtualLinks){
            if (!uplink_connectionPoints.contains(link.src()))
                uplink_connectionPoints.add(link.src());
            if (!uplink_connectionPoints.contains(link.dst()))
                uplink_connectionPoints.add(link.dst());

            if(!deviceInvolved.contains(link.src().deviceId()))
                deviceInvolved.add(link.src().deviceId());
            if(!deviceInvolved.contains(link.dst().deviceId()))
                deviceInvolved.add(link.dst().deviceId());
        }

        //getting ingress port


        //getting virtual ports of a device for this network
        //get list of ports from above part which check list of devices



        for(DeviceId deviceId: deviceInvolved) {
            //virtual port id maybe realized by different id from physical
            //so separate it out here
            List<VirtualPort> virtualPorts = new ArrayList<>();
            List<ConnectPoint> device_connectionPoints = new CopyOnWriteArrayList<>();
            virtualPorts.addAll(virtualNetworkService.getVirtualPorts(networkId, deviceId));


            for (VirtualPort virtualPort : virtualPorts) {
                if (virtualPort.realizedBy() != null)
                    device_connectionPoints.add(virtualPort.realizedBy());
            }
            //extract out the uplinks
            List<ConnectPoint> device_uplinkPoints = new ArrayList<>();

            for (ConnectPoint deviceuplinkpoint : uplink_connectionPoints) {
                //extract switch uplink into device_uplinkPoints
                if (deviceuplinkpoint.deviceId().equals(deviceId))
                    device_uplinkPoints.add(deviceuplinkpoint);
            }

            for (ConnectPoint deviceport : device_connectionPoints) {
                //remove the uplinks for this device in device_connectionPoints
           //     for(ConnectPoint uplink: device_uplinkPoints)
           //         if(uplink.equals(deviceport))
                if (device_uplinkPoints.contains(deviceport)) {
                    device_connectionPoints.remove(deviceport);
                }
            }

            //insert rule matching all the input port to all the output port
            //assume each tenant max uplink = 5; meterindex=
            for (int i=0;i<device_uplinkPoints.size();i++) {
                //ConnectPoint outport : device_uplinkPoints

                for (ConnectPoint inport : device_connectionPoints) {
                    if (tenantindex != -1)
                        insert_metering_rule(inport.port(), device_uplinkPoints.get(i).port(), deviceId, (tenantindex * 5) + i, tenantindex);
                    else
                        log.warn("Tenant not found!");
                }

                if (tenantindex != -1)
                    setmeterconfig(bandwidth, deviceId, (tenantindex * 5) + i);
                else
                    log.warn("Tenant not found!");

                //make rule fow swport-swport in same device
                //match for tenant id and outport
                //inport dont care
                //no need loop? as only creating rules for outgoing port
                if (device_uplinkPoints.size() > 1)
                    for (int j = 0; j < device_uplinkPoints.size(); j++) {
                        if(i==j)
                            continue;
                        insert_metering_sw_sw_rule(device_uplinkPoints.get(j).port(), device_uplinkPoints.get(i).port(), deviceId, (tenantindex * 5) + i, tenantindex);

                    }
            }
            }

        }

        */

    //make rule for sw-sw traffic
    private void insert_metering_sw_sw_rule(PortNumber inport,PortNumber uplinkport, DeviceId deviceId, int metercellindex,int tenantid){
        String TENANT_METER_CONTROL="ingress_control.tenant_meter_ingress_control.";
        PiTableId TENANT_UPLINK_TABLE= PiTableId.of(TENANT_METER_CONTROL+ "tenant_uplink_meter_table");

        PiMatchFieldId INGRESS_PORT = PiMatchFieldId.of("standard_metadata.ingress_port");
        PiMatchFieldId EGRESS_PORT = PiMatchFieldId.of("standard_metadata.egress_spec");
       // PiCriterion matchegress = PiCriterion.builder().matchExact(EGRESS_PORT,uplinkport.toLong()).build();
        //    PiCriterion match2 = PiCriterion.builder().matchExact(EGRESS_PORT,2).build();

        PiMatchFieldId MATCH_TENANT_ID = PiMatchFieldId.of("hdr.metadata.tenant_id");
        PiCriterion matchtenant = PiCriterion.builder().matchTernary(INGRESS_PORT,inport.toLong(),0xff).matchTernary(EGRESS_PORT,uplinkport.toLong(),0xff).matchTernary(MATCH_TENANT_ID,tenantid,0xff).build();

        PiActionId READ_METER_AND_TAG  = PiActionId.of(TENANT_METER_CONTROL + "read_meter_and_tag");

        PiActionParamId TENANT_OUTPORT = PiActionParamId.of("tenant_outport");
        PiActionParamId TENANT_ID = PiActionParamId.of("tenant_id");
        PiActionParam meterindex = new PiActionParam(TENANT_OUTPORT,metercellindex);
        PiActionParam tagTenantId = new PiActionParam(TENANT_ID,tenantid);
        //    PiActionParam meterindex2 = new PiActionParam(TENANT_OUTPORT,metercellindex);

        PiAction execute_meter= PiAction.builder().withId(READ_METER_AND_TAG).withParameter(meterindex).withParameter(tagTenantId).build();
        //    PiAction execute_meter2= PiAction.builder().withId(READ_METER_AND_TAG).withParameter(meterindex2).build();

        insertPiFlowRulelowprio(deviceId,TENANT_UPLINK_TABLE,matchtenant,execute_meter);
        //   insertPiFlowRule(DeviceId.deviceId("device:bmv2:s1"),TENANT_UPLINK_TABLE,match2,execute_meter2);

        PiTableId TENANT_FILTERING_TABLE = PiTableId.of(TENANT_METER_CONTROL + "tenant_uplink_meter_filtering_table");

        PiMatchFieldId PACKET_COLOUR = PiMatchFieldId.of("hdr.metadata.packet_colour");
        PiCriterion match_packet_colour = PiCriterion.builder().matchExact(PACKET_COLOUR,2).build();

        PiActionId _DROP = PiActionId.of(TENANT_METER_CONTROL + "_drop");
        PiAction droppacket= PiAction.builder().withId(_DROP).build();

        insertPiFlowRule(deviceId,TENANT_FILTERING_TABLE,match_packet_colour,droppacket);

//private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction)
    }




    //insert flow rule to direct traffic match against metercell index from edge port to uplink port

    private void insert_metering_rule(PortNumber uplinkport, DeviceId deviceId, int metercellindex, MplsLabel mplsLabel){
        String TENANT_METER_CONTROL="ingress.tenant_meter_ingress_control.";
        PiTableId TENANT_UPLINK_TABLE= PiTableId.of(TENANT_METER_CONTROL+ "tenant_uplink_meter_table");


        PiMatchFieldId EGRESS_PORT = PiMatchFieldId.of("standard_metadata.egress_spec");
       // PiCriterion matchegress = PiCriterion.builder().matchExact(EGRESS_PORT,uplinkport.toLong()).build();
    //    PiCriterion match2 = PiCriterion.builder().matchExact(EGRESS_PORT,2).build();

        //PiMatchFieldId INGRESS_PORT = PiMatchFieldId.of("standard_metadata.ingress_port");

       // PiCriterion matchinout = PiCriterion.builder().matchTernary(INGRESS_PORT,inport.toLong(),0xff).matchTernary(EGRESS_PORT,uplinkport.toLong(),0xff).build();
        PiMatchFieldId LABEL=PiMatchFieldId.of("hdr.mpls.label");
        PiCriterion matchlabelout=PiCriterion.builder().matchExact(LABEL,mplsLabel.toInt()).matchExact(EGRESS_PORT,uplinkport.toLong()).build();
        PiActionId READ_METER_AND_TAG  = PiActionId.of(TENANT_METER_CONTROL + "read_meter_and_tag");

        PiActionParamId TENANT_OUTPORT = PiActionParamId.of("tenant_outport");
        PiActionParamId TENANT_ID = PiActionParamId.of("tenant_id");
        PiActionParam meterindex = new PiActionParam(TENANT_OUTPORT,metercellindex);
    //    PiActionParam meterindex2 = new PiActionParam(TENANT_OUTPORT,metercellindex);

        PiAction execute_meter= PiAction.builder().withId(READ_METER_AND_TAG).withParameter(meterindex).build();
    //    PiAction execute_meter2= PiAction.builder().withId(READ_METER_AND_TAG).withParameter(meterindex2).build();

        insertPiFlowRule(deviceId,TENANT_UPLINK_TABLE,matchlabelout,execute_meter);
     //   insertPiFlowRule(DeviceId.deviceId("device:bmv2:s1"),TENANT_UPLINK_TABLE,match2,execute_meter2);

        PiTableId TENANT_FILTERING_TABLE = PiTableId.of(TENANT_METER_CONTROL + "tenant_uplink_meter_filtering_table");

        PiMatchFieldId PACKET_COLOUR = PiMatchFieldId.of("local_metadata.packet_colour");
        PiCriterion match_packet_colour = PiCriterion.builder().matchExact(PACKET_COLOUR,2).build();

        PiActionId _DROP = PiActionId.of(TENANT_METER_CONTROL + "_drop");
        PiAction droppacket= PiAction.builder().withId(_DROP).build();

        insertPiFlowRule(deviceId,TENANT_FILTERING_TABLE,match_packet_colour,droppacket);

//private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction)
    }

    //TODO
    //input tenant id and device connection points and bandwidth limit

    //input in Mbit
    private void setmeterconfig(int bandwidth,DeviceId deviceId,int metercellindex){
            int rate = (bandwidth*1000000)/8;
            PiMeterBand meterband=new PiMeterBand(rate,3000);
       //     PiMeterBand meterband2=new PiMeterBand(625000,3000);
            PiMeterId meterId = PiMeterId.of("ingress.tenant_meter_ingress_control.tenant_port_meter");
            PiMeterCellId cellId=PiMeterCellId.ofIndirect(meterId,metercellindex);
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

        P4RuntimeWriteClient.WriteResponse response = client.write(DEFAULT_P4DEVICEID,pipeconf).entity(meter1, P4RuntimeWriteClient.UpdateType.MODIFY).submitSync();
    //    P4RuntimeWriteClient.WriteResponse response2 = client.write(pipeconf).entity(meter2,P4RuntimeWriteClient.UpdateType.MODIFY).submitSync();
        log.warn("write1 result is " + response.isSuccess());
      //  log.warn("write2 result is " + response.isSuccess());
    //    if (response.isSuccess())
      //      getmeterconfig();
    //    else
     //       log.warn("dk what is happening :(");
    }

    private void getmeterconfig(){
        PiMeterId tenant_outport_meter = PiMeterId.of("ingress.tenant_meter_ingress_control" +
                                                              ".tenant_port_meter");
        DeviceId deviceId = DeviceId.deviceId("device:bmv2:s3");
      //  DeviceService deviceService = handler().get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);

        Driver bmv2=driverService.getDriver(deviceId);
      //  P4RuntimeController controller2=driverService.createHandler(deviceId).get(P4RuntimeController.class);
        Collection collect = bmv2.behaviours();
        //PiMeterBand band= new PiMeterBand(5000,10000);

        //to deal with the device driver directly
        DriverHandler handler=driverService.createHandler(deviceId);

       // log.warn(collect.toString());

      //  log.warn("reach point 1");

    //    log.warn("printing handler data = " + handler().data().toString());
     //   log.info("wtf");
        P4RuntimeController controller = handler.get(P4RuntimeController.class);

        log.warn("reach point 2");

        if(controller.get(deviceId)==null){
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return ;
        }
        P4RuntimeClient client = controller.get(deviceId);
        log.warn("reach point 3");
        PiPipeconfService piPipeconfService = handler.get(PiPipeconfService.class);
        PiPipeconf pipeconf = piPipeconfService.getPipeconf(deviceId).get();
        log.warn("reach point 4");
        try{
            //FIXME error reading meter spec ?
            P4RuntimeReadClient.ReadResponse result=client.read(DEFAULT_P4DEVICEID,pipeconf).meterCells(tenant_outport_meter).submitSync();
            log.warn("error occured during read = " + result.explanation());
            Collection<PiMeterCellConfig> meterCellConfigs = result.all(PiMeterCellConfig.class);
            log.warn("What is the output from metercellconfig ?? ");
            log.warn(meterCellConfigs.toString());

        }catch(Exception e){
            log.warn("Unknown error occured");
        }



    }

    private void insertPiFlowRule(DeviceId deviceId, PiTableId tableId, PortNumber piCriterion,PiCriterion piCriterion2, PiAction piAction){
            FlowRule rule = DefaultFlowRule.builder().forDevice(deviceId)
                    .forTable(tableId)
                    .withPriority(1000)
                    .fromApp(appId)
                    .makePermanent()
                    .withSelector(DefaultTrafficSelector.builder().matchInPort(piCriterion).matchPi(piCriterion2).build())
                    .withTreatment(DefaultTrafficTreatment.builder().piTableAction(piAction).build())
                    .build();
            log.warn(rule.toString());
            flowRuleService.applyFlowRules(rule);

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
    private void insertPiFlowRulelowprio(DeviceId deviceId, PiTableId tableId, PiCriterion piCriterion, PiAction piAction){
        FlowRule rule = DefaultFlowRule.builder().forDevice(deviceId)
                .forTable(tableId)
                .withPriority(500)
                .fromApp(appId)
                .makePermanent()
                .withSelector(DefaultTrafficSelector.builder().matchPi(piCriterion).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(piAction).build())
                .build();
        flowRuleService.applyFlowRules(rule);
    }

    private class NewdeviceListerner implements PiPipeconfWatchdogListener {
        @Override
        public void event(PiPipeconfWatchdogEvent event) {
                if(event.type()!=PiPipeconfWatchdogEvent.Type.PIPELINE_READY)
                    return;
      //          synchronized (this){
                DeviceId deviceId=event.subject();
//                getmeterconfig(deviceId);
    //    }
        }
/*
        @Override
        public void event(DeviceEvent event) {
                if(event.type()!=DeviceEvent.Type.DEVICE_ADDED) {
                    return;
                }
            Device device=event.subject();

            getmeterconfig(device);
        }

 */

    }








}
