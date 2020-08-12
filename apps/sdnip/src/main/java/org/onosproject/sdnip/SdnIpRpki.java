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

package org.onosproject.sdnip;

/**
 * Features to interact with BGP RPKI ROV.
 */
public interface SdnIpRpki {

    /**
     * Validate all known BGP routes.
     */
    public void validateAllRoutes();

    /**
     * Set RPKI Validator socket address.
     * @param ipPort Set RPKI Validator socket address (IP:PORT)
     */
    public void setvalidatorIp(String ipPort);

    /**
     * Enable RPKI feature and validate all known BGP routes.
     */
    public void enableRpki();

    /**
     * Disable RPKI feature and reinstall MP2SP intents for all known BGP routes.
     */
    public void disableRpki();

    /**
     * Print true if RPKI feature is enabled.
     */
    public void isRpkiEnabled();

    /**
     * Print RPKI Validator socket address.
     */
    public void getvalidatorIp();


}
