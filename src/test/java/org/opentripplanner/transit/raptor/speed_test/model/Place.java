package org.opentripplanner.transit.raptor.speed_test.model;

import java.util.Optional;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.WgsCoordinate;

import javax.annotation.Nullable;

/**
 * A Place is where a journey starts or ends, or a transit stop along the way.
 */
public class Place {

    /**
     * For transit stops, the name of the stop.  For points of interest, the name of the POI.
     */
    public final String name;

    public final FeedScopedId stopId;
    public final WgsCoordinate coordinate;

    public Place(String name, String feedId, String stopId, Double lat, Double lon) {
        this(
                name,
                id(feedId, stopId),
                new WgsCoordinate(lat, lon)
        );
    }


    private Place(String name, FeedScopedId stopId, WgsCoordinate coordinate) {
        this.name = name;
        this.stopId = stopId;
        this.coordinate = coordinate;
    }

    public GenericLocation toGenericLocation() {
        return Optional.ofNullable(stopId)
                .map(id -> GenericLocation.fromStopId(name, id.getFeedId(), id.getId()))
                .orElse(new GenericLocation(coordinate.latitude(), coordinate.longitude()));
    }

    @Nullable
    private static FeedScopedId id(String feedId, String id) {
        if (id == null || id.isBlank()) {return null;}
        return new FeedScopedId(feedId, id);
    }
}