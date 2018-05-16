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
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { UrlFnService } from '../../../../app/fw/remote/urlfn.service';

/**
 * ONOS GUI -- Remote -- General Functions - Unit Tests
 */
describe('UrlFnService', () => {
    let log: LogService;
    const windowMock = <any>{ location: <any> { hostname: 'localhost' } };

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [UrlFnService,
                { provide: LogService, useValue: log },
                { provide: Window, useValue: windowMock },
            ]
        });
    });

    it('should be created', inject([UrlFnService], (service: UrlFnService) => {
        expect(service).toBeTruthy();
    }));
});
