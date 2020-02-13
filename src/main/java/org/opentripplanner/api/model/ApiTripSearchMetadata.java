package org.opentripplanner.api.model;

import org.opentripplanner.model.routing.TripSearchMetadata;

/**
 * Meta-data about the trip search performed.
 */
public class ApiTripSearchMetadata {

    /**
     * This is the time window used by the raptor search. The window is an optional parameter and
     * OTP might override it/dynamically assign a new value.
     * <p>
     * Unit : seconds
     */
    public int searchWindowUsed;

    /**
     * This is the suggested search time for the "next page" or time window. Insert it together
     * with the {@link #searchWindowUsed} in the request to get a new set of trips following in the
     * time-window AFTER the current search. No duplicate trips should be returned, unless a trip
     * is delayed and new realtime-data is available.
     * <p>
     * If OTP for some reason is not able to calculate this value then it will be {@code null}.
     * <p>
     * Unit : epoch milliseconds
     */
    public Long nextDateTime;

    /**
     * This is the suggested search time for the "previous page" or time window. Insert it together
     * with the {@link #searchWindowUsed} in the request to get a new set of trips preceding in the
     * time-window BEFORE the current search. No duplicate trips should be returned, unless a trip
     * is delayed and new realtime-data is available.
     * <p>
     * If OTP for some reason is not able to calculate this value then it will be {@code null}.
     * <p>
     * Unit : epoch milliseconds
     */
    public Long prevDateTime;
}
