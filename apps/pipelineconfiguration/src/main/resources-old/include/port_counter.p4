#ifndef _PORT_COUNTER_P4_
#define _PORT_COUNTER_P4_

//#include "../bandwidth_isolate.p4"
#include "headers.p4"
//specifying a array of length MAX_PORT
//indexing by the port number
control port_counter_ingress_control(inout headers_t hdr,
                                     inout standard_metadata_t standard_metadata){
    counter(MAX_PORT,CounterType.packets_and_bytes) ingress_port_counter;

//FIXME might need casting here

    apply{
        ingress_port_counter.count((bit<32>)standard_metadata.ingress_port);
    }
}

control port_counter_egress_control(inout headers_t hdr,
                                    inout standard_metadata_t standard_metadata){

    counter(MAX_PORT,CounterType.packets_and_bytes) egress_port_counter;

//FIXME might need casting here

    apply{
        egress_port_counter.count((bit<32>)standard_metadata.egress_port);
    }
}













#endif