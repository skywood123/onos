#ifndef _ENUM_P4_
#define _ENUM_P4_


#define MAX_PORT 256

const bit<16> ethertype_ipv4=   0x0800;
const bit<16> ethertype_mpls=   0x8847;
const bit<16> ethertype_lldp=   0x08CC;
const bit<16> ethertype_arp =   0x0806;

typedef bit<9> Port_t;
const Port_t CPU_PORT=255;

typedef bit<2> MeterColour;
const MeterColour MeterColour_GREEN = 0;
const MeterColour MeterColour_YELLOW = 1;
const MeterColour MeterColour_RED= 2;

























#endif