package org.opentripplanner.jags.core;

import com.vividsolutions.jts.geom.Coordinate;

public class SpatialVertex extends Vertex implements Locatable {
	private static final long serialVersionUID = 7784891800734704432L;
	double x,y;
	
	public SpatialVertex(String label, double x, double y) {
		super(label);
		this.x = x;
		this.y = y;
	}

	public double distance(double x, double y) {
		return Math.pow((Math.pow(this.x-x,2)+Math.pow(this.y-y,2)),0.5);
	}
	
	public Coordinate getCoordinate() {
		return new Coordinate(x, y);
	}
}
