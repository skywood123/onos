#ifndef _TENANT_METERING_P4_
#define _TENANT_METERING_P4_

     // when a packet hit the tenant's flow, update the meter for it
    // do at rx
    //input port from host
    //output port to switch

#include "headers.p4"
//after mpls tagging
//in onos app, insert table entry of matching the mpls label and output port

control tenant_meter_ingress_control(inout headers_t hdr,
                                     inout local_metadata_t local_metadata,
                                      inout standard_metadata_t standard_metadata){
    meter(MAX_PORTS,MeterType.bytes) tenant_port_meter;
    // FIXME should be using indirect meter
    // Each flow for a tenant using different label
    // A tenant holding multiple MPLS labels
    // A indirect meter should be using : matching traffic from a tenant(from multiple mpls label or in port) going out an egress port


    //FIXME
    //a Counter Cell index cannot be cleared off
    counter(MAX_PORTS,CounterType.packets_and_bytes) virtual_network_counters;


   // action tag_the_packet(MeterColour colour){
   //     my_metadata.packet_colour=colour;
   // }


   action virtual_net_count(bit<8> tenant_outport){
        virtual_network_counters.count((bit<32>) tenant_outport);

   }

    action _drop(){
       mark_to_drop(standard_metadata);
       }

       //now i send in tenant number, match against specific meter here
       //tag as well
    action read_meter_and_tag(bit<8> tenant_outport){
       // hdr.metadata.setValid();
        //now tenant_outport = tenant_id
      //  hdr.metadata.tenant_id=tenant_id;
        tenant_port_meter.execute_meter((bit<32>)tenant_outport,local_metadata.packet_colour);
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
            local_metadata.packet_colour  : exact;
        }
        actions = {
        _drop();
        NoAction;
        }

    }

    table virtual_network_counter_table{
        key = {
         //   standard_metadata.ingress_port : ternary;
            standard_metadata.egress_spec : exact;
            hdr.mpls.label : exact;
        }
        actions = {
            virtual_net_count;
            NoAction;
        }
        default_action= NoAction;



    }



    apply{

    if(tenant_uplink_meter_table.apply().hit){
        tenant_uplink_meter_filtering_table.apply();
        virtual_network_counter_table.apply();
    }

        }
}
#endif