# IP Backbone and MPLS VPN tools

This repo contains approaches to different aspects of the data acquisition of an
IP/MPLS backbone network. 

**Note this code may be far from usable. You have been warned.**

## Main ideas that caused this project to start

Router configuration parsing
 - Cisco IOS/Juniper IOS
 - Extract vrf names, RDs, RTs, route-maps, interfaces / routing-instances, rd, rt, policy-statements, interfaces

Topology discovery
 - Identification of common topologies: full-mesh, hub-and-spoke
 - Apply logic to topologies that diverge from the common ones: route leaking among VRFs

Graphical representation of topologies
 - Interactive view of backbone topologies and VPNs

## To consider for future versions

 - Descend to the CPEs where mult-VRF is configured
 - Descend to the MPLS-to-the-edge CPEs?


