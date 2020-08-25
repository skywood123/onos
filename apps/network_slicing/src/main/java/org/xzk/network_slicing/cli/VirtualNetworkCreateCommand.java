package org.xzk.network_slicing.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;

@Service
@Command(scope = "onos", name = "ns-create-virtual-network",
        description = "Creates a new virtual network for the specified tenant")
public class VirtualNetworkCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "tenantId", description = "Tenant ID",
            required = true, multiValued = false)
    String tenantId = null;


    @Override
    protected void doExecute() throws Exception {
        // TODO: Input verification

        VirtualNetworkAdminService virtualNetworkAdminService = getService(VirtualNetworkAdminService.class);
        VirtualNetwork virtualNetwork = virtualNetworkAdminService.createVirtualNetwork(TenantId.tenantId(tenantId));
        print("Virtual network (ID=" + virtualNetwork.id().toString() + ") is successfully created for tenant " + tenantId);
    }
}
