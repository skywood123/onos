/*
 * Copyright 2018-present Open Networking Foundation
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
import { TestBed, async } from '@angular/core/testing';
import { RouterModule, RouterOutlet, ChildrenOutletContexts } from '@angular/router';
import { LogService } from '../../app/log.service';
import { ConsoleLoggerService } from '../../app/consolelogger.service';
import { IconComponent } from '../../app/fw/svg/icon/icon.component';
import { MastComponent } from '../../app/fw/mast/mast/mast.component';
import { NavComponent } from '../../app/fw/nav/nav/nav.component';
import { OnosComponent } from '../../app/onos.component';
import { DialogService } from '../../app/fw/layer/dialog.service';
import { EeService } from '../../app/fw/util/ee.service';
import { GlyphService } from '../../app/fw/svg/glyph.service';
import { IconService } from '../../app/fw/svg/icon.service';
import { KeyService } from '../../app/fw/util/key.service';
import { LionService } from '../../app/fw/util/lion.service';
import { NavService } from '../../app/fw/nav/nav.service';
import { OnosService } from '../../app/onos.service';
import { PanelService } from '../../app/fw/layer/panel.service';
import { QuickHelpService } from '../../app/fw/layer/quickhelp.service';
import { ThemeService } from '../../app/fw/util/theme.service';
import { SpriteService } from '../../app/fw/svg/sprite.service';
import { VeilService } from '../../app/fw/layer/veil.service';
import { WebSocketService } from '../../app/fw/remote/websocket.service';

class MockDialogService {}

class MockEeService {}

class MockGlyphService {}

class MockIconService {}

class MockKeyService {}

class MockLionService {}

class MockNavService {}

class MockOnosService {}

class MockPanelService {}

class MockQuickHelpService {}

class MockSpriteService {}

class MockThemeService {}

class MockVeilService {}

class MockWebSocketService {}

/**
 * ONOS GUI -- Onos Component - Unit Tests
 */
describe('OnosComponent', () => {
    let log: LogService;

    beforeEach(async(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            declarations: [
                IconComponent,
                MastComponent,
                NavComponent,
                OnosComponent,
                RouterOutlet
            ],
            providers: [
                { provide: ChildrenOutletContexts, useClass: ChildrenOutletContexts },
                { provide: DialogService, useClass: MockDialogService },
                { provide: EeService, useClass: MockEeService },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: IconService, useClass: MockIconService },
                { provide: KeyService, useClass: MockKeyService },
                { provide: LionService, useClass: MockLionService },
                { provide: LogService, useValue: log },
                { provide: NavService, useClass: MockNavService },
                { provide: OnosService, useClass: MockOnosService },
                { provide: QuickHelpService, useClass: MockQuickHelpService },
                { provide: PanelService, useClass: MockPanelService },
                { provide: SpriteService, useClass: MockSpriteService },
                { provide: ThemeService, useClass: MockThemeService },
                { provide: VeilService, useClass: MockVeilService },
                { provide: WebSocketService, useClass: MockWebSocketService },
            ]
        }).compileComponents();
    }));

    it('should create the app', async(() => {
        const fixture = TestBed.createComponent(OnosComponent);
        const app = fixture.debugElement.componentInstance;
        expect(app).toBeTruthy();
    }));

    it(`should have as title 'onos'`, async(() => {
        const fixture = TestBed.createComponent(OnosComponent);
        const app = fixture.debugElement.componentInstance;
        expect(app.title).toEqual('onos');
    }));
});
