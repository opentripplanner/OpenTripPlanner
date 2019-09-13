package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStop;

public class FlexTransitBoardAlight extends TransitBoardAlight implements TemporaryEdge {

    // normalized to [0, 1]
    private double startIndex;
    private double endIndex;
    private PartialPatternHop hop;

    public FlexTransitBoardAlight(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                                       int stopIndex, PartialPatternHop hop) {
        super(fromStopVertex, toPatternVertex, stopIndex, hop.getMode());
        setIndices(hop);
    }

    public FlexTransitBoardAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                                       int stopIndex, PartialPatternHop hop) {
        super(fromPatternStop, toStationVertex, stopIndex, hop.getMode());
        setIndices(hop);
    }

    private void setIndices(PartialPatternHop hop) {
        if (hop.getOriginalHopLength() > 0) {
            this.startIndex = hop.getStartIndex() / hop.getOriginalHopLength();
            this.endIndex = hop.getEndIndex() / hop.getOriginalHopLength();
        } else {
            // entirely-deviated area hop. Never add entire path time.
            this.startIndex = 0.0d;
            this.endIndex = 0.0d;
        }
        this.hop = hop;
    }

    @Override
    public State traverse(State s0) {
        // do not board call-n-ride if it is not a temporary stop and we aren't doing a fixed route-C&R transfer
        if (!s0.getOptions().arriveBy && boarding && hop.isDeviatedRouteBoard()
            && !((TransitStopDepart) getFromVertex()).getStopVertex().checkCallAndRideBoardAlightOk(s0)) {
            return null;
        }

        if (s0.getOptions().arriveBy && !boarding && hop.isDeviatedRouteAlight()
            && !(((TransitStopArrive) getToVertex()).getStopVertex().checkCallAndRideBoardAlightOk(s0))) {
            return null;
        }

        return super.traverse(s0);
    }

    @Override
    public TripTimes getNextTrip(State s0, ServiceDay sd, Timetable timetable) {
        if (hop.isUnscheduled()) {
            RoutingRequest options = s0.getOptions();
            int time = (int) Math.round(hop.timeLowerBound(options));
            return timetable.getNextCallNRideTrip(s0, sd, getStopIndex(), boarding, time);
        }
        double adjustment = boarding ? startIndex : -1 * (1 - endIndex);
        return timetable.getNextTrip(s0, sd, getStopIndex(), boarding, adjustment, hop.getStartVehicleTime(), hop.getEndVehicleTime());
    }

    @Override
    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        if (hop.isUnscheduled()) {
            int currTime = sd.secondsSinceMidnight(s0.getTimeSeconds());
            boolean useClockTime = !s0.getOptions().flexIgnoreDrtAdvanceBookMin;
            long clockTime = s0.getOptions().clockTimeSec;
            if (boarding) {
                int scheduledTime = tripTimes.getCallAndRideBoardTime(getStopIndex(), currTime, (int) hop.timeLowerBound(s0.getOptions()), sd, useClockTime, clockTime);
                if (scheduledTime < 0)
                    throw new IllegalArgumentException("Unexpected bad wait time");
                return (int) (sd.time(scheduledTime) - s0.getTimeSeconds());
            } else {
                int scheduledTime = tripTimes.getCallAndRideAlightTime(getStopIndex(), currTime, (int) hop.timeLowerBound(s0.getOptions()), sd, useClockTime, clockTime);
                if (scheduledTime < 0)
                    throw new IllegalArgumentException("Unexpected bad wait time");
                return (int) (s0.getTimeSeconds() - (sd.time(scheduledTime)));
            }
        }
        int stopIndex = getStopIndex();
        if (boarding) {
            int startVehicleTime = hop.getStartVehicleTime();
            if (startVehicleTime != 0) {
                startVehicleTime = tripTimes.getDemandResponseMaxTime(startVehicleTime);
            }
            int offset = (int) Math.round(startIndex * (tripTimes.getRunningTime(stopIndex)));
            return  (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + offset - startVehicleTime) - s0.getTimeSeconds());
        }
        else {
            int endVehicleTime = hop.getEndVehicleTime();
            if (endVehicleTime != 0) {
                endVehicleTime = tripTimes.getDemandResponseMaxTime(endVehicleTime);
            }
            int offset = (int) Math.round((1-endIndex) * (tripTimes.getRunningTime(stopIndex - 1)));
            return (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) - offset + endVehicleTime));
        }
    }

    @Override
    public long getExtraWeight(RoutingRequest options) {
        boolean deviatedRoute = (boarding && hop.isDeviatedRouteBoard()) || (!boarding && hop.isDeviatedRouteAlight());
        return (deviatedRoute ? options.flexDeviatedRouteExtraPenalty : options.flexFlagStopExtraPenalty);
    }

    @Override
    public boolean isDeviated() {
        return boarding ? hop.isDeviatedRouteBoard() : hop.isDeviatedRouteAlight();
    }

    @Override
    public String toString() {
        return "FlexTransitBoardAlight(" +
                (boarding ? "boarding " : "alighting ") +
                getFromVertex() + " to " + getToVertex() + ")";
    }
}
