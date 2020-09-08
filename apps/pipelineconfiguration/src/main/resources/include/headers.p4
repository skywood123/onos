#ifndef _HEADERS_P4_
#define _HEADERS_P4_


header Ethernet_t{
    bit<48> dest_addr;
    bit<48> src_addr;
    bit<16> eth_type;
}

header Ipv4_t{
    bit<4> version;
    bit<4> ihl;
    bit<6> dscp;
    bit<2> ecn;
    bit<16> length;
    bit<16> id;
    bit<3> flag;
    bit<13> frag_off;
    bit<8> ttl;
    bit<8> protocol;
    bit<16> hdr_checksum;
    bit<32> ipv4_src;
    bit<32> ipv4_dst;
  //  varbit<128> options;                 //TODO check on varbit compilation and how it actually see how many bits are there for the options
}
//32 bit = 4 bytes
header Mpls_t{
    bit<20> label;
    bit<3> exp;
    bit<1> s;
    bit<8> ttl;
}

//header metadata_t{
//    bit<8> packet_colour;
//    bit<8> tenant_id;
//}


@controller_header("packet_in")
header packet_in_hdr{
    bit<16> ingress_port;
}

@controller_header("packet_out")
header packet_out_hdr{
    bit<16> egress_port;
}

#ifndef _HEADERS_T_
#define _HEADERS_T_
struct headers_t {
    Ethernet_t ethernet;
    Ipv4_t ipv4;
    Mpls_t mpls;
    packet_in_hdr packet_in;
    packet_out_hdr packet_out;
//    metadata_t metadata;

}
#endif

#ifndef _MY_METADATA_T_
#define _MY_METADATA_T_
struct my_metadata_t {
   //metadata_t metadata;
   bit<8> packet_colour;
}

#endif

#endif