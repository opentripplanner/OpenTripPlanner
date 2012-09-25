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

package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;
import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * A set of trips that have the same stops in the same order In other words, a TripPattern, but
 * across all service days.
 * 
 * @author novalis
 * 
 */
public class RaptorRoute implements Serializable {
    private static final long serialVersionUID = -882026076718046636L;

    public RaptorStop[] stops;

    public TransitBoardAlight[/* stops - 1 */][/* patterns */] boards;// array of patternboards for each
                                                                // stop

    public TransitBoardAlight[/* stops - 1 */][/* patterns */] alights;// array of patternalights for
                                                                  // each stop

    public TraverseMode mode;

    public HashMap<AgencyAndId, RaptorInterlineData> interlinesOut = new HashMap<AgencyAndId, RaptorInterlineData>();
    public HashMap<AgencyAndId, RaptorInterlineData> interlinesIn = new HashMap<AgencyAndId, RaptorInterlineData>();

    public RaptorRoute(int nStops, int nPatterns) {
        stops = new RaptorStop[nStops];
        boards = new TransitBoardAlight[nStops - 1][nPatterns];
        alights = new TransitBoardAlight[nStops - 1][nPatterns];
    }

    public int getNStops() {
        return stops.length;
    }

    public int getAlightTime(TripTimes tripTimes, int stopNo) {
        return tripTimes.getArrivalTime(stopNo - 1);
    }

    public int getBoardTime(TripTimes tripTimes, int stopNo) {
        return tripTimes.getDepartureTime(stopNo);
    }

    public RaptorBoardSpec getTripIndex(RoutingRequest request, int arrivalTime, int stopNo) {

        RaptorBoardSpec spec = new RaptorBoardSpec();
        spec.departureTime = Integer.MAX_VALUE;
        spec.tripTimes = null;
        spec.patternIndex = -1;

        for (int i = 0; i < boards[stopNo].length; ++i) {
            TransitBoardAlight board = boards[stopNo][i];

            State state = new State(board.getFromVertex(), arrivalTime, request);
            State result = board.traverse(state);
            if (result == null)
                continue;
            int time = (int) result.getTime();
            if (time < spec.departureTime) {
                spec.departureTime = time;
                spec.tripTimes = result.getTripTimes();
                spec.patternIndex = i;
                spec.serviceDay = result.getServiceDay();
                spec.tripId = result.getTripId();
            }

        }

        if (spec.patternIndex == -1)
            return null;

        return spec;
    }

    public RaptorBoardSpec getTripIndexReverse(RoutingRequest request, int arrivalTime, int stopNo) {

        RaptorBoardSpec spec = new RaptorBoardSpec();
        spec.departureTime = 0;
        spec.tripTimes = null;
        spec.patternIndex = -1;

        for (int i = 0; i < alights[stopNo-1].length; ++i) {
            TransitBoardAlight alight = alights[stopNo-1][i];

            State state = new State(alight.getToVertex(), arrivalTime, request);
            State result = alight.traverse(state);
            if (result == null)
                continue;
            int time = (int) result.getTime();
            if (time > spec.departureTime) {
                spec.departureTime = time;
                spec.tripTimes = result.getTripTimes();
                spec.patternIndex = i;
                spec.serviceDay = result.getServiceDay();
                spec.tripId = result.getTripId();
            }
        }

        if (spec.patternIndex == -1)
            return null;

        return spec;
    }

    public String toString() {
        return GtfsLibrary.getRouteName(boards[0][0].getPattern().getExemplar().getRoute())
                + " from " + stops[0].stopVertex.getLabel();
    }

}
