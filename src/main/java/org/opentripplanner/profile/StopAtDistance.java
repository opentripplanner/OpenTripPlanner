package org.opentripplanner.profile;

import lombok.AllArgsConstructor;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;

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
    //public State state;
    public List<WalkStep> walkSteps;

    /** @param state a state at a TransitStop */
    public StopAtDistance (State state) {
        //this.state = state;
        // This is rather slow. We are finding paths for every stop we encounter, not just those in the result.
        // TODO Retain the routing contexts and destroy them all at request's end, so we can generate walksteps only as needed.
        // TODO profile why generating the path or the walksteps is slow
        GraphPath path = new GraphPath(state, false);
        walkSteps = PlanGenerator.generateWalkSteps(path.states.toArray(new State[0]), null);
        etime = (int) state.getElapsedTimeSeconds();
        distance = (int) state.getWalkDistance(); // TODO includes driving? Is this really needed?
        mode = state.getNonTransitMode(); // not sure if this is reliable, reset in caller.
        if (state.getVertex() instanceof TransitStop) {
            TransitStop tstop = (TransitStop) state.getVertex();
            stop = tstop.getStop();
        }
    }

    @Override
    public int compareTo(StopAtDistance that) {
        return this.etime - that.etime;
    }

    public String toString() {
        return String.format("stop %s via mode %s at %d min", stop.getCode(), mode, etime / 60);
    }

}
