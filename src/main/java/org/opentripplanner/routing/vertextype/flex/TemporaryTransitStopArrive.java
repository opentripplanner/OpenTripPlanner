package org.opentripplanner.routing.vertextype.flex;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;

public class TemporaryTransitStopArrive extends TransitStopArrive implements TemporaryVertex {
    public TemporaryTransitStopArrive(Stop stop, TransitStop transitStop) {
        super(null, stop, transitStop);
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }
}
