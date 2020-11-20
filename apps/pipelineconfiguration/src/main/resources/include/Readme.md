Reference link
https://github.com/p4lang/p4-applications/blob/master/telemetry/specs/INT.mdk

1. We describe four encapsulation formats in this specification, covering
   different deployment scenarios, with and without network virtualization:
   
   *INT over TCP/UDP* - A shim header is inserted following TCP/UDP
   header. INT Headers are carried between this shim header and TCP/UDP payload.
   Since v2.0, the spec also supports an option to insert a new UDP header
   (followed by INT headers) before the existing L4 header.
   This approach doesn’t rely on any tunneling/virtualization mechanism and is
   versatile to apply INT to both native and virtualized traffic.
2. ### INT over TCP/UDP
   
   In case the traffic being monitored is not encapsulated by any virtualization
   header, one can also put the INT metadata just after layer 4 headers (TCP/UDP).
   The scheme assumes that the non-INT devices between the INT source and the
   INT sink either do not parse beyond layer-4 headers or can skip through the
   INT stack using the Length field in the INT shim header. If TCP has any options,
   the INT stack may come before or after the TCP options but the decision must
   be consistent within an INT domain.
   
   Note that INT over UDP can be used even when the packet is encapsulated by VXLAN,
   Geneve, or GUE (Generic UDP Encapsulation). INT over TCP/UDP also makes it
   easier to add INT stack into outer, inner, or even both layers. In such cases
   both INT header stacks carry information for respective layers and need not be
   considered interfering with each other.
   
   A field in Ethernet, IP, or TCP/UDP should indicate if the
   INT header exists after the TCP/UDP header.
    
IPv4 DSCP or IPv6 Traffic Class field: A value or a bit can be used to
indicate the existence of INT after TCP/UDP. When the INT source inserts the
INT header into a packet, it sets the reserved value in the field or sets the
bit. The INT source may write the original DSCP value in the INT headers so
that the INT sink can restore the original value. Restoring the original value
is optional.
  - Allocating a bit, as opposed to a value codepoint, will allow the rest of
    DSCP field to be used for QoS, hence allowing the coexistence of DSCP-based
    QoS and INT. If the traffic being monitored is subjected to QoS services
    such as rate limiting, shaping, or differentiated queueing based on DSCP
    field, QoS classification in the network must be programmed to
    ignore the designated bit position to ensure that the INT-enabled traffic
    receives the same treatment as the original traffic being monitored.
  - In brownfield scenarios, however, the network operator may not find a bit
    available to allocate for INT but may still have a fragmented space of 32
    unused DSCP values. The operator can allocate an INT-enabled DSCP value
    for every QoS DSCP value, map the INT-enabled DSCP value to the same
    QoS behavior as the corresponding QoS DSCP value. This may double the
    number of QoS rules but will allow the co-existence of DSCP-based QoS and
    INT even when a single DSCP bit is not available for INT.
  - Within an INT domain, DSCP values used for INT must exclusively be used
    for INT. INT transit and sink nodes must not receive non-INT packets
    marked with DSCP values used for INT. Any time a node forwards a packet
    into the INT domain and there is no INT header present, it must ensure that
    the DSCP/Traffic class value is not the same as any of the values used
    to indicate INT.