package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class Transfer extends AbstractEdge {

    private static final long serialVersionUID = 1L;
    
    double distance = 0;

    public Transfer(Vertex fromv, Vertex tov, double distance) {
        super(fromv, tov);
        this.distance = distance;
    }

    public String getDirection() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getDistance() {
        return distance;
    }

    public String getEnd() {
        // TODO Auto-generated method stub
        return null;
    }

    public Geometry getGeometry() {
        // TODO Auto-generated method stub
        return null;
    }

    public TransportationMode getMode() {
        return TransportationMode.TRANSFER;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return "transfer";
    }

    public String getStart() {
        // TODO Auto-generated method stub
        return null;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        s1.incrementTimeInSeconds((int)(distance / wo.speed));
        return new TraverseResult(distance / wo.speed, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        s1.incrementTimeInSeconds(-(int)(distance / wo.speed));
        return new TraverseResult(distance / wo.speed, s1);
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Transfer)) {
            return false;
        }
        Transfer t = (Transfer) o;
        return t.getToVertex().equals(getToVertex()) && t.getFromVertex().equals(getFromVertex());
    }
    
    public int hashCode() {
        return getToVertex().hashCode() ^ getFromVertex().hashCode();
    }
}
