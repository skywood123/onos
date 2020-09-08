package org.onosproject.meterconfiguration.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.meterconfiguration.MeteringService;

@Service
@Command(scope = "onos", name = "debuggingmeter",description = "set bandwidth for a network")
public class meteringcli extends AbstractShellCommand {


    private MeteringService service = get(MeteringService.class);

    @Override
    protected void doExecute() throws Exception {
        print("Hello World.");
    }
}
