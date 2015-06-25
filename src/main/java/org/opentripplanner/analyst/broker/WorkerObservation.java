package org.opentripplanner.analyst.broker;

/**
 *
 */
public class WorkerObservation {

    public final String workerId;
    public final String graphAffinity;
    public final long lastSeen;

    public WorkerObservation (String workerId, String graphAffinity) {
        this.workerId = workerId;
        this.graphAffinity = graphAffinity;
        this.lastSeen = System.currentTimeMillis();
    }

}
