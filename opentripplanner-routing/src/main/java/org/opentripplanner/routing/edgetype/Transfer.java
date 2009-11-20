package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class Transfer extends AbstractEdge {
    
    /*
     * Models traveling between two stops in a transfer complex, such as a large underground subway station. Increments the travel
     * time by an amount determined by the distance and travel speed. 
     * 
     * Note that a transfer penalty is not applied by the Transfer edge-type because of the need to penalize transfers in situations
     * where changing vehicles either involves the same physical stop or movement between two physical stops not intermediated by a 
     * Transfer edge-type. Instead, penalties on transfers are applied at Board and Alight edge-types.
     */

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

}
