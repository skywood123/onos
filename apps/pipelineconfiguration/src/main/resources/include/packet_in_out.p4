#ifndef _PACKET_IN_OUT_P4_
#define _PACKET_IN_OUT_P4_

/*
control packet_in_control(inout headers_t hdr,
                      inout standard_metadata_t standard_metadata){


    apply{
        if(standard_metadata.egress_spec==CPU_PORT){
                hdr.packet_in.setValid();
                hdr.packet_in.ingress_port=(bit<16>)standard_metadata.ingress_port;   //this packet-in is triggered from
        }                                                         //this physical standard_metadata.ingress_port
    }
}
*/

control packet_out_control(inout headers_t hdr,
                            inout standard_metadata_t standard_metadata){

        apply{

        }

}


#endif