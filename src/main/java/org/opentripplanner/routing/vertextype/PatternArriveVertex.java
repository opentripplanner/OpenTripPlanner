package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class PatternArriveVertex extends PatternStopVertex {

    private static final long serialVersionUID = 20140101;

    /** constructor for table trip patterns */
    public PatternArriveVertex(Graph g, TripPattern pattern, int stopIndex) {
        super(g, makeLabel(pattern, stopIndex), pattern, pattern.stopPattern.stops[stopIndex]);
    }

    // constructor for frequency patterns is now missing
    // it is possible to have both a freq and non-freq pattern with the same stop pattern

    private static String makeLabel(TripPattern pattern, int stop) {
        return String.format("%s_%02d_A", pattern.code, stop);
    }


}
