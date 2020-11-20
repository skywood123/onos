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

/* -*- P4_16 -*- */
#ifndef __INT_PARSER__
#define __INT_PARSER__

parser int_parser (
    packet_in packet,
    out headers_t hdr,
    inout local_metadata_t local_metadata,
  //  inout my_metadata_t my_metadata,
    inout standard_metadata_t standard_metadata) {

    state start {
        transition select(standard_metadata.ingress_port) {
            CPU_PORT: parse_packet_out;
            default: parse_ethernet;
        }
    }

    state parse_packet_out {
        packet.extract(hdr.packet_out);
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.ether_type) {
            ethertype_mpls : parse_mpls;
            ETH_TYPE_IPV4 : parse_ipv4;
            default : accept;
        }
    }
    state parse_mpls{
  //      packet.extract(hdr.metadata);
        packet.extract(hdr.mpls);
        transition parse_ipv4;
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition select(hdr.ipv4.protocol) {
            IP_PROTO_TCP : parse_tcp;
            IP_PROTO_UDP : parse_udp;
            default: accept;
        }
    }
//if DSCP value indicating there is INT header embedded in the TCP payload, parse the INT headers
    state parse_tcp {
        packet.extract(hdr.tcp);
        local_metadata.l4_src_port = hdr.tcp.src_port;
        local_metadata.l4_dst_port = hdr.tcp.dst_port;
        transition select(hdr.ipv4.dscp) {
            DSCP_INT &&& DSCP_MASK: parse_intl4_shim;        // &&& this is mask
                                                            //given  8w0x0A &&& 8w0x0F ; this 8 bit value pattern matching will be xxxx1010 ; the mask is xxxx1111
            default: accept;
        }
    }

    state parse_udp {
        packet.extract(hdr.udp);
        local_metadata.l4_src_port = hdr.udp.src_port;
        local_metadata.l4_dst_port = hdr.udp.dst_port;
        transition select(hdr.ipv4.dscp) {
            DSCP_INT &&& DSCP_MASK: parse_intl4_shim;
            default: accept;
        }
    }

//A shim header is inserted following TCP/UDP
 // header. INT Headers are carried between this shim header and TCP/UDP payload.
    state parse_intl4_shim {
        packet.extract(hdr.intl4_shim);
        local_metadata.int_meta.intl4_shim_len = hdr.intl4_shim.len;
        transition parse_int_header;
    }

    state parse_int_header {
        packet.extract(hdr.int_header);
        transition parse_int_data;
    }
/*
void extract<T>(out T headerLvalue, in bit<32> variableFieldSize);

The expression headerLvalue must be a l-value representing a header that contains exactly one varbit
  field. The expression variableFieldSize must evaluate to a bit<32> value that indicates the number of
  bits to be extracted into the unique varbit field of the header (i.e., this size is not the size of the complete
  header, just the varbit field).*/
    state parse_int_data {
        // Parse INT metadata stack
        packet.extract(hdr.int_data, ((bit<32>) (local_metadata.int_meta.intl4_shim_len - INT_HEADER_LEN_WORD)) << 5); //this is calculating the actual bit width of the data; <<5 is converting bytes to bits
        transition accept;
    }
}

control int_deparser(
    packet_out packet,
    in headers_t hdr) {
    apply {
        packet.emit(hdr.packet_in);
        packet.emit(hdr.report_ethernet);
        packet.emit(hdr.report_ipv4);
        packet.emit(hdr.report_udp);
        packet.emit(hdr.report_fixed_header);
        packet.emit(hdr.ethernet);
        packet.emit(hdr.mpls);
        packet.emit(hdr.ipv4);
        packet.emit(hdr.tcp);
        packet.emit(hdr.udp);
        packet.emit(hdr.intl4_shim);
        packet.emit(hdr.int_header);
        packet.emit(hdr.int_switch_id);
        packet.emit(hdr.int_level1_port_ids);
        packet.emit(hdr.int_hop_latency);
        packet.emit(hdr.int_q_occupancy);
        packet.emit(hdr.int_ingress_tstamp);
        packet.emit(hdr.int_egress_tstamp);
        packet.emit(hdr.int_level2_port_ids);
        packet.emit(hdr.int_egress_tx_util);
        packet.emit(hdr.int_data);
    }
}

#endif
