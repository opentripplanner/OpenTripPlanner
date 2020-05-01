package org.opentripplanner.routing.edgetype.flex;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

/**
 * This is associated with a PatternHop for stop_time information, but its geometry bears no
 * relation to the route geometry. And its timing is approximate.
 */
public class TemporaryDirectPatternHop extends TemporaryPartialPatternHop implements TemporaryEdge {
    private static final long serialVersionUID = 1L;

    /*
     * This is the direct time a car would take to do this hop. Based on DRT service parameters,
     * it actually may take a different amount of time.
     */
    private int directTime;

    public TemporaryDirectPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, LineString geometry, int time) {
        super(hop, from, to, fromStop, toStop);
        setGeometry(geometry);
        this.directTime = time;
    }

    @Override
    public boolean isUnscheduled() {
        return true;
    }

    @Override
    public boolean isTrivial(RoutingRequest options) {
        return false;
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return directTime;
    }

    @Override
    public int getRunningTime(State s0) {
        TripTimes tt = s0.getTripTimes();
        return tt.getDemandResponseMaxTime(directTime);
    }

    @Override
    public int getWeight(State s0, int runningTime) {
        return (int) Math.round(s0.getOptions().flexCallAndRideReluctance * runningTime);
    }

    @Override
    public boolean isDeviatedRouteBoard() {
        return true;
    }

    @Override
    public boolean isDeviatedRouteAlight() {
        return true;
    }

    @Override
    public int getDirectVehicleTime() {
        return directTime;
    }

    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementCallAndRideTime(directTime);
        if (s1.getCallAndRideTime() >= s0.getOptions().flexMaxCallAndRideSeconds) {
            return null;
        }
        return super.traverse(s0, s1);
    }
}
