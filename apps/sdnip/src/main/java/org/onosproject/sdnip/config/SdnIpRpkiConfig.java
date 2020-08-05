/*
 * Copyright 2014-present Open Networking Foundation
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

package org.onosproject.sdnip.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration class for SDN-IP RPKI feature from JSON.
 */
public class SdnIpRpkiConfig extends Config<ApplicationId> {

    public static final String VALIDATOR_IP = "validatorIP";
    public static final String VALIDATOR_PORT = "validatorPort";
    public static final String RPKI_FEATURE_ENABLED = "rpkiEnabled";
    public static final String RPKICONFIG = "rpkiConfig";
    public static final String SDN_IP_RPKI = "sdnip-rpki";

    /**
     * Get SDN-IP RPKI configuration from JSON.
     * @return Configuration object for SDN-IP RPKI
     */
    public RpkiConfig getRpkiConfiguration() {

        JsonNode rpkiNode = object.get(RPKICONFIG);

        if (rpkiNode == null) {
            return null;
        }

            RpkiConfig rpkiConfiguration =
                    new RpkiConfig(
                            rpkiNode.get(VALIDATOR_IP).asText(),
                            rpkiNode.get(VALIDATOR_PORT).asText(),
                            rpkiNode.get(RPKI_FEATURE_ENABLED).asBoolean());

        return rpkiConfiguration;

    }

    /**
     * Configuration object for SDN-IP RPKI feature.
     */
    public static class RpkiConfig {
        private String validatorIP;
        private String validatorPort;
        private boolean rpki;

        public RpkiConfig(String validatorIP, String validatorPort, boolean rpki) {
            this.validatorIP = checkNotNull(validatorIP);
            this.validatorPort = checkNotNull(validatorPort);
            this.rpki = rpki;
        }

        public String validatorSocketaddress() {

            return validatorIP + ":" + validatorPort;
        }

        public Boolean isRpkiEnabled() {

            return rpki;
        }
    }

}

