#ifndef _PORT_METER_P4_
#define _PORT_METER_P4_

//#include "../bandwidth_isolate.p4"
#include "headers.p4"
//to have the statistic counting only
//This is for the port rx
//FIXME might need casting on port
control port_meter_ingress_control(inout headers_t hdr,
                                   inout standard_metadata_t standard_metadata){
        meter (MAX_PORT, MeterType.bytes) ingress_port_meter;
        MeterColour ingress_colour=MeterColour_GREEN;
        apply{
            ingress_port_meter.execute_meter<MeterColour>((bit<32>)standard_metadata.ingress_port,ingress_colour);
        }//If ingress_colour get changed after consulting the meter, then make sense
        //we can drop the packet here


}

//This is for the port tx
control port_meter_egress_control(inout headers_t hdr,
                                  inout standard_metadata_t standard_metadata){
        meter(MAX_PORT,MeterType.bytes) egress_port_meter;
        MeterColour egress_colour=MeterColour_GREEN;

        apply{
            egress_port_meter.execute_meter<MeterColour>((bit<32>)standard_metadata.egress_port,egress_colour);
        }



}






















#endif