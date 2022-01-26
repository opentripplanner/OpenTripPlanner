package org.opentripplanner.ext.legacygraphqlapi.model;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;

/**
 * Class that contains a {@link Stop} on a {@link Trip}.
 */
public class LegacyGraphQLStopOnTripModel {

    /**
     * Stop that should be on the trip but technically it's possible that it isn't or that it's
     * null.
     */
    private final StopLocation stop;

    /**
     * Trip that should contain the stop but technically it's possible that the stop isn't on the
     * trip and it's also possible that the trip is null.
     */
    private final Trip trip;

    public LegacyGraphQLStopOnTripModel(StopLocation stop, Trip trip) {
        this.stop = stop;
        this.trip = trip;
    }

    public StopLocation getStop() {
        return stop;
    }

    public Trip getTrip() {
        return trip;
    }
}
