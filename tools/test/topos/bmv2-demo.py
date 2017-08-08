#!/usr/bin/python

import os
import sys
import json
import argparse

TEMP_NETCFG_FILE = '/tmp/bmv2-demo-cfg.json'
BASE_LONGITUDE = -115
SWITCH_BASE_LATITUDE = 25
HOST_BASE_LATITUDE = 28
BASE_SHIFT = 8
VLAN_NONE = -1
DEFAULT_SW_BW = 50
DEFAULT_HOST_BW = 25

if 'ONOS_ROOT' not in os.environ:
    print "Environment var $ONOS_ROOT not set"
    exit()
else:
    ONOS_ROOT = os.environ["ONOS_ROOT"]
    sys.path.append(ONOS_ROOT + "/tools/dev/mininet")
if 'RUN_PACK_PATH' not in os.environ:
    print "Environment var $RUN_PACK_PATH not set"
    exit()
else:
    RUN_PACK_PATH = os.environ["RUN_PACK_PATH"]

from onos import ONOSCluster, ONOSCLI
from bmv2 import ONOSBmv2Switch

from itertools import combinations
from time import sleep
from subprocess import call

from mininet.cli import CLI
from mininet.link import TCLink
from mininet.log import setLogLevel
from mininet.net import Mininet
from mininet.node import RemoteController, Host
from mininet.topo import Topo

