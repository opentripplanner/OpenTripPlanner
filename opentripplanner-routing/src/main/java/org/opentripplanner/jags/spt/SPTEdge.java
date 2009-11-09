package org.opentripplanner.jags.spt;

import org.opentripplanner.jags.core.AbstractEdge;
import org.opentripplanner.jags.edgetype.Traversable;

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