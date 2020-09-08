#ifndef _TENANT_METERING_P4_
#define _TENANT_METERING_P4_

     // when a packet hit the tenant's flow, update the meter for it
    // do at rx
    //input port from host
    //output port to switch
#include "enum.p4"
#include "headers.p4"
//after mpls tagging
//in onos app, insert table entry of matching the mpls label and output port

control tenant_meter_ingress_control(inout headers_t hdr,
                                     inout my_metadata_t my_metadata,
                                      inout standard_metadata_t standard_metadata){
    meter(MAX_PORT,MeterType.bytes) tenant_port_meter;
    // FIXME should be using indirect meter
    // Each flow for a tenant using different label
    // A tenant holding multiple MPLS labels
    // A indirect meter should be using : matching traffic from a tenant(from multiple mpls label or in port) going out an egress port


   // action tag_the_packet(MeterColour colour){
   //     my_metadata.packet_colour=colour;
   // }

    action _drop(){
       mark_to_drop(standard_metadata);
       }

       //now i send in tenant number, match against specific meter here
       //tag as well
    action read_meter_and_tag(bit<8> tenant_outport){
       // hdr.metadata.setValid();
        //now tenant_outport = tenant_id
      //  hdr.metadata.tenant_id=tenant_id;
        tenant_port_meter.execute_meter((bit<32>)tenant_outport,my_metadata.packet_colour);
    }

    table tenant_uplink_meter_table{
        key = {
         //   standard_metadata.ingress_port : ternary;
            standard_metadata.egress_spec : exact;
            hdr.mpls.label : exact;
        }
        actions = {
            read_meter_and_tag;
            NoAction;
        }
        default_action= NoAction;
    }

    table tenant_uplink_meter_filtering_table{

        key= {
            my_metadata.packet_colour  : exact;
        }
        actions = {
        _drop();
        NoAction;
        }

    }
    apply{

    if(tenant_uplink_meter_table.apply().hit)
        tenant_uplink_meter_filtering_table.apply();



        }
}
#endif