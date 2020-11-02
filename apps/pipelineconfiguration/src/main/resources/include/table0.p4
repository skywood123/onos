/*
 * Copyright 2017-present Open Networking Foundation
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

#ifndef __TABLE0__
#define __TABLE0__

#include "headers.p4"
#include "defines.p4"

control table0_control(inout headers_t hdr,
                       inout local_metadata_t local_metadata,
                       inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) table0_counter;

    action set_next_hop_id(next_hop_id_t next_hop_id) {
        local_metadata.next_hop_id = next_hop_id;
    }

    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
    }

    action set_egress_port(port_t port) {               //old code  = set_out_port
        standard_metadata.egress_spec = port;
    }

    action drop() {
        mark_to_drop(standard_metadata);
    }

    //old code

        action mpls_push(bit<20> mpls_label_id,port_t port){
            hdr.mpls.setValid();
            hdr.mpls.label=mpls_label_id;
            hdr.mpls.exp=0;
            hdr.mpls.s=1;
            hdr.mpls.ttl=255;
            hdr.ethernet.ether_type=ethertype_mpls;
            set_egress_port(port);
        }

        //FIXME when 3 device used ( middle only swap the label and specify output port)
        // now in pipeconf if 2 instructions come will directly use mpls_pop and specify output port
        //TODO should add another action to swap the label only and specify outputport
        action mpls_pop(port_t port){
            hdr.mpls.setInvalid();
            hdr.ethernet.ether_type=ethertype_ipv4;
            set_egress_port(port);
       //     hdr.metadata.setInvalid();
        }

        action mpls_swap(bit<20> mpls_label_id,port_t port){
            hdr.mpls.label=mpls_label_id;
            set_egress_port(port);
            hdr.mpls.ttl=hdr.mpls.ttl-1;
        }
     //end of old code

    table table0 {                                     //old l2_fwd
        key = {
            standard_metadata.ingress_port : ternary;
            hdr.ethernet.src_addr          : ternary;
            hdr.ethernet.dst_addr          : ternary;
            hdr.ethernet.ether_type        : ternary;
            hdr.ipv4.src_addr              : ternary;
            hdr.ipv4.dst_addr              : ternary;
            hdr.ipv4.protocol              : ternary;
            hdr.mpls.label                 : ternary; //from old code
            hdr.mpls.s                     : ternary; //from old code
            local_metadata.l4_src_port     : ternary;
            local_metadata.l4_dst_port     : ternary;
        }
        actions = {
            set_egress_port;
            send_to_cpu;
            set_next_hop_id;
            mpls_push;
            mpls_pop;
            mpls_swap;
            drop;
        }
        const default_action = drop();
        counters = table0_counter;
    }

    apply {
        table0.apply();
     }
}

#endif