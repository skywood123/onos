package org.onosproject.meterconfiguration;

/**
 * execute every second to query the counter from the dataplane to check the utilization of each network at each device's port
 * use scheduledexecutor service
 * read and check the difference with previous value and output current utilization value
 * UNABLE to clear off counter and reusing same counter index causing the counter value inaccurate; not outputing the counter value
 */
public class VirtualNetworkUtilization {
    

}