class ClosTopo(Topo):
    "2 stage Clos topology"

    def __init__(self, pipeconfId="", **opts):
        # Initialize topology and default options
        Topo.__init__(self, **opts)

        bmv2SwitchIds = ["s11", "s12", "s13", "s21", "s22", "s23"]
        bmv2Switches = {}

        for switchId in bmv2SwitchIds:
            deviceId=int(switchId[1:])
            # Use first number in device id to calculate latitude (row number)
            latitude = SWITCH_BASE_LATITUDE + (deviceId // 10) * BASE_SHIFT

            # Use second number in device id to calculate longitude (column number)
            longitude = BASE_LONGITUDE + (deviceId % 10) * BASE_SHIFT
            bmv2Switches[switchId] = self.addSwitch(switchId,
                                                    cls=ONOSBmv2Switch,
                                                    loglevel="warn",
                                                    deviceId=deviceId,
                                                    netcfg=False,
                                                    longitude=longitude,
                                                    latitude=latitude,
                                                    pipeconfId=pipeconfId)

        for i in (1, 2, 3):
            for j in (1, 2, 3):
                if i == j:
                    # 2 links
                    self.addLink(bmv2Switches["s1%d" % i], bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=DEFAULT_SW_BW)
                    self.addLink(bmv2Switches["s1%d" % i], bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=DEFAULT_SW_BW)
                else:
                    self.addLink(bmv2Switches["s1%d" % i], bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=DEFAULT_SW_BW)

        for hostId in (1, 2, 3):
            host = self.addHost("h%d" % hostId,
                                cls=DemoHost,
                                ip="10.0.0.%d/24" % hostId,
                                mac='00:00:00:00:00:%02x' % hostId)
            self.addLink(host, bmv2Switches["s1%d" % hostId], cls=TCLink, bw=DEFAULT_HOST_BW)


class DemoHost(Host):
    "Demo host"

    def __init__(self, name, inNamespace=True, **params):
        Host.__init__(self, name, inNamespace=inNamespace, **params)
        self.exectoken = "/tmp/mn-exec-token-host-%s" % name
        self.cmd("touch %s" % self.exectoken)

    def config(self, **params):
        r = super(Host, self).config(**params)

        for off in ["rx", "tx", "sg"]:
            cmd = "/sbin/ethtool --offload %s %s off" % (self.defaultIntf(), off)
            self.cmd(cmd)

        # disable IPv6
        self.cmd("sysctl -w net.ipv6.conf.all.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.default.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.lo.disable_ipv6=1")

        return r

    def startPingBg(self, h):
        self.cmd(self.getInfiniteCmdBg("ping -i0.5 %s" % h.IP()))
        self.cmd(self.getInfiniteCmdBg("arping -w5000000 %s" % h.IP()))

    def startIperfServer(self):
        self.cmd(self.getInfiniteCmdBg("iperf3 -s"))

    def startIperfClient(self, h, flowBw="512k", numFlows=5, duration=5):
        iperfCmd = "iperf3 -c{} -b{} -P{} -t{}".format(h.IP(), flowBw, numFlows, duration)
        self.cmd(self.getInfiniteCmdBg(iperfCmd, sleep=0))

    def stop(self):
        self.cmd("killall iperf3")
        self.cmd("killall ping")
        self.cmd("killall arping")

    def describe(self):
        print "**********"
        print self.name
        print "default interface: %s\t%s\t%s" % (
            self.defaultIntf().name,
            self.defaultIntf().IP(),
            self.defaultIntf().MAC()
        )
        print "**********"

    def getInfiniteCmdBg(self, cmd, logfile="/dev/null", sleep=1):
        return "(while [ -e {} ]; " \
               "do {}; " \
               "sleep {}; " \
               "done;) > {} 2>&1 &".format(self.exectoken, cmd, sleep, logfile)

    def getCmdBg(self, cmd, logfile="/dev/null"):
        return "{} > {} 2>&1 &".format(cmd, logfile)

def generateNetcfg(onosIp, net):
    netcfg = { 'devices': {}, 'links': {}, 'hosts': {}}
    # Device configs
    for sw in net.switches:
        srcIp = sw.getSourceIp(onosIp)
        netcfg['devices'][sw.onosDeviceId] = sw.getDeviceConfig(srcIp)

    hostLocations = {}
    # Link configs
    for link in net.links:
        switchPort = link.intf1.name.split('-')
        sw1Name = switchPort[0] # s11
        port1Name = switchPort[1] # eth0
        port1 = port1Name[3:]
        switchPort = link.intf2.name.split('-')
        sw2Name = switchPort[0]
        port2Name = switchPort[1]
        port2 = port2Name[3:]
        sw1 = net[sw1Name]
        sw2 = net[sw2Name]
        if isinstance(sw1, Host):
            # record host location and ignore it
            # e.g. {'h1': 'device:bmv2:11'}
            hostLocations[sw1.name] = '%s/%s' % (sw2.onosDeviceId, port2)
            continue

        if isinstance(sw2, Host):
            # record host location and ignore it
            # e.g. {'h1': 'device:bmv2:11'}
            hostLocations[sw2.name] = '%s/%s' % (sw1.onosDeviceId, port1)
            continue

        linkId = '%s/%s-%s/%s' % (sw1.onosDeviceId, port1, sw2.onosDeviceId, port2)
        netcfg['links'][linkId] = {
            'basic': {
                'type': 'DIRECT',
                'bandwidth': 50
            }
        }

    # Host configs
    longitude = BASE_LONGITUDE
    for host in net.hosts:
        longitude = longitude + BASE_SHIFT
        hostDefaultIntf = host.defaultIntf()
        hostMac = host.MAC(hostDefaultIntf)
        hostIp = host.IP(hostDefaultIntf)
        hostId = '%s/%d' % (hostMac, VLAN_NONE)
        location = hostLocations[host.name]

        # use host Id to generate host location
        hostConfig = {
            'basic': {
                'locations': [location],
                'ips': [hostIp],
                'name': host.name,
                'latitude': HOST_BASE_LATITUDE,
                'longitude': longitude
            }
        }
        netcfg['hosts'][hostId] = hostConfig

    print "Writing network config to %s" % TEMP_NETCFG_FILE
    with open(TEMP_NETCFG_FILE, 'w') as tempFile:
        json.dump(netcfg, tempFile)

def main(args):
    setLogLevel('debug')
    if not args.onos_ip:
        controller = ONOSCluster('c0', 3)
        onosIp = controller.nodes()[0].IP()
    else:
        controller = RemoteController('c0', ip=args.onos_ip)
        onosIp = args.onos_ip

    topo = ClosTopo(pipeconfId=args.pipeconf_id)

    net = Mininet(topo=topo, build=False, controller=[controller])

    net.build()
    net.start()

    print "Network started"

    # Generate background traffic.
    sleep(3)
    for (h1, h2) in combinations(net.hosts, 2):
        h1.startPingBg(h2)
        h2.startPingBg(h1)

    print "Background ping started"

    for h in net.hosts:
        h.startIperfServer()

    print "Iperf servers started"

    # sleep(4)
    # print "Starting traffic from h1 to h3..."
    # net.hosts[0].startIperfClient(net.hosts[-1], flowBw="200k", numFlows=100, duration=10)

    generateNetcfg(onosIp, net)

    print "Uploading netcfg..."
    call(("%s/onos-netcfg" % RUN_PACK_PATH, onosIp, TEMP_NETCFG_FILE))

    if not args.onos_ip:
        ONOSCLI(net)
    else:
        CLI(net)

    net.stop()
    call(("rm", "-f", TEMP_NETCFG_FILE))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='BMv2 mininet demo script (2-stage Clos topology)')
    parser.add_argument('--onos-ip', help='ONOS-BMv2 controller IP address',
                        type=str, action="store", required=False)
    parser.add_argument('--pipeconf-id', help='Pipeconf ID for switches',
                        type=str, action="store", required=False, default='')
    args = parser.parse_args()
    setLogLevel('info')
    main(args)
