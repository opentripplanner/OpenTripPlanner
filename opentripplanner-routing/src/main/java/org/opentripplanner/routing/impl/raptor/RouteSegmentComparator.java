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
