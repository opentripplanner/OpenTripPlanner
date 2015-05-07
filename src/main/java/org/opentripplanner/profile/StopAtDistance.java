package org.opentripplanner.profile;

import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * A stop cluster associated with its elapsed time from a search location and the path for reaching it.
 * Used in profile routing.
 * TODO rename this StopPath or something.
 */
public class StopAtDistance implements Comparable<StopAtDistance> {

    public StopCluster stopCluster; // TODO rename to stopCluster, use StopCluster objects not strings?
    public QualifiedMode qmode;
    public int etime;
    public State state;

    /** 
     * @param state a state at a TransitStop, at the tail of a path
     * @param qmode the qualified mode (e.g. BICYCLE_RENT) used to produce this path
     */
    public StopAtDistance (State state, QualifiedMode qmode) {
        this.state = state;
        etime = (int) state.getElapsedTimeSeconds();
        // The mode from the state is not reliable for drive to transit or bicycle rental (which end with walking).
        // Use the more specific mode passed in from the caller.
        this.qmode = qmode;
        if (state.getVertex() instanceof TransitStop) {
            TransitStop tstop = (TransitStop) state.getVertex();
            stopCluster = state.getOptions().rctx.graph.index.stopClusterForStop.get(tstop.getStop());
        }
    }

    @Override
    public int compareTo(StopAtDistance that) {
        return this.etime - that.etime;
    }

    public String toString() {
        return String.format("stop cluster %s via mode %s at %d min", stopCluster.id, qmode, etime / 60);
    }

}
