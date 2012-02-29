package org.opentripplanner.graph_builder.impl.extra_elevation_data;

public class ElevationPoint implements Comparable<ElevationPoint> {
    public double distanceAlongShape, ele;

    public ElevationPoint(double distance, double ele) {
        this.distanceAlongShape = distance;
        this.ele = ele;
    }

    public ElevationPoint fromBack(double length) {
        return new ElevationPoint(length - distanceAlongShape, ele);
    }

    @Override
    public int compareTo(ElevationPoint arg0) {
        return (int) Math.signum(distanceAlongShape - arg0.distanceAlongShape);
    }
    
    public String toString() {
        return "ElevationPoint(" + distanceAlongShape + ", " + ele + ")";
    }

}
