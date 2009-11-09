package org.opentripplanner.jags.core;

import java.io.Serializable;

import org.opentripplanner.jags.edgetype.Walkable;
import org.opentripplanner.jags.gtfs.exception.NegativeWeightException;


public class Edge extends AbstractEdge implements Serializable{
	private static final long serialVersionUID = 2847531383395983317L;
	public Vertex fromv;
    public Vertex tov;
    public Walkable payload;
    
    Edge( Vertex fromv, Vertex tov, Walkable payload ) {
        this.fromv = fromv;
        this.tov = tov;
        this.payload = payload;
    }
    
    public WalkResult walk(State s0, WalkOptions wo) throws NegativeWeightException {
    	WalkResult wr = this.payload.walk( s0, wo );
        return wr;
    }
    
    public WalkResult walkBack(State s0, WalkOptions wo) throws NegativeWeightException{
    	WalkResult wr = this.payload.walkBack( s0, wo );
        return wr;
    }
    
    public String toString() {
        return fromv.label + " -"+payload.toString()+"-> " + tov.label;
    }
}