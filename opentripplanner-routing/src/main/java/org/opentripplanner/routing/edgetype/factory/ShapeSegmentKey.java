/**
 * 
 */
package org.opentripplanner.routing.edgetype.factory;

import org.onebusaway.gtfs.model.AgencyAndId;

class ShapeSegmentKey {
    private AgencyAndId shapeId;

    private double shapeDistanceFrom;

    private double shapeDistanceTo;

    public ShapeSegmentKey(AgencyAndId shapeId, double shapeDistanceFrom, double shapeDistanceTo) {
        this.shapeId = shapeId;
        this.shapeDistanceFrom = shapeDistanceFrom;
        this.shapeDistanceTo = shapeDistanceTo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(shapeDistanceFrom);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(shapeDistanceTo);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((shapeId == null) ? 0 : shapeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ShapeSegmentKey other = (ShapeSegmentKey) obj;
        if (Double.doubleToLongBits(shapeDistanceFrom) != Double
                .doubleToLongBits(other.shapeDistanceFrom))
            return false;
        if (Double.doubleToLongBits(shapeDistanceTo) != Double
                .doubleToLongBits(other.shapeDistanceTo))
            return false;
        if (shapeId == null) {
            if (other.shapeId != null)
                return false;
        } else if (!shapeId.equals(other.shapeId))
            return false;
        return true;
    }

}