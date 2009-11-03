package org.opentripplanner.jags.edgetype;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;

import com.vividsolutions.jts.geom.Geometry;

public class StreetTransitLink extends AbstractPayload {

    private static final long serialVersionUID = -3311099256178798981L;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        // TODO Auto-generated method stub
        return null;
    }

    public Geometry getGeometry() {
        return null;
    }

    public TransportationMode getMode() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return "street transit link";
    }

    public String getStart() {
        // TODO Auto-generated method stub
        return null;
    }

    public WalkResult walk(State s0, WalkOptions wo) {
        State s1 = s0.clone();
        return new WalkResult(0, s1);
    }

    public WalkResult walkBack(State s0, WalkOptions wo) {
        State s1 = s0.clone();
        return new WalkResult(0, s1);
    }

}
