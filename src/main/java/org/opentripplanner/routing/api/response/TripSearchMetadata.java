package org.opentripplanner.routing.api.response;

import org.opentripplanner.model.base.ToStringBuilder;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Meta-data about the trip search performed.
 */
public class TripSearchMetadata {

    /**
     * This is the time window used by the raptor search. The window is an optional parameter and
     * OTP might override it/dynamically assign a new value.
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


    private TripSearchMetadata(Duration searchWindowUsed, Instant prevDateTime, Instant nextDateTime) {
        this.searchWindowUsed = searchWindowUsed;
        this.nextDateTime = nextDateTime;
        this.prevDateTime = prevDateTime;
    }

    public static TripSearchMetadata createForArriveBy(
        Instant reqTime,
        int searchWindowUsed,
        @Nullable Instant previousTimeInclusive
    ) {
        Instant prevDateTime = previousTimeInclusive == null
                ? reqTime.minusSeconds(searchWindowUsed)
                // Round up to closest minute, to meet the _inclusive_ requirement
                : previousTimeInclusive
                        .minusSeconds(1)
                        .truncatedTo(ChronoUnit.MINUTES)
                        .plusSeconds(60);

        return new TripSearchMetadata(
            Duration.ofSeconds(searchWindowUsed),
            prevDateTime,
            reqTime.plusSeconds(searchWindowUsed)
        );
    }

    public static TripSearchMetadata createForDepartAfter(
        Instant reqTime, int searchWindowUsed, Instant nextDateTimeExcusive
    ) {
        Instant nextDateTime = nextDateTimeExcusive == null
            ? reqTime.plusSeconds(searchWindowUsed)
            : nextDateTimeExcusive.truncatedTo(ChronoUnit.MINUTES);

        return new TripSearchMetadata(
            Duration.ofSeconds(searchWindowUsed),
            reqTime.minusSeconds(searchWindowUsed),
            nextDateTime
        );
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripSearchMetadata.class)
            .addDuration("searchWindowUsed", searchWindowUsed)
            .addObj("nextDateTime", nextDateTime)
            .addObj("prevDateTime", prevDateTime)
            .toString();
    }
}
