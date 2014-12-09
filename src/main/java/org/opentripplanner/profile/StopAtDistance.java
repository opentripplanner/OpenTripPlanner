package org.opentripplanner.profile;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.List;

/**
 * A stop associated with its elapsed time from a search location and the path for reaching it.
 * Used in profile routing.
 * TODO rename this StopPath or something.
 */
public class StopAtDistance implements Comparable<StopAtDistance> {

    public StopCluster stop; // TODO rename to stopCluster, use StopCluster objects not strings?
    public TraverseMode mode;
    public int etime;
    public State state;

    /** @param state a state at a TransitStop */
    public StopAtDistance (State state) {
        this.state = state;
        etime = (int) state.getElapsedTimeSeconds();
        /* This mode is not reliable for drive to transit (which ends with walking), reset in caller. */
        mode = state.getNonTransitMode();
        if (state.getVertex() instanceof TransitStop) {
            TransitStop tstop = (TransitStop) state.getVertex();
            stop = state.getOptions().rctx.graph.index.stopClusterForStop.get(tstop.getStop());
        }
    }

    @Override
    public int compareTo(StopAtDistance that) {
        return this.etime - that.etime;
    }

    public String toString() {
        return String.format("stop cluster %s via mode %s at %d min", stop, mode, etime / 60);
    }

}
