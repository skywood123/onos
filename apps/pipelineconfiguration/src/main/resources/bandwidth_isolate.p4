#include <core.p4>
#include <v1model.p4>

#include "include/enum.p4"
#include "include/headers.p4"
#include "include/forwarding.p4"
#include "include/port_counter.p4"
#include "include/port_meter.p4"
#include "include/packet_in_out.p4"
#include "include/tenant_metering.p4"
/*
struct my_metadata_t {
//   metadata_t metadata;
}

//Use of struct
//does a packet necessarily contain all headers specified in struct ?
//Nope. It's the set of possible header appear
struct headers_t {
    Ethernet_t ethernet;
    Ipv4_t ipv4;
    Mpls_t mpls;
    packet_in_hdr packet_in;
    packet_out_hdr packet_out;

}
*/

/*******************************************************************************************
************************************** Parser *********************************************
*****************************************************************************************/
//parser configuration
parser parser_impl (packet_in packet,
                    out headers_t hdr,
                    inout my_metadata_t my_metadata,
                    inout standard_metadata_t standard_metadata) {
    state start {
        transition select(standard_metadata.ingress_port){
        CPU_PORT : parse_packet_out;
        default : parse_ethernet;
        }
    }
    //    transition select(packet.ethernet.eth_type){
    //        ethertype_ipv4: parse_ipv4;
    //        ethertype_mpls: parse_mpls;
    //    }
    state parse_packet_out{
        packet.extract(hdr.packet_out);
        transition parse_ethernet;
    }

    state parse_ethernet{
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.eth_type){
            ethertype_mpls : parse_mpls;
            ethertype_ipv4 : parse_ipv4;
            default : accept;
        }
    }


    state parse_mpls{
  //      packet.extract(hdr.metadata);
        packet.extract(hdr.mpls);
        transition parse_ipv4;
    }
    //assuming top is always ipv4
    state parse_ipv4{
        packet.extract(hdr.ipv4);
        transition accept;
    }
 //   state parse_metadata{
   //     packet.extract(hdr.metadata);
     //   transition accept;
  //  }
}

//ingress pipeline configuration

control ingress_control(inout headers_t hdr,
                        inout my_metadata_t my_metadata,
                        inout standard_metadata_t standard_metadata) {

        apply{
        port_counter_ingress_control.apply(hdr,standard_metadata);
        port_meter_ingress_control.apply(hdr,standard_metadata);
   //     packet_out_control.apply(hdr,standard_metadata);
        forwarding.apply(hdr,my_metadata,standard_metadata);
        tenant_meter_ingress_control.apply(hdr,my_metadata,standard_metadata);

        }
}
//only emit valid headers
control deparser(packet_out packet, in headers_t hdr) {
    apply {
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
   //     packet.emit(hdr.metadata);
        packet.emit(hdr.mpls);
        packet.emit(hdr.ipv4);
    }
}

control verify_checksum_control(inout headers_t hdr,
                                inout my_metadata_t my_metadata) {
    apply {
    }
}

control compute_checksum_control(inout headers_t hdr,
                                 inout my_metadata_t my_metadata) {
    apply {
    }
}



control egress_control(inout headers_t hdr,
                        inout my_metadata_t my_metadata,
                        inout standard_metadata_t standard_metadata) {
    apply {

        port_counter_egress_control.apply(hdr,standard_metadata);
        port_meter_egress_control.apply(hdr,standard_metadata);
    //    packet_in_control.apply(hdr,standard_metadata);
    }
}

V1Switch(parser_impl(),
         verify_checksum_control(),
         ingress_control(),
         egress_control(),
         compute_checksum_control(),
         deparser()
         )
          main;