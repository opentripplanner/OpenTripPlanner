package org.opentripplanner.model.routing;

import java.time.Duration;
import java.time.Instant;

/**
 * Meta-data about the trip search performed.
 */
public class TripSearchMetadata {

    /**
     * This is the time window used by the raptor search. The window is an optional parameter and
     * OTP might override it/dynamically assign a new value.
     * <p>
     * Unit : seconds
     */
    public Duration searchWindowUsed;

    /**
     * This is the suggested search time for the "next page" or time window. Insert it together
     * with the {@link #searchWindowUsed} in the request to get a new set of trips following in the
     * time-window AFTER the current search. No duplicate trips should be returned, unless a trip
     * is delayed and new realtime-data is available.
     */
    public Instant nextDateTime;

    /**
     * This is the suggested search time for the "previous page" or time window. Insert it together
     * with the {@link #searchWindowUsed} in the request to get a new set of trips preceding in the
     * time-window BEFORE the current search. No duplicate trips should be returned, unless a trip
     * is delayed and new realtime-data is available.
     */
    public Instant prevDateTime;


    public TripSearchMetadata(Duration searchWindowUsed, Instant prevDateTime, Instant nextDateTime) {
        this.searchWindowUsed = searchWindowUsed;
        this.nextDateTime = nextDateTime;
        this.prevDateTime = prevDateTime;
    }
}
