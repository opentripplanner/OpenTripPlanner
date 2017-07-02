package org.opentripplanner.routing.edgetype.flex;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
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
    private PartialPatternHop hop;

    public TemporaryTransitBoardAlight(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                                       int stopIndex, PartialPatternHop hop) {
        super(fromStopVertex, toPatternVertex, stopIndex, hop.getMode());
        setIndices(hop);
    }

    public TemporaryTransitBoardAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                                       int stopIndex, PartialPatternHop hop) {
        super(fromPatternStop, toStationVertex, stopIndex, hop.getMode());
        setIndices(hop);
    }

    private void setIndices(PartialPatternHop hop) {
        this.startIndex = hop.getStartIndex() / hop.getOriginalHopLength();
        this.endIndex = hop.getEndIndex() / hop.getOriginalHopLength();
        this.hop = hop;
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }


    @Override
    public TripTimes getNextTrip(State s0, ServiceDay sd) {
        double adjustment = boarding ? startIndex : -1 * (1 - endIndex);
        RoutingRequest options = s0.getOptions();
        Timetable timetable = getPattern().getUpdatedTimetable(options, sd);
        TripTimes tripTimes = timetable.getNextTrip(s0, sd, getStopIndex(), boarding, adjustment);
        return tripTimes;
    }

    @Override
    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        int stopIndex = getStopIndex();
        if (boarding) {
            // we need to fudge this by two seconds so that we can optimize later on.
            int offset = (int) Math.round(startIndex * (tripTimes.getRunningTime(stopIndex)));
            return  (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + offset) - s0.getTimeSeconds());
        }
        else {
            int offset = (int) Math.round((1-endIndex) * (tripTimes.getRunningTime(stopIndex - 1)));
            return (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) - offset));
        }
    }

    @Override
    public long getPenaltyWeight(State s0, RoutingRequest options, Trip trip) {
        return super.getPenaltyWeight(s0, options, trip) + options.flagStopExtraPenalty;
    }
}
