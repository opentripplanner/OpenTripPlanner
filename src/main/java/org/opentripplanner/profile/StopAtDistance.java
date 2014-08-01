package org.opentripplanner.profile;

import lombok.AllArgsConstructor;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * A stop associated with its elapsed time from a search location and the path for reaching it.
 * Used in profile routing.
 * TODO rename this StopPath or something.
 */
public class StopAtDistance implements Comparable<StopAtDistance> {

    public Stop stop;
    public TraverseMode mode;
    public int etime;
    public int distance; // deprecate?
    public State state;

    /** @param state a state at a TransitStop */
    public StopAtDistance (State state) {
        TransitStop tstop = (TransitStop) state.getVertex();
        this.state = state;
        stop = tstop.getStop();
        etime = (int) state.getElapsedTimeSeconds();
        distance = (int) state.getWalkDistance(); // TODO includes driving? Is this really needed?
        mode = state.getNonTransitMode(); // not sure if this is reliable, reset in caller.
    }

    @Override
    public int compareTo(StopAtDistance that) {
        return this.etime - that.etime;
    }

    public String toString() {
        return String.format("stop %s via mode %s at %d min", stop.getCode(), mode, etime / 60);
    }

}
