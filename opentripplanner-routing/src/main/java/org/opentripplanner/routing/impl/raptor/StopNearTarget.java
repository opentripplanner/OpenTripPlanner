package org.opentripplanner.routing.impl.raptor;

public class StopNearTarget {

    RaptorStop stop;
    double walkDistance;
    int time;

    public StopNearTarget(RaptorStop stop, double walkDistance, int time) {
        this.stop = stop;
        this.walkDistance = walkDistance;
        this.time = time;
    }

}
