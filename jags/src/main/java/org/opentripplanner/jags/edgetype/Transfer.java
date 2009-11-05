package org.opentripplanner.jags.edgetype;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.TraverseOptions;
import org.opentripplanner.jags.core.TraverseResult;

import com.vividsolutions.jts.geom.Geometry;

public class Transfer implements Traversable {

    double distance = 0;

    public Transfer(double distance) {
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
        return new TraverseResult(distance / wo.speed + wo.transferPenalty, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        return new TraverseResult(distance / wo.speed + wo.transferPenalty, s1);
    }

}
