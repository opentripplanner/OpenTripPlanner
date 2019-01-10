package org.opentripplanner.routing.vertextype.flex;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

public class TemporaryPatternDepartVertex extends PatternDepartVertex implements TemporaryVertex {

    public TemporaryPatternDepartVertex(TripPattern pattern, int stopIndex, Stop stop) {
        super(null, pattern, stopIndex, stop);
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }
}
