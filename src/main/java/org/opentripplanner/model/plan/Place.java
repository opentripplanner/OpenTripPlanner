package org.opentripplanner.model.plan;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.base.ToStringBuilder;

/** 
* A Place is where a journey starts or ends, or a transit stop along the way.
*/ 
public class Place {

    /** 
     * For transit stops, the name of the stop.  For points of interest, the name of the POI.
     */
    public final String name;

    /**
     * Reference to the stop.
     */
    public StopLocation stop;

    /**
     * The coordinate of the place.
     */
    public final WgsCoordinate coordinate;

    public String orig;

    /**
     * For transit trips, the stop index (numbered from zero from the start of the trip).
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
    public FeedScopedId bikeShareId;

    public Place(Double lat, Double lon, String name) {
        this.name = name;
        this.vertexType = VertexType.NORMAL;
        this.coordinate = WgsCoordinate.creatOptionalCoordinate(lat, lon);
    }

    public Place(Stop stop) {
        this.name = stop.getName();
        this.stop = stop;
        this.coordinate = stop.getCoordinate();
        this.vertexType = VertexType.TRANSIT;
    }

    /**
     * Test if the place is likely to be at the same location. First check the coordinates
     * then check the stopId [if it exist].
     */
    public boolean sameLocation(Place other) {
        if(this == other) { return true; }
        if(coordinate != null) {
            return coordinate.sameLocation(other.coordinate);
        }
        return stop != null && stop.equals(other.stop);
    }

    /**
     * Return a short versio to be used in other classes toStringMethods. Should return
     * just the necessary information for a human to identify the place in a given the context.
     */
    public String toStringShort() {
        StringBuilder buf = new StringBuilder(name);
        if(stop != null) {
            buf.append(" (").append(stop.getId()).append(")");
        } else {
            buf.append(" ").append(coordinate.toString());
        }

        return buf.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(Place.class)
                .addStr("name", name)
                .addObj("stop", stop)
                .addObj("coordinate", coordinate)
                .addStr("orig", orig)
                .addNum("stopIndex", stopIndex)
                .addNum("stopSequence", stopSequence)
                .addEnum("vertexType", vertexType)
                .addObj("bikeShareId", bikeShareId)
                .toString();
    }
}
