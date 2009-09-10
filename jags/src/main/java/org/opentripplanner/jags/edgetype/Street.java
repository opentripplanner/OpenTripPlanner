package org.opentripplanner.jags.edgetype;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;

public class Street extends AbstractPayload {

	private static final long serialVersionUID = -3215764532108343102L;
	String name;
    double length;
    
    public Street( double length ) {
        this.length = length;
    }
    
    public Street( String name, double length ) {
        this.name = name;
        this.length = length;
    }
    
    public WalkResult walk( State s0, WalkOptions wo ) {
        State s1 = s0.clone();
        double weight = this.length/wo.speed;
        return new WalkResult( weight, s1 );
    }
    
    public WalkResult walkBack( State s0, WalkOptions wo ) {
        State s1 = s0.clone();
        double weight = this.length/wo.speed;
        return new WalkResult( weight, s1 );
    }
    
    public String toString() {
        return "Street("+this.length+")";
    }
    
}