package org.onosproject.meterconfiguration.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.meterconfiguration.BandwidthInventoryService;
import org.onosproject.meterconfiguration.MeteringService;

@Service
@Command(scope = "onos", name = "debugging",description = "set bandwidth for a network")
public class meteringcli extends AbstractShellCommand {


    private BandwidthInventoryService service = get(BandwidthInventoryService.class);

    @Override
    protected void doExecute() throws Exception {
        service.printall();
    }
}
