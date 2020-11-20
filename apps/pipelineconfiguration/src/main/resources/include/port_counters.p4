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

#ifndef __PORT_COUNTERS__
#define __PORT_COUNTERS__

#include "headers.p4"
#include "defines.p4"

control port_counters_ingress(inout headers_t hdr,
                              inout standard_metadata_t standard_metadata) {

   // counter(MAX_PORTS, CounterType.packets) ingress_port_counter;
      counter(MAX_PORTS,CounterType.packets_and_bytes) ingress_port_counter;  //old code
    apply {
        ingress_port_counter.count((bit<32>) standard_metadata.ingress_port);
    }
}

control port_counters_egress(inout headers_t hdr,
                             inout standard_metadata_t standard_metadata) {

  //  counter(MAX_PORTS, CounterType.packets) egress_port_counter;
      counter(MAX_PORTS,CounterType.packets_and_bytes) egress_port_counter; // old code
      register<bit<32>>(MAX_PORTS) port_bytes;
    apply {
        egress_port_counter.count((bit<32>) standard_metadata.egress_port);
        bit<32> current_value=0;
        //void read(out T result, in bit<32> index);
        port_bytes.read(current_value,(bit<32>)standard_metadata.egress_port);

        current_value = current_value + standard_metadata.packet_length;

        //void write(in bit<32> index, in T value);
        port_bytes.write((bit<32>)standard_metadata.egress_port,current_value);

        if(hdr.int_egress_bytes.isValid()){
            hdr.int_egress_bytes.egress_port_bytes = current_value;
        }

    }
}

//if place at egress, the mpls label is after processed mpls.
//push and swap : mpls header is valid ; pop will be invalid
//if there is mpls label, means it is on the shared link
/*
control virtual_network_counters(inout headers_t hdr,
                             inout standard_metadata_t standard_metadata) {

      counter(MPLS_LABELS,CounterType.packets_and_bytes) virtual_network_counters; // old code

    apply {
        //counting mpls label ?
        if(hdr.mpls.isValid()){
        virtual_network_counters.count((bit<32>) hdr.mpls.label);

        }


    }
}
*/
#endif
