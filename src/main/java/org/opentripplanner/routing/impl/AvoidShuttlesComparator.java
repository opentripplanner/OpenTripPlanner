package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.spt.GraphPath;

public class AvoidShuttlesComparator extends PathComparator {
    public AvoidShuttlesComparator(boolean compareStartTimes) {
        super(compareStartTimes);
    }

    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        boolean o1HasShuttles = hasShuttles(o1);
        boolean o2HasShuttles = hasShuttles(o2);

        if (o1HasShuttles == o2HasShuttles) return super.compare(o1, o2);
        // means that if o1 has shuttles then it's "greater" than o2 and hence it goes to the end of the list
        else return o1HasShuttles ? 1 : -1;
    }

    private boolean hasShuttles(GraphPath gp) {
        return gp.edges
                .stream()
                .anyMatch(e -> {
                    if (e instanceof TransitBoardAlight) return ((TransitBoardAlight) e).getPattern().route.isShuttle();
                    else return false;
                });
    }
}
