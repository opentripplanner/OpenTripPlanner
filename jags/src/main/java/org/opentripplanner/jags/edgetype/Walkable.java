package org.opentripplanner.jags.edgetype;

import java.util.GregorianCalendar;

import com.vividsolutions.jts.geom.Geometry;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;

public interface Walkable {

	public TransportationMode getMode();
	public String getName();
	public String getDirection();
	public Geometry getGeometry();
	public String getStart();
	public String getEnd();
	public double getDistanceKm();
	
    WalkResult walk( State s0, WalkOptions wo );
    WalkResult walkBack( State s0, WalkOptions wo );    
}

