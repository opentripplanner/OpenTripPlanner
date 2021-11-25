package org.opentripplanner.model;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;

/**
 * Represents a location that is to be used in a routing request. It can be either a from, to, or
 * intermediate location. This has to be resolved to a vertex or a collection of vertices before
 * routing can start.
 */
public class GenericLocation {

    /**
     * A label for the place, if provided. This is pass-through information and does not affect
     * routing in any way.
     */
    public final String label;

    /**
     * Refers to a specific element in the OTP model. This can currently be a Stop or StopCollection.
     */
    public final FeedScopedId stopId;

    /**
     * Coordinates of the location. These can be used by themselves or as a fallback if placeId is
     * not found.
     */
    public final Double lat;

    public final Double lng;

    public GenericLocation(String label, FeedScopedId stopId, Double lat, Double lng) {
        this.label = label;
        this.stopId = stopId;
        this.lat = lat;
        this.lng = lng;
    }

    public GenericLocation(Double lat, Double lng) {
        this.label = null;
        this.stopId = null;
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * Returns this as a Coordinate object.
     */
    public Coordinate getCoordinate() {
        if (this.lat == null || this.lng == null) {
            return null;
        }
        return new Coordinate(this.lng, this.lat);
    }

    public boolean isSpecified() {
        return stopId != null || (lat != null && lng != null);
    }

    public static GenericLocation fromStopId(String name, String feedId, String stopId) {
        return new GenericLocation(name,
                new FeedScopedId(feedId, stopId), null, null
        );
    }

    @Override
    public String toString() {
        ValueObjectToStringBuilder buf = ValueObjectToStringBuilder.of().skipNull();
        if(label != null && !label.isBlank()) {
            buf.addText(label);
        }
        buf.addObj(stopId);
        buf.addCoordinate(lat, lng);
        return buf.toString();
    }
}
