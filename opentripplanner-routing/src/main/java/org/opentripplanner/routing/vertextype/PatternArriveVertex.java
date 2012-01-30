package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class PatternArriveVertex extends PatternStopVertex {

    private static final long serialVersionUID = 4858000141204480555L;

    public PatternArriveVertex(Graph g, TripPattern tripPattern, StopTime stopTime) {
        super(g, makeLabel(tripPattern.getExemplar(), stopTime), tripPattern, stopTime.getStop());
    }

    // constructor for single-trip hops with no trip pattern
    public PatternArriveVertex(Graph g, Trip trip, StopTime stopTime) {
        super(g, makeLabel(trip, stopTime), null, stopTime.getStop());
    }

    private static String makeLabel(Trip t, StopTime st) {
        return GtfsLibrary.convertIdToString(st.getStop().getId()) + "_" + 
                GtfsLibrary.convertIdToString(t.getId()) + "_" + 
                st.getStopSequence() + "_A";
    }

}
