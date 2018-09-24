package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

import java.util.Random;

public class PatternArriveVertex extends PatternStopVertex {

    private static final long serialVersionUID = 20140101;

    /** constructor for table trip patterns */
    public PatternArriveVertex(Graph g, TripPattern pattern, int stopIndex) {
        super(g, makeLabel(pattern, stopIndex), pattern, pattern.stopPattern.stops[stopIndex]);
    }

    /** constructor for temporary trip patterns */
    public PatternArriveVertex(Graph g, TripPattern pattern, int stopIndex, Stop stop) {
        super(g, makeTemporaryLabel(pattern, stopIndex), pattern, stop);
    }

    // constructor for frequency patterns is now missing
    // it is possible to have both a freq and non-freq pattern with the same stop pattern

    private static String makeLabel(TripPattern pattern, int stop) {
        return String.format("%s_%02d_A", pattern.code, stop);
    }

    private static String makeTemporaryLabel(TripPattern pattern, int stop) {
        return String.format("%s_%02d_A_%d", pattern.code + "_temp", stop, new Random().nextInt());
    }

}
