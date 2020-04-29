package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.model.FeedScopedId;

import java.util.Objects;

public class Place {
    private final double lat;
    private final double lon;
    private final String description;
    private final FeedScopedId stopId;

    public Place(String description, String feedId, String stopId, double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        this.description = description;
        this.stopId = isBlank(stopId) ? null : new FeedScopedId(feedId, stopId);
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getDescription() {
        return description;
    }

    public FeedScopedId getStopId() {
        return stopId;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(lon, lat);
    }

    public String coordinateAsString() {
        return String.format("(%.3f, %.3f)", lat, lon);
    }

    @Override
    public String toString() {
        return String.format("Place<stop=%s, name=%s %s>", stopId, description, coordinateAsString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Double.compare(place.lat, lat) == 0 &&
                Double.compare(place.lon, lon) == 0 &&
                Objects.equals(stopId, place.stopId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon, stopId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
