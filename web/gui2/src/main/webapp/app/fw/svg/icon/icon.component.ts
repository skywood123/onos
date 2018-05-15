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
import { Component, OnInit, Input } from '@angular/core';
import { IconService, glyphMapping } from '../icon.service';
import { LogService } from '../../../log.service';
import * as d3 from 'd3';

/**
 * Icon Component
 *
 * Note: This is an alternative to the Icon Directive from ONOS 1.0.0
 * It has been implemented as a Component because it was inadvertently adding
 * in a template through d3 DOM manipulations - it's better to make it a Comp
 * and build a template the Angular 6 way
 *
 * Remember: The CSS files applied here only apply to this component
 */
@Component({
  selector: 'onos-icon',
  templateUrl: './icon.component.html',
  styleUrls: ['./icon.component.css', './icon.theme.css', './glyph.css', './glyph-theme.css']
})
export class IconComponent implements OnInit {
    @Input() iconId: string;
    @Input() iconSize: number = 20;

    constructor(
        private is: IconService,
        private log: LogService
    ) {
        // Note: iconId is not available until initialization
        this.log.debug('IconComponent constructed');
    }

    ngOnInit() {
        this.is.loadIconDef(this.iconId);
        this.log.debug('IconComponent initialized for ', this.iconId);
    }

    /**
     * Get the corresponding iconTag from the glyphMapping in the iconService
     * @returns The iconTag corresponding to the iconId of this instance
     */
    iconTag(): string {
        return '#' + glyphMapping.get(this.iconId);
    }
}
