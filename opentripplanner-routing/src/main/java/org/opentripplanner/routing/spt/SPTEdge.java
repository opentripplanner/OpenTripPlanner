package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.edgetype.Traversable;

public class SPTEdge extends AbstractEdge {
    public SPTVertex fromv;

    public SPTVertex tov;

    public Traversable payload;

    SPTEdge(SPTVertex fromv, SPTVertex tov, Traversable ep) {
        this.fromv = fromv;
        this.tov = tov;
        this.payload = ep;
    }
}