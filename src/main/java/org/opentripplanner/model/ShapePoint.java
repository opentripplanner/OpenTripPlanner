/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.model;

public final class ShapePoint extends IdentityBean<Integer> implements Comparable<ShapePoint> {

    private static final long serialVersionUID = 1L;

    private static final double MISSING_VALUE = -999;

    private int id;

    private AgencyAndId shapeId;

    private int sequence;

    private double lat;

    private double lon;

    private double distTraveled = MISSING_VALUE;

    public ShapePoint() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AgencyAndId getShapeId() {
        return shapeId;
    }

    public void setShapeId(AgencyAndId shapeId) {
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
    public String toString() {
        return "<ShapePoint " + getShapeId() + " #" + getSequence() + " (" + getLat() + ","
                + getLon() + ")>";
    }

    @Override
    public int compareTo(ShapePoint o) {
        return this.getSequence() - o.getSequence();
    }
}
