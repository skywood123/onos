package org.onosproject.meterconfiguration.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.MeteringService;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Command(scope = "onos", name = "displaycurrentbandwidth",description = "display bandwidth rate for a network")
public class displayCurrentBandwidth extends AbstractShellCommand  {

    @Argument(index = 0,name="seconds", description = "Num of seconds to display", required = true, multiValued = false)
    Long seconds = null;

    private BandwidthInventoryService service = get(BandwidthInventoryService.class);
    private MeteringService meterservice = get(MeteringService.class);

   // private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static int count = 0;
    //https://www.codota.com/web/assistant/code/rs/5c65655a1095a5000149bc12#L62

    @Reference
    Session session ;

    @Override
    protected void doExecute() throws Exception {

  /*
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
*/
        Runnable helloRunnable = new Runnable() {
            public void run() {
                try {
                    session.execute("clear");
                    meterservice.bandwidthRate();
                    count++;
                    System.out.println("Count = " + count + " ; seconds*2 = " + seconds*2);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

   //     ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
      //  ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(helloRunnable, 0, 500, TimeUnit.MILLISECONDS);

        while(true){
            try {
                session.execute("clear");
                meterservice.bandwidthRate();
                count++;
      //          System.out.println("Count = " + count + " ; seconds*2 = " + seconds*2);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(500);
            if(count == seconds*2){
            //    scheduledFuture.cancel(true);
            //    executor.shutdown();
                count = 0;
                break;
            }
        }

    }

}
