package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

import java.util.Random;

public class PatternDepartVertex extends PatternStopVertex {

    private static final long serialVersionUID = 20140101;

    /** constructor for table trip patterns */
    public PatternDepartVertex(Graph g, TripPattern pattern, int stopIndex) {
        super(g, makeLabel(pattern, stopIndex), pattern, pattern.stopPattern.stops[stopIndex]);
    }

    /** constructor for temporary trip patterns */
    public PatternDepartVertex(Graph g, TripPattern pattern, int stopIndex, Stop stop) {
        super(g, makeTemporaryLabel(pattern, stopIndex), pattern, stop);
    }

    // constructor for single-trip hops with no trip pattern (frequency patterns) is now missing
    // it is possible to have both a freq and non-freq pattern with the same stop pattern

    private static String makeLabel(TripPattern pattern, int stop) {
        return String.format("%s_%02d_D", pattern.code, stop);
    }

    private static String makeTemporaryLabel(TripPattern pattern, int stop) {
        return String.format("%s_%02d_D_%d", pattern.code + "_temp", stop, new Random().nextInt());
    }

}
