#ifndef _FORWARDING_P4_
#define _FORWARDING_P4_

#include "enum.p4"
//#include "../bandwidth_isolate.p4"
#include "headers.p4"


//TODO add in MPLS tenant forwarding

control forwarding(inout headers_t hdr,
                        inout my_metadata_t my_metadata,
                        inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) flow_counter;
    //TODO direct_meter<MeterColour>(MeterType.bytes) flow_meter;
    //each device has its relative mpls label for each flow
    // a meter cell index = tenant(mpls labels) Z with uplink(sw-sw) port
 //   meter (MAX_PORT, MeterType.bytes) tenantsSharedResourceMeter;

    action set_out_port(Port_t port){
        standard_metadata.egress_spec=port;
    }
    action _drop(){
    mark_to_drop(standard_metadata);
    }
    action send_to_cpu(){
                    standard_metadata.egress_spec=CPU_PORT;
                    hdr.packet_in.setValid();
                    hdr.packet_in.ingress_port=(bit<16>)standard_metadata.ingress_port;   //this packet-in is triggered from
            }


    action mpls_push(bit<20> mpls_label_id,Port_t port){
        hdr.mpls.setValid();
        hdr.mpls.label=mpls_label_id;
        hdr.mpls.exp=0;
        hdr.mpls.s=1;
        hdr.mpls.ttl=255;
        hdr.ethernet.eth_type=ethertype_mpls;
        set_out_port(port);
    }

    //FIXME when 3 device used ( middle only swap the label and specify output port)
    // now in pipeconf if 2 instructions come will directly use mpls_pop and specify output port
    //TODO should add another action to swap the label only and specify outputport
    action mpls_pop(Port_t port){
        hdr.mpls.setInvalid();
        hdr.ethernet.eth_type=ethertype_ipv4;
        set_out_port(port);
   //     hdr.metadata.setInvalid();
    }

    action mpls_swap(bit<20> mpls_label_id,Port_t port){
        hdr.mpls.label=mpls_label_id;
        set_out_port(port);
        hdr.mpls.ttl=hdr.mpls.ttl-1;
    }

 //   action mark_the_packet(bit<8> tenant_outport){
  //      tenantsSharedResourceMeter.execute_meter(tenant_outport,my_metadata.packet_colour);
  //  }

    //possibly need to split the table, each entry only support one action
    //to push/pop mpls label, should use another table
    //TODO
   /** table mpls_processing_table{
        key={
            hdr.ipv4.ipv4_dest  :lpm;
            hdr.mpls.label      :exact;
            standard_metadata.ingress_port  : ternary;
        }

        actions= {
        }


    }//1 action with 2 parameters in P4
    */
    table l2_fwd{
        key ={
            standard_metadata.ingress_port  : ternary;
            hdr.ethernet.dest_addr          : ternary;
            hdr.ethernet.src_addr           : ternary;
            hdr.ethernet.eth_type           : ternary;
            hdr.mpls.label                  : ternary;
            hdr.mpls.s                      : ternary;
            hdr.ipv4.ipv4_dst               : lpm;
        }
        //TODO update the action
        actions={
            set_out_port;
            send_to_cpu;
            mpls_push;
            mpls_pop;
            mpls_swap;
            _drop;
            NoAction;
       //     mark_the_packet;
        }
        default_action=NoAction;
        counters = flow_counter; //initialize the constructor
     //TODO   meters = flow_meter;
    }

        apply {
             if(standard_metadata.ingress_port==CPU_PORT){
                            standard_metadata.egress_spec=(bit<9>)hdr.packet_out.egress_port; //specify that only using 9 bit from the packet out egress port field
                            hdr.packet_out.setInvalid();

                    }
             else{
                l2_fwd.apply();


             }
        }

}





#endif