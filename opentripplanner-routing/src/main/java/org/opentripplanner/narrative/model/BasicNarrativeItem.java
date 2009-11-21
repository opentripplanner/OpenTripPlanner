package org.opentripplanner.narrative.model;


import com.vividsolutions.jts.geom.Geometry;

class BasicNarrativeItem implements NarrativeItem {
    private String name;

    private String direction;

    private Geometry geometry;

    private String start;

    private String end;

    private double distance;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDirection() {
        return direction;
    }

    public String getTowards() {
        return null;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void addGeometry(Geometry geometry) {
        this.geometry = this.geometry.union(geometry);
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public double getDistanceKm() {
        return distance;
    }
}