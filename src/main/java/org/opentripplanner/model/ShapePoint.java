/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.io.Serializable;
import java.util.Objects;

public final class ShapePoint implements Serializable, Comparable<ShapePoint> {

    private static final long serialVersionUID = 1L;

    private static final double MISSING_VALUE = -999;

    private FeedScopedId shapeId;

    private int sequence;

    private double lat;

    private double lon;

    private double distTraveled = MISSING_VALUE;

    public ShapePoint() {
    }

    public FeedScopedId getShapeId() {
        return shapeId;
    }

    public void setShapeId(FeedScopedId shapeId) {
        this.shapeId = shapeId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public boolean isDistTraveledSet() {
        return distTraveled != MISSING_VALUE;
    }

    /**
     * @return the distance traveled along the shape path. If no distance was
     *         specified, the value is undefined. Check first with
     *         {@link #isDistTraveledSet()}
     */
    public double getDistTraveled() {
        return distTraveled;
    }

    public void setDistTraveled(double distTraveled) {
        this.distTraveled = distTraveled;
    }

    public void clearDistTraveled() {
        this.distTraveled = MISSING_VALUE;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ShapePoint that = (ShapePoint) o;
        return sequence == that.sequence && Objects.equals(shapeId, that.shapeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shapeId, sequence);
    }

    @Override
    public String toString() {
        return "<ShapePoint " + getShapeId() + " #" + getSequence() + " (" + getLat() + ","
                + getLon() + ")>";
    }

    @Override
    public int compareTo(ShapePoint o) {
        return this.getSequence() - o.getSequence();
    }
}
