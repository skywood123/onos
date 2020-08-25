package org.xzk.network_slicing.cli;


import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.xzk.network_slicing.netinfo;

@Service
@Command(scope = "onos", name = "getinfonetslice",description = "get netslicing info")
public class GetInfo extends AbstractShellCommand {

     private netinfo service;

    @Override
    protected void doExecute() throws Exception {
        service=get(netinfo.class);

        service.getinfo();
    }


}

