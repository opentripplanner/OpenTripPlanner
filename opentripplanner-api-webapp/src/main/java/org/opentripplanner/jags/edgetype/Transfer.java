package org.opentripplanner.jags.edgetype;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;

import com.vividsolutions.jts.geom.Geometry;

public class Transfer implements Walkable {

	double distance = 0;

	public Transfer(double distance) {
		this.distance = distance;
	}

	public String getDirection() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getDistanceKm() {
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
		// TODO Auto-generated method stub
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

	public WalkResult walk(State s0, WalkOptions wo) {
		if (s0.justTransfered) {
			// do not transfer twice in a row -- you might as well just make the
			// correct transfer the first time.
			return null;
		}
		State s1 = s0.clone();
		s1.justTransfered = true;
		return new WalkResult(distance / wo.speed + wo.transferPenalty, s1);
	}

	public WalkResult walkBack(State s0, WalkOptions wo) {
		if (s0.justTransfered) {
			return null;
		}
		State s1 = s0.clone();
		s1.justTransfered = true;
		return new WalkResult(distance / wo.speed + wo.transferPenalty, s1);
	}

}
