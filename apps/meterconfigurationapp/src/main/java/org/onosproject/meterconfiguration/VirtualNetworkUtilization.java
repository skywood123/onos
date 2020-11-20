package org.onosproject.meterconfiguration;

import com.google.common.base.Strings;
import org.onosproject.drivers.p4runtime.AbstractP4RuntimeHandlerBehaviour;
import org.onosproject.grpc.utils.AbstractGrpcHandshaker;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * execute every second to query the counter from the dataplane to check the utilization of each network at each device's port
 * use scheduledexecutor service
 * read and check the difference with previous value and output current utilization value
 * UNABLE to clear off counter and reusing same counter index causing the counter value inaccurate; not outputing the counter value
 */
public class VirtualNetworkUtilization {



    //a list of networkId; each contain a list of device; each device contain list of port; each port has a value;
    private Map<NetworkId,Map<DeviceId,Map<PortNumber,Integer>>> utilizationStore = new HashMap<>();

    @Reference(cardinality =  ReferenceCardinality.MANDATORY)
    private VirtualNetworkService virtualNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DriverService driverService;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    public void activate(){

    }

    @Deactivate
    public void deactivate(){

    }



    /**
     * p4runtimereadclient reading the device
     */
    private void updateUtilization(){
            utilizationStore.keySet()
                    .stream()
                    .forEach(networkId -> {
                utilizationStore.get(networkId).keySet()
                        .stream()
                        .forEach(deviceId ->{

                        } );
                    }
                    );
    }

    private void clientReading(DeviceId deviceId){
        DriverHandler handler=driverService.createHandler(deviceId);
        P4RuntimeController controller = handler.get(P4RuntimeController.class);

        P4RuntimeClient client = controller.get(deviceId);
        PiPipeconfService piPipeconfService = handler.get(PiPipeconfService.class);

        PiPipeconf pipeconf = piPipeconfService.getPipeconf(deviceId).get();

        final BasicDeviceConfig cfg = handler.get(NetworkConfigService.class)
                .getConfig(deviceId, BasicDeviceConfig.class);
        URI device_mgmt_addr;
        if (cfg == null || Strings.isNullOrEmpty(cfg.managementAddress())) {
            log.error("Missing or invalid config for {}, cannot derive " +
                              "gRPC server endpoints", deviceId);
             device_mgmt_addr = null;

        }

        try {
            device_mgmt_addr= new URI(cfg.managementAddress());
        } catch (URISyntaxException e) {
            log.error("Management address of {} is not a valid URI: {}",
                      deviceId, cfg.managementAddress());
            device_mgmt_addr = null;
        }
        long p4DeviceId = extractP4DeviceId(device_mgmt_addr);

        PiCounterId virtual_network_counter = PiCounterId.of("ingress.tenant_meter_ingress_control.virtual_network_counters");
        P4RuntimeReadClient.ReadResponse response = client.read(p4DeviceId, pipeconf).counterCells(virtual_network_counter).submitSync();

        //TODO
        //extract response, update the corresponding port from the meter cell index of that device for this virtual network
    }

    private Long extractP4DeviceId(URI uri){
        if (uri == null) {
            return null;
        }
        String[] segments = uri.getRawQuery().split("&");
        try {
            for (String s : segments) {
                if (s.startsWith("device_id=")) {
                    return Long.parseUnsignedLong(
                            URLDecoder.decode(
                                    s.substring("device_id=".length()), "utf-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to decode P4Runtime-internal device_id from URI {}: {}",
                      uri, e.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid P4Runtime-internal device_id in URI {}: {}",
                      uri, e.toString());
        }
        log.error("Missing P4Runtime-internal device_id in URI {}", uri);
        return null;
    }


    private void queryUtilization(){
        Runnable helloRunnable = new Runnable() {
            public void run() {
                System.out.println("Hello world");
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(helloRunnable, 0, 1, TimeUnit.SECONDS);
    }
}
