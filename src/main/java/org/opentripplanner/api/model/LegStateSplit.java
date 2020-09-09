package org.opentripplanner.api.model;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.core.State;

import java.util.List;

public class LegStateSplit {

    private final List<State> states;
    private final Coordinate nextSplitBeginning;

    public LegStateSplit(List<State> states, Coordinate coordinate) {
        this.states = states;
        this.nextSplitBeginning = coordinate;
    }

    public List<State> getStates() {
        return states;
    }

    public Coordinate getNextSplitBeginning() {
        return nextSplitBeginning;
    }
}
