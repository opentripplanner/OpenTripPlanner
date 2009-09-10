package org.opentripplanner.jags.spt;

import org.opentripplanner.jags.core.AbstractEdge;
import org.opentripplanner.jags.edgetype.Walkable;




public class SPTEdge extends AbstractEdge{
    public SPTVertex fromv;
    public SPTVertex tov;
    public Walkable payload;
    
    SPTEdge( SPTVertex fromv, SPTVertex tov, Walkable ep ) {
        this.fromv = fromv;
        this.tov = tov;
        this.payload = ep;
    }
}