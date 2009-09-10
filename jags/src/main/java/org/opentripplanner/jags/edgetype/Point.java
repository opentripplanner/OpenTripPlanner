package org.opentripplanner.jags.edgetype;

public class Point {
	public float x;
	public float y;
	public float z;
	
	public Point(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public String toString() {
		return "("+x+" "+y+" "+z+")";
	}
}
