package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class Alight extends AbstractEdge {

    public Alight(Vertex fromv, Vertex tov) {
        super(fromv, tov);
    }
    
    /**
     *  Models alighting a vehicle - that is to say, traveling from a station on vehicle to a station off vehicle. When traversed forward,
     *  the the resultant state is unchanged. When traversed backward, the resultant state has the time of the last arrival 
     *  before the input state. An alighting penalty can also be applied to discourage transfers.
     */

    String start_id; // a street vertex's id

    String end_id; // a transit node's GTFS id

    private static final long serialVersionUID = 1L;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        return null;
    }

    public Geometry getGeometry() {
        // TODO Auto-generated method stub -- need to provide link between
        // location of street node and location of transit node.
        return null;
    }

    public TransportationMode getMode() {
        return TransportationMode.ALIGHTING;
    }

    public String getName() {
        //this text won't be used -- the streetTransitLink or StationEntrance's text will 
        return "alight from vehicle";
    }

    public String getStart() {
        return null;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        return new TraverseResult(1, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        return new TraverseResult(1, s1);
    }

}
