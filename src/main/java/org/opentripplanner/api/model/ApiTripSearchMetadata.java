package org.opentripplanner.api.model;

/**
 * Meta-data about the trip search performed.
 */
public class ApiTripSearchMetadata {

    /**
     * This is the time window used by the raptor search. The window is an optional parameter and
     * OTP might override it/dynamically assign a new value.
     * <p>
     * Note! The `searchWindowUsed` is NOT adjusted in the post-search-filtering. If the
     * {@code numOfItineraries} request parameter is set, optimal itineraries are removed from the
     * end of the result. Be aware of this when adding the results of more than on search together.
     * If the client support paging/scrolling, do not use the {@code numOfItineraries} parameter,
     * cache or hide the last part of the returned list of itineraries instead.
     * <p>
     * Unit : seconds
     */
    public int searchWindowUsed;

    /**
     * This is the suggested search time for the "next page" or time-window. Insert it together
     * with the {@link #searchWindowUsed} in the request to get a new set of trips following in the
     * time-window AFTER the current search. No duplicate trips should be returned, unless a trip
     * is delayed and new realtime-data is available.
     * <p>
     * Be careful to use paging/scrolling with the {@code numOfItineraries} parameter set. It is
     * safe to scroll forward when the {@code arriveBy=false}, but not if {@code arriveBy=true}. If
     * you need to find the trips that arrive immediately AFTER the latest-arrival-time, be sure
     * NOT to set the {@code numOfItineraries} request parameter in the next request.
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
     * Be careful to use paging/scrolling with the {@code numOfItineraries} parameter set. It is
     * safe to scroll backward when the {@code arriveBy=true}, but not if {@code arriveBy=false}.
     * If you need to find the trips that depart immediately BEFORE the earliest-departure-time, be
     * sure NOT to set the {@code numOfItineraries} request parameter in the next request.
     * <p>
     * If OTP for some reason is not able to calculate this value then it will be {@code null}.
     * <p>
     * Unit : epoch milliseconds
     */
    public Long prevDateTime;
}
