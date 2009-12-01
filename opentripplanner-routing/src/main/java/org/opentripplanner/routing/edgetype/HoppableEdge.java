package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Stop;

public interface HoppableEdge {
    public Stop getStartStop();
    public Stop getEndStop();
}
