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

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.TableTripPattern;
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

    public PatternBoard[/* stops - 1 */][/* patterns */] boards;// array of patternboards for each
                                                                // stop

    public PatternAlight[/* stops - 1 */][/* patterns */] alights;// array of patternalights for
                                                                  // each stop

    public RaptorRoute(int nStops, int nPatterns) {
        stops = new RaptorStop[nStops];
        boards = new PatternBoard[nStops - 1][nPatterns];
        alights = new PatternAlight[nStops - 1][nPatterns];
    }

    public int getNStops() {
        return stops.length;
    }

    public int getBoardTime(int patternIndex, int tripIndex, int stopNo) {
        PatternBoard board = boards[stopNo][patternIndex];
        TableTripPattern pattern = board.getPattern();
        return pattern.getDepartureTime(stopNo, tripIndex);
    }

    public int getAlightTime(TripTimes tripTimes, int stopNo) {
        return tripTimes.getArrivalTime(stopNo - 1);
    }

    public RaptorBoardSpec getTripIndex(RoutingRequest request, int arrivalTime, int stopNo) {

        RaptorBoardSpec spec = new RaptorBoardSpec();
        spec.departureTime = Integer.MAX_VALUE;
        spec.tripTimes = null;
        spec.patternIndex = -1;

        for (int i = 0; i < boards[stopNo].length; ++i) {
            PatternBoard board = boards[stopNo][i];

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
