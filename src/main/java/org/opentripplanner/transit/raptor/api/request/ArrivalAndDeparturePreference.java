package org.opentripplanner.transit.raptor.api.request;

/**
 * TODO OTP2 - Write doc if implemented
 */
public enum ArrivalAndDeparturePreference {
    /**
     * Find the best results within the time window. The traveler is flexible within the
     * search window. This is the default.
     */
    TIME_TABLE,

    /**
     * The traveler prefer to arrive as early as possible given the earliest departure time.
     */
    ARRIVE_EARLY,

    /**
     * The traveler prefer to depart as late as possible given the latest arrival time.
     */
    DEPART_LATE
}
