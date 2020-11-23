package org.onosproject.meterconfiguration.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.MeteringService;
import org.onosproject.meterconfiguration.Record;
import org.onosproject.meterconfiguration.RecordType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Command(scope = "onos", name = "displaycurrentbandwidth",description = "display bandwidth rate for a network")
public class displayCurrentBandwidth extends AbstractShellCommand  {



    private BandwidthInventoryService service = get(BandwidthInventoryService.class);
    private MeteringService meterservice = get(MeteringService.class);

    //https://www.codota.com/web/assistant/code/rs/5c65655a1095a5000149bc12#L62

    @Reference
    Session session ;

    @Override
    protected void doExecute() throws Exception {
        try {
        //    Process process = Runtime.getRuntime().exec("clear");
            session.execute("clear");
        //    execute("clear");
            meterservice.bandwidthRate();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
/*
        Runnable helloRunnable = new Runnable() {
            public void run() {
                try {
                    Process process = Runtime.getRuntime().exec("clear");
                    execute("clear");
                    meterservice.bandwidthRate();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(helloRunnable, 0, 500, TimeUnit.MILLISECONDS);

 */
    }

}
