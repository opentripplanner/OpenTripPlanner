package org.opentripplanner.transit.raptor.speed_test.model.testcase;

import java.util.Optional;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.WgsCoordinate;

import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;

/**
 * A Place is where a journey starts or ends, or a transit stop along the way.
 */
public record Place(
        /**
         * For transit stops, the name of the stop.  For points of interest, the name of the POI.
         */
        String name,
        FeedScopedId stopId,
        WgsCoordinate coordinate
) {

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

    @Override
    public String toString() {
        return ToStringBuilder.of(Place.class)
                .addStr("name", name)
                .addObj("stopId", stopId)
                .addObj("coordinate", coordinate)
                .toString();
    }
}