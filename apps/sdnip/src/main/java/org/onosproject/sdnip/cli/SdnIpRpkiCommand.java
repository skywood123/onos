/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.sdnip.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdnip.SdnIpService;
import org.onosproject.sdnip.cli.completer.SdnIpRpkiCommandCompleter;

/**
 * CLI to interact with SDN-IP RPKI features.
 */
@Service
@Command(scope = "onos", name = "rpki",
        description = "Manage SDN-IP RPKI features")
public class SdnIpRpkiCommand extends AbstractShellCommand {

    private SdnIpService sdnIpService;

    @Argument(index = 0, name = "command", description = "Command name (enable|" +
            "disable|show|set|validateallroutes)", required = true, multiValued = false)
    @Completion(SdnIpRpkiCommandCompleter.class)
    String command = null;

    @Argument(index = 1, name = "socketaddress", description = "Set SDN-IP RPKI Validator IP:PORT",
    required = false, multiValued = false)
    String socketaddress = null;

    @Override
    protected void doExecute() throws Exception {

        if (sdnIpService == null) {
            sdnIpService = get(SdnIpService.class);
        }

        SdnIpRpkiEnum enumCommand = SdnIpRpkiEnum.enumFromString(command);

        if (enumCommand != null) {
            switch (enumCommand) {
                case ENABLE:
                    sdnIpService.enableRpki();
                    break;
                case DISABLE:
                    sdnIpService.disableRpki();
                    break;
                case SHOW:
                    sdnIpService.isRpkiEnabled();
                    sdnIpService.getvalidatorIp();
                    break;
                case VALIDATE_ALL_ROUTES:
                    sdnIpService.validateAllRoutes();
                    break;
                case SET:
                    if (socketaddress != null) {
                        sdnIpService.setvalidatorIp(socketaddress);
                    } else {
                        print("validator IP:PORT not found!");
                    }
                    break;
                default:
                    print("command not found!");

            }
        } else {
            print("command not found!");
        }


    }
}
