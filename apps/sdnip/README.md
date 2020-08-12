# ONOS SDN-IP ROUTE-VALIDATION

## Introduction
SDN-IP ROUTE VALIDATION is a modified ONOS Application [SDN-IP](https://wiki.onosproject.org/display/ONOS/SDN-IP+Tutorial) that support RPKI BGP Route Origin Validation(ROV).
It is implemented as an optional feature to the SDN-IP application.

This application is developed in ONOS 2.1 with the use of http service from RPKI relying part software [Routinator](https://github.com/NLnetLabs/routinator).

The code that handling the http communication with Routinator is at [Rpkirov.java](https://github.com/skywood123/onos/blob/onos-2.1-sdniprpki/apps/sdnip/src/main/java/org/onosproject/sdnip/Rpkirov.java). Modification can be made here if the JSON format used with another RPKI Validator is different.
### How it works

Original SDN-IP application will install mulitpoint-to-singlepoint(MP2SP) intent for the BGP route advertised from the neighbor BGP router.

With RPKI ROV, the route advertised from neighbor is validated before installing the MP2SP intent.

SDN-IP Route Validation application will use ``` http GET ``` to validate the BGP Route received from the neighbors with the RPKI Validator.
MP2SP intent will be installed for the route if the BGP Route is valid.

***Sample response of querying a ASN and Prefix with Routinator http GET***

Response from Routinator http service for a given ASN and Prefix will be ```valid | invalid | not-found```

```
{
  "validated_route": {
    "route": {
      "origin_asn": "AS12345",
      "prefix": "192.168.0.0/24"
    },
    "validity": {
      "state": "not-found",
      "description": "No VRP Covers the Route Prefix",
      "VRPs": {
        "matched": [
        ],
        "unmatched_as": [
        ],
        "unmatched_length": [
        ]      }
    }
  }
}
```

### List of available commands for this application from ONOS CLI:
```
rpki enable
rpki disable
rpki set
rpki show
rpki validateallroutes
```

## Guide

**FROM ONOS CLI**

1. Set the RPKI Validator Ip address and port number (xxx.xxx.xxx.xxx:port)
```
rpki set 123.123.123.123:8323
```
2. Enable RPKI ROV feature.
```
rpki enable
```
3. (Optional) Check Current RPKI configuration.
```
rpki show
```
4. (Optional) Disable RPKI ROV feature.
```
rpki disable
```
5. (Optional) Force BGP Route Origin Validation for all BGP Routes.
```
rpki validateallroutes
```

