package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/**
 * Created by dbenoff on 2/10/17.
 */
public class TemporaryTransitBoardAlight extends TransitBoardAlight implements TemporaryEdge {

    double distanceRatio;

    public TemporaryTransitBoardAlight(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                                       int stopIndex, TraverseMode mode, double distanceRatio) {
        super(fromStopVertex, toPatternVertex, stopIndex, mode);
        this.distanceRatio = distanceRatio;
    }

    public TemporaryTransitBoardAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                                       int stopIndex, TraverseMode mode, double distanceRatio) {
        super(fromPatternStop, toStationVertex, stopIndex, mode);
        this.distanceRatio = distanceRatio;
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }


    @Override
    public TripTimes getNextTrip(State s0, ServiceDay sd) {
        RoutingRequest options = s0.getOptions();
        Timetable timetable = getPattern().getUpdatedTimetable(options, sd);
        TripTimes tripTimes = timetable.getNextTrip(s0, sd, getStopIndex(), boarding, distanceRatio);
        return tripTimes;
    }

    @Override
    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        int stopIndex = getStopIndex();
        int adjustment = (int) (distanceRatio * (tripTimes.getRunningTime(stopIndex)));
        // we need to fudge this by two seconds so that we can optimize later on.
        return boarding ?
                (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + adjustment) - s0.getTimeSeconds()) + 2:
                (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) + adjustment));
    }
}
