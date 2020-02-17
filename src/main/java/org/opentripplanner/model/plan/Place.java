package org.opentripplanner.model.plan;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.Constants;
import org.opentripplanner.util.CoordinateUtils;

import java.util.Calendar;

/** 
* A Place is where a journey starts or ends, or a transit stop along the way.
*/ 
public class Place {

    /** 
     * For transit stops, the name of the stop.  For points of interest, the name of the POI.
     */
    public String name = null;

    /** 
     * The ID of the stop. This is often something that users don't care about.
     */
    public FeedScopedId stopId = null;

    /** 
     * The "code" of the stop. Depending on the transit agency, this is often
     * something that users care about.
     */
    public String stopCode = null;

    /**
      * The code or name identifying the quay/platform the vehicle will arrive at or depart from
      *
    */
    public String platformCode = null;

    /**
     * The longitude of the place.
     */
    public Double lon = null;
    
    /**
     * The latitude of the place.
     */
    public Double lat = null;

    /**
     * The time the rider will arrive at the place.
     */
    public Calendar arrival = null;

    /**
     * The time the rider will depart the place.
     */
    public Calendar departure = null;
    public String orig;
    public String zoneId;

    /**
     * For transit trips, the stop index (numbered from zero from the start of the trip
     */
    public Integer stopIndex;

    /**
     * For transit trips, the sequence number of the stop. Per GTFS, these numbers are increasing.
     */
    public Integer stopSequence;

    /**
     * Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit stop)
     * Mostly used for better localization of bike sharing and P+R station names
     */
    public VertexType vertexType;

    /**
     * In case the vertex is of type Bike sharing station.
     */
    public String bikeShareId;

    public Place() { }

    public Place(Double lon, Double lat, String name) {
        this.lon = lon;
        this.lat = lat;
        this.name = name;
        this.vertexType = VertexType.NORMAL;
    }

    public Place(Double lon, Double lat, String name, Calendar arrival, Calendar departure) {
        this(lon, lat, name);
        this.arrival = arrival;
        this.departure = departure;
    }

    /**
     * Returns the geometry in GeoJSON format
     */
    String getGeometry() {
        return Constants.GEO_JSON_POINT + lon + "," + lat + Constants.GEO_JSON_TAIL;
    }

    /**
     * Test if the place is likely to be at the same location. First check the coordinates
     * then check the stopId [if it exist].
     */
    public boolean sameLocation(Place other) {
        if(this == other) { return true; }
        if(lat != null && lon != null && other.lat != null && other.lon != null) {
            return CoordinateUtils.compare(lat, lon, other.lat, other.lon);
        }
        return stopId != null && stopId.equals(other.stopId);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toStringAdd(sb, "name", name);
        toStringAdd(sb, "stopId", stopId);
        toStringAdd(sb, "stopCode", stopCode);
        toStringAdd(sb, "platformCode", platformCode);
        toStringAdd(sb, "lon", lon);
        toStringAdd(sb, "lat", lat);
        toStringAdd(sb, "arrival", arrival);
        toStringAdd(sb, "departure", departure);
        toStringAdd(sb, "orig", orig);
        toStringAdd(sb, "zoneId", zoneId);
        toStringAdd(sb, "stopIndex", stopIndex);
        toStringAdd(sb, "stopSequence", stopSequence);
        toStringAdd(sb, "vertexType", vertexType);
        toStringAdd(sb, "bikeShareId", bikeShareId);

        return "Place{" + (sb.length() > 0 ? sb.substring(2) : "") + "}";
    }
    private static void toStringAdd(StringBuilder sb, String name, String value) {
        if(value == null) { return; }
        sb.append(", ").append(name).append("='").append(value).append("'");
    }
    private static void toStringAdd(StringBuilder sb, String name, Object value) {
        if(value == null) { return; }
        sb.append(", ").append(name).append("=").append(value);
    }

}
