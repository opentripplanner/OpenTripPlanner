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

import java.util.Comparator;
import java.util.HashMap;

import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.transit_index.RouteSegment;

public class RouteSegmentComparator implements Comparator<RouteSegment> {

    HashMap<TripPattern, Integer> patternOrder = new HashMap<TripPattern, Integer>();
    int maxPattern;
    
    int getPatternIndex(TripPattern pattern) {
        Integer patternIndex = patternOrder.get(pattern);
        if (patternIndex == null) {
            patternIndex = maxPattern++;
            patternOrder.put(pattern, patternIndex);
        }
        return patternIndex;
    }
    
    @Override
    public int compare(RouteSegment arg0, RouteSegment arg1) {
        TransitBoardAlight board0 = (TransitBoardAlight) arg0.board;
        TransitBoardAlight board1 = (TransitBoardAlight) arg1.board;
        if (board0 == null) {
            if (board1 == null) {
                TransitBoardAlight alight0 = (TransitBoardAlight) arg0.alight;
                TransitBoardAlight alight1 = (TransitBoardAlight) arg1.alight;
                // both are last segment
                if (alight0.getStopIndex() != alight1.getStopIndex()) {
                    System.out.println("DOOM");
                }
                return getPatternIndex(alight0.getPattern()) - getPatternIndex(alight1.getPattern());
            }
            return 1;
        }
        if (board1 == null) {
            return -1;
        }

        int stopComp = board0.getStopIndex() - board1.getStopIndex();
        if (stopComp == 0) {
            return getPatternIndex(board0.getPattern()) - getPatternIndex(board1.getPattern());
        }
        return stopComp;
    }

}
