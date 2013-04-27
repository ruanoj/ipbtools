I wanted to write some code to automatically discovering the topology of an
IP/MPLS backbone network to which there is SNMP access.

Using IGP to retrieve the adjacencies would have been the easiest way, but the
network I am dealing with has no support for the ISIS-specific SNMP MIBs, so I
had to look for alternatives.

Luckily, the backbone has LDP for MPLS label signalling.

Further details (interface names) can be inferred from routes to neighbours and
ARP entries to next hops.  


Basic adjacency: Identifies adjacent nodes

1) Given an initial node, obtain active LDP connections. If a connection is
performed on port 646, the peer is adjacent; add it to list of nodes to query.

2) Query any newly discovered nodes

Output:

Origin - Destination(s) table

