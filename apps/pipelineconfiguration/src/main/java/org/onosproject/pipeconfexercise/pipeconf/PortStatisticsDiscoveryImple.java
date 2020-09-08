package org.onosproject.pipeconfexercise.pipeconf;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PortStatisticsDiscoveryImple extends AbstractHandlerBehaviour implements PortStatisticsDiscovery {

    private static final long DEFAULT_P4_DEVICE_ID = 1;
    //FIXME
    // Previous testing if all devices using same device id
    // something causing connection failure between bmv2 and onos
    //maybe can extract long number in deviceid name as the p4device id ?
    protected final Logger log = LoggerFactory.getLogger(getClass());


    public PiCounterId ingressCounterId(){
        return PiCounterId.of("ingress_control.port_counter_ingress_control.ingress_port_counter");
    }

    public PiCounterId egressCounterId(){
        return PiCounterId.of("egress_control.port_counter_egress_control.egress_port_counter");
    }

    private void setmeterconfig(){
        PiMeterBand meterband=new PiMeterBand(1,1);
        PiMeterBand meterband2=new PiMeterBand(1,1);
        PiMeterId meterId = PiMeterId.of("ingress_control.tenant_meter_ingress_control.tenant_port_meter");
        PiMeterCellId cellId=PiMeterCellId.ofIndirect(meterId, 1);
        PiMeterCellConfig meters3=PiMeterCellConfig.builder()
                                    .withMeterBand(meterband)
                                    .withMeterBand(meterband2)
                                    .withMeterCellId(cellId)
                                    .build();
        DeviceId deviceId = this.data().deviceId();
     //   DeviceId deviceId = DeviceId.deviceId("device:bmv2:s3");
       // DriverHandler handler=driverService.createHandler(deviceId);
        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        P4RuntimeClient client = controller.get(deviceId);

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
    //    PiPipeconf pipeconf = piPipeconfService.getPipeconf(deviceId).get();
        Optional<PiPipeconf> pipeconf = piPipeconfService.getPipeconf(deviceId);
        if (pipeconf.equals(null)){
            log.warn("Pipeconf for {} is not found!", deviceId);
            return;
        }

        //TODO
        //client has no api to get (long p4deviceid)
        //how to get it ?
        //Default to 1 ?
        //https://github.com/skywood123/onos/blob/onos-2.1-sdniprpki/pipelines/basic/src/main/java/org/onosproject/pipelines/basic/PortStatisticsDiscoveryImpl.java
        P4RuntimeWriteClient.WriteResponse response = client.write(DEFAULT_P4_DEVICE_ID,pipeconf.get())
                                                            .entity(meters3, P4RuntimeWriteClient.UpdateType.MODIFY)
                                                            .submitSync();
        log.warn("write result is " + response.isSuccess());
        if(!response.isSuccess())
            log.warn(response.toString());
   //     if (response.isSuccess())
      //      getmeterconfig();

     //   else
       //     log.warn("dk what is happening :(");
    }

    /**
     * Returns a list of port statistics descriptions appropriately annotated
     * to support downstream model extension via projections of their parent
     * device.
     *
     * @return annotated port statistics
     */
    @Override
    public Collection<PortStatistics> discoverPortStatistics() {

        //Service for interacting with the inventory of infrastructure devices.
        //.handler() return DriverHandler
        //DriverHandler : Representation of context for interacting with a device.
        //DriverHandler.get(Class) Returns the reference to the implementation of the specified service.
        DeviceService deviceService = this.handler().get(DeviceService.class);
   //     log.warn("what is the handler?");
    //    log.warn(handler().data().driver().name());

        //Behaviour.data() Returns the driver data context. Return Type = DriverData
        //DriverData.deviceId() Returns the device identifier.
        DeviceId deviceId = this.data().deviceId();

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        //.ofDevice() Returns the pipeconf identifier currently associated with the given device identifier, if present.
        //In here, just checking whether this device belongs to any pipeconf or does the pipeconf exist
        if (!piPipeconfService.getPipeconf(deviceId).isPresent()) {
            log.warn("Unable to get the pipeconf of {}, aborting operation", deviceId);
            return Collections.emptyList();
        }
        //get the corresponding pipeconf interface for that device pipeconf
        //deprecated in onos2.1
        Optional<PiPipeconf> pipeconf = piPipeconfService.getPipeconf(deviceId);

        //code at onos/protocols/p4runtime/api/src/main/java/org/onosproject/p4runtime/api/
        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        //removed ald, exist at older onos version
       // if (!controller.hasClient(deviceId)) {
          if(controller.get(deviceId)==null){
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return Collections.emptyList();
        }


        P4RuntimeClient client = controller.get(deviceId);

        Map<Long, DefaultPortStatistics.Builder> portStatBuilders = Maps.newHashMap();

        deviceService.getPorts(deviceId)
                .forEach(p -> portStatBuilders.put(p.number().toLong(),
                                                   DefaultPortStatistics.builder()
                                                           .setPort(p.number())
                                                           .setDeviceId(deviceId)));

        Set<PiCounterCellId> counterCellIds = Sets.newHashSet();
        //LambdaParameters -> LambdaBody
        portStatBuilders.keySet().forEach(p -> {
            // Counter cell/index = port number.
            counterCellIds.add(PiCounterCellId.ofIndirect(ingressCounterId(), p));
            counterCellIds.add(PiCounterCellId.ofIndirect(egressCounterId(), p));
        });

        Set<PiCounterId> counterset = Sets.newHashSet();
        counterset.add(ingressCounterId());
        counterset.add(egressCounterId());

        Collection<PiCounterCell> counterEntryResponse;

        P4RuntimeReadClient.ReadResponse readcounterresponse;
        //P4RuntimeReadClient.ReadResponse readmetercell;

        try {
                  readcounterresponse=client.read(DEFAULT_P4_DEVICE_ID,pipeconf.get()).counterCells(counterset).submitSync();
       //           readmetercell=client.read(pipeconf).meterCells(PiMeterId.of("ingress_control.tenant_meter_ingress_control" +
         //                                                                                                              ".tenant_port_meter")).submitSync();

            //    counterEntryResponse = client.readCounterCells(counterCellIds, pipeconf).get();
        } catch (Exception e) {
            log.warn("Exception while reading port counters from {}: {}", deviceId, e.toString());
            log.debug("", e);
            return Collections.emptyList();
        }
        log.debug("Printing point....................");

        counterEntryResponse=readcounterresponse.all(PiCounterCell.class);
     //   setmeterconfig();
        /*

         */
     //   Collection<PiMeterCellConfig> meterresponse=readmetercell.all(PiMeterCellConfig.class);
     //   log.warn("Printing meterresponse");
     //   log.warn(meterresponse.toString());
        //returning
        //PiCounterCell{cellId=ingress_control.port_counter_ingress_control.ingress_port_counter:1, counterData=PiCounterCellData{packets=22, bytes=2767}
   //     log.debug(response.toString());
   //     log.warn(response.toString());
   //     System.out.println(response.toString());
   //     for(PiCounterCell s : response)
   //         counterEntryResponse.add(s.data());
        //counterEntryResponse: a collection(list of) PiCounterCell, with [cellId and counterData]
     //   log.warn("Printing key set ....................");
     //   log.warn(portStatBuilders.keySet().toString());

        counterEntryResponse.forEach(counterCell -> {
            if (counterCell.cellId().counterType() != PiCounterType.INDIRECT) {
                log.warn("Invalid counter data type {}, skipping countertype = ", counterCell.cellId().counterType());
                return;
            }

           // log.warn("portstatbuilders " + portStatBuilders.size());
           // log.warn(portStatBuilders.entrySet().toString());
            //FIXME now the range for the cell/index contain many empty one
            //FIXME port for experiment only have 3 ports, the rest is empty
            //FIXME porstatebuilders keyset only have 3 because it's following the number of port for that deviceId
            if (!portStatBuilders.containsKey(counterCell.cellId().index())) {
           //     log.warn("index = " + counterCell.cellId().index());
           //     log.warn("Unrecognized counter index {}, skipping countercell = ", counterCell.cellId().index());
                return;
            }
            DefaultPortStatistics.Builder statsBuilder = portStatBuilders.get(counterCell.cellId().index());
            if (counterCell.cellId().counterId().equals(ingressCounterId())) {
                statsBuilder.setPacketsReceived(counterCell.data().packets());
                statsBuilder.setBytesReceived(counterCell.data().bytes());
            } else if (counterCell.cellId().counterId().equals(egressCounterId())) {
                statsBuilder.setPacketsSent(counterCell.data().packets());
                statsBuilder.setBytesSent(counterCell.data().bytes());
            } else {
                log.warn("Unrecognized counter ID {}, skipping countercell =", counterCell);
            }
        });
     //   log.warn("Successfully reach end of the code");

        return portStatBuilders
                .values()
                .stream()
                .map(DefaultPortStatistics.Builder::build)
                .collect(Collectors.toList());

    }
}
