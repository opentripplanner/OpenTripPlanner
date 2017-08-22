/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

public class FlexTransitBoardAlight extends TransitBoardAlight {

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
    public TripTimes getNextTrip(State s0, ServiceDay sd) {
        if (hop.isUnscheduled()) {
            RoutingRequest options = s0.getOptions();
            Timetable timetable = getPattern().getUpdatedTimetable(options, sd);
            int stopIndex = boarding ? getStopIndex() : getStopIndex() - 1;
            int time = hop.getRunningTime(s0);
            return timetable.getNextTrip(s0, sd, stopIndex, boarding, 0, 0, time);
        }
        double adjustment = boarding ? startIndex : -1 * (1 - endIndex);
        RoutingRequest options = s0.getOptions();
        Timetable timetable = getPattern().getUpdatedTimetable(options, sd);
        TripTimes tripTimes = timetable.getNextTrip(s0, sd, getStopIndex(), boarding, adjustment, hop.getStartVehicleTime(), hop.getEndVehicleTime());
        return tripTimes;
    }

    @Override
    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        if (hop.isUnscheduled()) {
            if (boarding) {
                return (int) (sd.time(tripTimes.getDepartureTime(getStopIndex())) - s0.getTimeSeconds());
            } else {
                return (int) (s0.getTimeSeconds() - (sd.time(tripTimes.getArrivalTime(getStopIndex() - 1)) + hop.getRunningTime(s0)));
            }
        }
        int stopIndex = getStopIndex();
        if (boarding) {
            int offset = (int) Math.round(startIndex * (tripTimes.getRunningTime(stopIndex)));
            return  (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + offset) - s0.getTimeSeconds()) - hop.getStartVehicleTime();
        }
        else {
            int offset = (int) Math.round((1-endIndex) * (tripTimes.getRunningTime(stopIndex - 1)));
            return (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) - offset)) + hop.getEndVehicleTime();
        }
    }

    @Override
    public long getExtraWeight(RoutingRequest options) {
        boolean deviatedRoute = (boarding && hop.isDeviatedRouteBoard()) || (!boarding && hop.isDeviatedRouteAlight());
        return (deviatedRoute ? options.deviatedRouteExtraPenalty : options.flagStopExtraPenalty);
    }

    @Override
    public String toString() {
        return "FlexTransitBoardAlight(" +
                (boarding ? "boarding " : "alighting ") +
                getFromVertex() + " to " + getToVertex() + ")";
    }
}
