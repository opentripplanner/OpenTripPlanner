package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/**
 * Created by sjacobs on 2/17/17.
 */
public class TransitBoardAlightAtFlex extends TransitBoardAlight {

    private final double percentageOfHop;

    /** Boarding constructor (TransitStopDepart --> PatternStopVertex) */
    public TransitBoardAlightAtFlex (TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                               int stopIndex, TraverseMode mode, double percentageOfHop) {
        super(fromStopVertex, toPatternVertex, stopIndex, mode);
        this.percentageOfHop = 1 - percentageOfHop;
    }

    /** Alighting constructor (PatternStopVertex --> TransitStopArrive) */
    public TransitBoardAlightAtFlex (PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                               int stopIndex, TraverseMode mode, double percentageOfHop) {
        super(fromPatternStop, toStationVertex, stopIndex, mode);
        this.percentageOfHop = percentageOfHop;
    }

    @Override
    public TripTimes getNextTrip(State s0, ServiceDay sd) {
        RoutingRequest options = s0.getOptions();
        Timetable timetable = getPattern().getUpdatedTimetable(options, sd);
        TripTimes tripTimes = timetable.getNextTrip(s0, sd, getStopIndex(), boarding, percentageOfHop);
        return tripTimes;
    }

    @Override
    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        int stopIndex = getStopIndex();
        int adjustment = (int) (percentageOfHop * (tripTimes.getRunningTime(stopIndex)));
        // we need to fudge this by two seconds so that we can optimize later on.
        return boarding ?
                (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + adjustment) - s0.getTimeSeconds()) + 2:
                (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) + adjustment));
    }

    @Override
    public String toString() {
        return "TransitBoardAlightAtFlex(" +
                (boarding ? "boarding " : "alighting ") +
                getFromVertex() + " to " + getToVertex() + ")";
    }
}
