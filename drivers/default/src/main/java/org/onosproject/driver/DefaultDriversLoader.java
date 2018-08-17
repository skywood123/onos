/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.driver;

import org.onosproject.net.driver.AbstractIndependentDriverLoader;
import org.onosproject.net.driver.DefaultDriverProviderService;
import org.osgi.service.component.annotations.Component;

/**
 * Loader for default device drivers.
 */
@Component(immediate = true, service = DefaultDriversLoader.class)
public class DefaultDriversLoader extends AbstractIndependentDriverLoader
        implements DefaultDriverProviderService {
    public DefaultDriversLoader() {
        super("/onos-drivers.xml");
    }
}
