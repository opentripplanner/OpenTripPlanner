package org.opentripplanner.transit.raptor.speed_test.model;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.WgsCoordinate;

/**
 * A Place is where a journey starts or ends, or a transit stop along the way.
 */
public class Place {

    /** For transit stops, the name of the stop.  For points of interest, the name of the POI. */
    public final String name;

    public final FeedScopedId stopId;
    public final WgsCoordinate coordinate;

    /** This is the stop index in the RaptorTransitData */
    public final int rrStopIndex;

    public Place(org.opentripplanner.model.Stop stop, int rrStopIndex) {
        this(stop.getName(), stop.getId(), stop.getCoordinate(), rrStopIndex);
    }

    public Place(String name, String feedId, String stopId, Double lat, Double lon) {
        this(name, new FeedScopedId(feedId, stopId), new WgsCoordinate(lat, lon), -1);
    }


    private Place(String name, FeedScopedId stopId, WgsCoordinate coordinate, int rrStopIndex) {
        this.name = name;
        this.stopId = stopId;
        this.coordinate = coordinate;
        this.rrStopIndex = rrStopIndex;
    }
}
