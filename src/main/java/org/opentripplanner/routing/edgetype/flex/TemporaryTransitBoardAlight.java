package org.opentripplanner.routing.edgetype.flex;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PartialPatternHop;
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

    // normalized to [0, 1]
    private double startIndex;
    private double endIndex;

    public TemporaryTransitBoardAlight(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                                       int stopIndex, TraverseMode mode, PartialPatternHop hop) {
        super(fromStopVertex, toPatternVertex, stopIndex, mode);
        setIndices(hop);
    }

    public TemporaryTransitBoardAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                                       int stopIndex, TraverseMode mode, PartialPatternHop hop) {
        super(fromPatternStop, toStationVertex, stopIndex, mode);
        setIndices(hop);
    }

    private void setIndices(PartialPatternHop hop) {
        this.startIndex = hop.getStartIndex() / hop.getOriginalHopLength();
        this.endIndex = hop.getEndIndex() / hop.getOriginalHopLength();
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
        TripTimes tripTimes = timetable.getNextTrip(s0, sd, getStopIndex(), boarding, startIndex, -2);
        return tripTimes;
    }

    @Override
    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        int stopIndex = getStopIndex();
        if (boarding) {
            // we need to fudge this by two seconds so that we can optimize later on.
            int adjustment = (int) (startIndex * (tripTimes.getRunningTime(stopIndex)));
            return  (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + adjustment) - s0.getTimeSeconds()) + 2;
        }
        else {
            int adjustment = (int) (startIndex * (tripTimes.getRunningTime(stopIndex)));
            return (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) + adjustment));
        }
    }

    @Override
    public long getPenaltyWeight(State s0, RoutingRequest options, Trip trip) {
        return super.getPenaltyWeight(s0, options, trip) + options.flagStopExtraPenalty;
    }
}
