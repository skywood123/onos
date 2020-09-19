package org.xzk.network_slicing.models;

import org.onlab.packet.MplsLabel;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;

public class FlowRuleInformation {

    private FlowRule flowRule;
    private MplsLabel mplsLabel; // current flow tagged label, not the next label
    private ConnectPoint sourceConnectPoint;
    private ConnectPoint destinationConnectPoint;

    public FlowRuleInformation(FlowRule flowRule, MplsLabel mplsLabel, DeviceId deviceId, PortNumber sourcePort, PortNumber destPort) {
        this.flowRule = flowRule;
        this.mplsLabel = mplsLabel;
        this.sourceConnectPoint = new ConnectPoint(deviceId, sourcePort);
        this.destinationConnectPoint = new ConnectPoint(deviceId,destPort);
    }

    public DeviceId getFlowRuleDeviceId() {
        return this.flowRule.deviceId();
    }

    public FlowRule getFlowRule() {
        return this.flowRule;
    }

    public MplsLabel getMplsLabel() {
        return this.mplsLabel;
    }

    public boolean compareSourceCP(ConnectPoint connectPoint){
        if(sourceConnectPoint.equals(connectPoint)) {
            return true;
        }
        return false;
    }
    public boolean compareDestinationCP(ConnectPoint connectPoint){
        if(destinationConnectPoint.equals(connectPoint)) {
            return true;
        }
        return false;
    }

    public long getOutPort(){
        return destinationConnectPoint.port().toLong();
    }
}
