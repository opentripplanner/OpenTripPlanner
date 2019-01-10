package org.opentripplanner.routing.vertextype.flex;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TemporaryTransitStop extends TransitStop implements TemporaryVertex {

    // stop is *at* a street vertex
    private StreetVertex streetVertex;

    public TemporaryTransitStop(Stop stop, StreetVertex streetVertex) {
        super(null, stop);
        this.streetVertex = streetVertex;
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }

    public StreetVertex getStreetVertex() {
        return streetVertex;
    }

    @Override
    public boolean checkCallAndRideBoardAlightOk(State state) {
        return true;
    }

    @Override
    public boolean checkCallAndRideStreetLinkOk(State s0) {
        return true;
    }
}
