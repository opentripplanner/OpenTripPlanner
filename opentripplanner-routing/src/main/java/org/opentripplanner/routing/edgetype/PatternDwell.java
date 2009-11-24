package org.opentripplanner.routing.edgetype;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class PatternDwell extends AbstractEdge {

    /**
     *  Models traveling from one station on vehicle to another station on vehicle by incrementing the State.time by an amount contingent
     *  upon the State.pattern
     */
    
    private static final long serialVersionUID = 1L;

    private TripPattern pattern;
    private int stopIndex;
    
    public PatternDwell(Vertex startJourney, Vertex endJourney, int stopIndex, TripPattern tripPattern) {
        super(startJourney, endJourney);
        this.stopIndex = stopIndex;
        this.pattern = tripPattern;
    }

    public String getDirection() {
        return pattern.exemplar.getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        return null;
    }

    public TransportationMode getMode() {
        return GtfsLibrary.getTransportationMode(pattern.exemplar.getRoute());
    }

    public String getStart() {
        return null;
    }

    public String getName() {
        return GtfsLibrary.getRouteName(pattern.exemplar.getRoute());
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        int dwellTime = pattern.getDwellTime(stopIndex, state0.getPattern());
        state1.incrementTimeInSeconds(dwellTime);
        return new TraverseResult(dwellTime, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        int dwellTime = pattern.getDwellTime(stopIndex, state0.getPattern());
        state1.incrementTimeInSeconds(-dwellTime);
        return new TraverseResult(dwellTime, state1);
    }

    public Geometry getGeometry() {
        return null;
    }

}
