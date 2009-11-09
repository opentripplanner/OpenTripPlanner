package org.opentripplanner.routing.edgetype;

public class DrawablePoint {
    public float x;

    public float y;

    public float z;

    public DrawablePoint(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String toString() {
        return "(" + x + " " + y + " " + z + ")";
    }
}
