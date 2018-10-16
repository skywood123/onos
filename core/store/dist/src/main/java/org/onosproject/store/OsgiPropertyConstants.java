/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.store;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    //@Property(name = "msgHandlerPoolSize", intValue = MESSAGE_HANDLER_THREAD_POOL_SIZE,
    //    label = "Number of threads in the message handler pool")
    public static final String MESSAGE_HANDLER_THREAD_POOL_SIZE = "msgHandlerPoolSize:Integer";
    public static final int MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 8;

    //@Property(name = "backupPeriod", intValue = BACKUP_PERIOD_MILLIS,
    //    label = "Delay in ms between successive backup runs")
    public static final String BACKUP_PERIOD_MILLIS = "backupPeriod:Integer";
    public static final int BACKUP_PERIOD_MILLIS_DEFAULT = 2000;

    //@Property(name = "antiEntropyPeriod", intValue = ANTI_ENTROPY_PERIOD_MILLIS,
    //    label = "Delay in ms between anti-entropy runs")
    public static final String ANTI_ENTROPY_PERIOD_MILLIS = "antiEntropyPeriod:Integer";
    public static final int ANTI_ENTROPY_PERIOD_MILLIS_DEFAULT = 5000;

    //@Property(name = "persistenceEnabled", boolValue = false,
    //    label = "Indicates whether or not changes in the flow table should be persisted to disk.")
    public static final String EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED = "ECFlowRuleStorePersistenceEnabled:Boolean";
    public static final boolean EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED_DEFAULT = false;

    //@Property(name = "backupCount", intValue = DEFAULT_MAX_BACKUP_COUNT,
    //    label = "Max number of backup copies for each device")
    public static final String MAX_BACKUP_COUNT = "backupCount:Integer";
    public static final int MAX_BACKUP_COUNT_DEFAULT = 2;

    //@Property(name = "heartbeatInterval", intValue = DEFAULT_HEARTBEAT_INTERVAL,
    //        label = "Interval time to send heartbeat to other controller nodes (millisecond)")
    public static final String HEARTBEAT_INTERVAL = "heartbeatInterval:Integer";
    public static final int HEARTBEAT_INTERVAL_DEFAULT = 100;

    //@Property(name = "phiFailureThreshold", intValue = DEFAULT_PHI_FAILURE_THRESHOLD,
    //        label = "the value of Phi threshold to detect accrual failure")
    public static final String PHI_FAILURE_THRESHOLD = "phiFailureThreshold:Integer";
    public static final int PHI_FAILURE_THRESHOLD_DEFAULT = 10;

    //@Property(name = "minStandardDeviationMillis", longValue = DEFAULT_MIN_STANDARD_DEVIATION_MILLIS,
    //    label = "The minimum standard deviation to take into account when computing the Phi value")
    public static final String MIN_STANDARD_DEVIATION_MILLIS = "minStandardDeviationMillis:Long";
    public static final long MIN_STANDARD_DEVIATION_MILLIS_DEFAULT = 50;

    //@Property(name = "electionTimeoutMillis", longValue = DEFAULT_ELECTION_TIMEOUT_MILLIS,
    //        label = "the leader election timeout in milliseconds")
    public static final String ELECTION_TIMEOUT_MILLIS = "electionTimeoutMillis:Long";
    public static final long ELECTION_TIMEOUT_MILLIS_DEFAULT = 2500;

    //@Property(name = "garbageCollect", boolValue = GARBAGE_COLLECT,
    //        label = "Enable group garbage collection")
    public static final String GARBAGE_COLLECT = "garbageCollect:Boolean";
    public static final boolean GARBAGE_COLLECT_DEFAULT = false;

    //@Property(name = "gcThresh", intValue = GC_THRESH,
    //        label = "Number of rounds for group garbage collection")
    public static final String GARBAGE_COLLECT_THRESH = "gcThresh:Integer";
    public static final int GARBAGE_COLLECT_THRESH_DEFAULT = 6;

    //@Property(name = "allowExtraneousGroups", boolValue = ALLOW_EXTRANEOUS_GROUPS,
    //        label = "Allow groups in switches not installed by ONOS")
    public static final String ALLOW_EXTRANEOUS_GROUPS = "garbageCollect:Boolean";
    public static final boolean ALLOW_EXTRANEOUS_GROUPS_DEFAULT = false;

    //@Property(name = "persistenceEnabled", boolValue = PERSIST,
    //        label = "EXPERIMENTAL: Enable intent persistence")
    public static final String GIS_PERSISTENCE_ENABLED = "GISPersistenceEnabled:Boolean";
    public static final boolean GIS_PERSISTENCE_ENABLED_DEFAULT = false;

    //@Property(name = "messageHandlerThreadPoolSize", intValue = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE,
    //        label = "Size of thread pool to assign message handler")
    public static final String DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE = "DPSMessageHandlerThreadPoolSize:Integer";
    public static final int DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 4;

    //@Property(name = "messageHandlerThreadPoolSize", intValue = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE,
    //        label = "Size of thread pool to assign message handler")
    public static final String DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE = "DFSMessageHandlerThreadPoolSize:Integer";
    public static final int DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 4;

    //@Property(name = "messageHandlerThreadPoolSize", intValue = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE,
    //        label = "Size of thread pool to assign message handler")
    public static final String DSS_MESSAGE_HANDLER_THREAD_POOL_SIZE = "DFSMessageHandlerThreadPoolSize:Integer";
    public static final int DSS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 4;

    //@Property(name = "linkWeightFunction", value = DEFAULT_LINK_WEIGHT_FUNCTION,
    //        label = "Default link-weight function: hopCount, linkMetric, geoDistance")
    public static final String LINK_WEIGHT_FUNCTION = "linkWeightFunction";
    public static final String LINK_WEIGHT_FUNCTION_DEFAULT = "hopCount";
}
