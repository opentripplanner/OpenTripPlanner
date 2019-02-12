package org.opentripplanner.routing.edgetype;

import java.util.Locale;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;

import org.locationtech.jts.geom.LineString;


/**
 *  Models waiting in a station on a vehicle.  The vehicle is not permitted to change 
 *  names during this time -- PatternInterlineDwell represents that case.
 */
public class PatternDwell extends TablePatternEdge implements OnboardEdge, DwellEdge {
    
    private static final long serialVersionUID = 1L;

    private int stopIndex;
    
    public PatternDwell(PatternArriveVertex from, PatternDepartVertex to, int stopIndex, TripPattern tripPattern) {
        super(from, to);
        this.stopIndex = stopIndex;
    }

    public String getDirection() {
        return getPattern().getDirection();
    }

    public double getDistance() {
        return 0;
    }
        
    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(getPattern().route);
    }

    public String getName() {
        return GtfsLibrary.getRouteName(getPattern().route);
    }

    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    public State traverse(State state0) {
        //int trip = state0.getTrip();
        TripTimes tripTimes = state0.getTripTimes();
        int dwellTime = tripTimes.getDwellTime(stopIndex);
        StateEditor s1 = state0.edit(this);
        s1.setBackMode(getMode());
        s1.incrementTimeInSeconds(dwellTime);
        s1.incrementWeight(dwellTime);
        return s1.makeState();
    }

    @Override
    public State optimisticTraverse(State s0) {
        int dwellTime = getPattern().scheduledTimetable.getBestDwellTime(stopIndex);
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(dwellTime);
        s1.setBackMode(getMode());
        s1.incrementWeight(dwellTime);
        return s1.makeState();
    }
    
    @Override
    public double timeLowerBound(RoutingRequest options) {
        return getPattern().scheduledTimetable.getBestDwellTime(stopIndex);
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }

    public LineString getGeometry() {
        return null;
    }

    public String toString() {
        return "PatternDwell(" + super.toString() + ")";
    }

    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }

    @Override
    public int getStopIndex() {
        return stopIndex;
    }
}
