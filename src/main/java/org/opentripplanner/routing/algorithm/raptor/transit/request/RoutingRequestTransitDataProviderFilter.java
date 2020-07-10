package org.opentripplanner.routing.algorithm.raptor.transit.request;

import lombok.RequiredArgsConstructor;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Set;

@RequiredArgsConstructor
public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

    private final boolean requireBikesAllowed;
    private final boolean requireWheelchairAccessible;
    private final Set<TransitMode> transitModes;
    private final Set<FeedScopedId> bannedRoutes;

    public RoutingRequestTransitDataProviderFilter(RoutingRequest request) {
        this(
                request.modes.directMode == StreetMode.BIKE,
                request.wheelchairAccessible,
                request.modes.transitModes,
                request.rctx.bannedRoutes
        );
    }

    @Override
    public <T extends TripPatternForDate> boolean tripPatternPredicate(T tripPatternForDate) {
        return routeIsNotBanned(tripPatternForDate) && transitModeIsAllowed(tripPatternForDate);
    }

    @Override public <T extends TripTimes> boolean tripTimesPredicate(T tripTimes) {
        if (requireBikesAllowed) {
            return BikeAccess.fromTrip(tripTimes.trip) == BikeAccess.ALLOWED;
        }

        if (requireWheelchairAccessible) {
            return tripTimes.trip.getWheelchairAccessible() == 1;
        }

        return true;
    }

    private <T extends TripPatternForDate> boolean routeIsNotBanned(T tripPatternForDate) {
        FeedScopedId routeId = tripPatternForDate.getTripPattern().getPattern().route.getId();
        return !bannedRoutes.contains(routeId);
    }

    private <T extends TripPatternForDate> boolean transitModeIsAllowed(T tripPatternForDate) {
        TransitMode transitMode = tripPatternForDate.getTripPattern().getTransitMode();
        return transitModes.contains(transitMode);
    }
}
