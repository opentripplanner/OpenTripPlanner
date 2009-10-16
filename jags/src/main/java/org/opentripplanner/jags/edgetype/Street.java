package org.opentripplanner.jags.edgetype;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Street extends AbstractPayload {

	private static final long serialVersionUID = -3215764532108343102L;
	private static final String[] DIRECTIONS = { "north", "northeast", "east",
			"southeast", "south", "southwest", "west", "northwest" };
	String id;
	String name;
	Geometry geometry;
	double length;

	public Street(double length) {
		this.length = length;
	}

	public Street(String id, String name, double length) {
		this.id = id;
		this.name = name;
		this.length = length;
	}

	public void setGeometry(Geometry g) {
		geometry = g;
	}

	public WalkResult walk(State s0, WalkOptions wo) {
		State s1 = s0.clone();
		double weight = this.length / wo.speed;
		return new WalkResult(weight, s1);
	}

	public WalkResult walkBack(State s0, WalkOptions wo) {
		State s1 = s0.clone();
		double weight = this.length / wo.speed;
		return new WalkResult(weight, s1);
	}

	public String toString() {
		if (this.name != null) {
			return "Street(" + this.id + ", " + this.name + ", " + this.length
					+ ")";
		} else {
			return "Street(" + this.length + ")";
		}
	}

	public String getDirection() {
		Coordinate[] coordinates = geometry.getCoordinates();
		double run = coordinates[1].x - coordinates[0].x;
		double rise = coordinates[1].y - coordinates[0].y;
		double direction = Math.atan2(run, -rise);
		int octant = (int) (8 + Math.round(direction * 8 / (Math.PI * 2))) % 8;

		return DIRECTIONS[octant];
	}

	public double getDistanceKm() {
		return length;
	}

	public String getEnd() {
		// TODO Auto-generated method stub
		return null;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public TransportationMode getMode() {
		// this is actually WALK or BICYCLE depending on the walkoptions
		return TransportationMode.WALK;
	}

	public String getName() {
		return name;
	}

	public String getStart() {
		// TODO Auto-generated method stub
		return null;
	}

}