package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

import com.vividsolutions.jts.geom.Geometry;

public class SPTEdge extends AbstractEdge {
    public SPTVertex fromv;

    public SPTVertex tov;

    public Edge payload;

    SPTEdge(SPTVertex fromv, SPTVertex tov, Edge ep) {
        super(fromv, tov);
        this.fromv = fromv;
        this.tov = tov;
        this.payload = ep;
    }

    @Override
    public String getDirection() {
        return payload.getDirection();
    }

    @Override
    public double getDistance() {
        return payload.getDistance();
    }

    @Override
    public String getEnd() {
        return payload.getEnd();
    }

    @Override
    public Geometry getGeometry() {
        return payload.getGeometry();
    }

    @Override
    public TransportationMode getMode() {
        return payload.getMode();
    }

    @Override
    public String getName() {
        return payload.getName();
    }

    @Override
    public String getStart() {
        return payload.getStart();
    }

    @Override
    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException {
        return payload.traverse(s0, wo);
    }

    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException {
        return payload.traverseBack(s0, wo);
    }
}