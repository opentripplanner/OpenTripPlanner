package org.opentripplanner.routing.algorithm.raptor.router;

import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Optional;

public class AdditionalSearchDays {

    private final boolean arriveBy;
    private final ZonedDateTime searchDateTime;
    private final Optional<Duration> searchWindow;
    private final Duration maxSearchWindow;
    private final Duration maxJourneyTime;

    public AdditionalSearchDays(
            boolean arriveBy,
            ZonedDateTime searchDateTime,
            Duration searchWindow,
            Duration maxSearchWindow,
            Duration maxJourneyTime
    ) {
        this.arriveBy = arriveBy;
        this.searchDateTime = searchDateTime;
        this.searchWindow = Optional.ofNullable(searchWindow);
        this.maxSearchWindow = maxSearchWindow;
        this.maxJourneyTime = maxJourneyTime;
    }


    /**
     * How many days that are prior to the search date time should be searched for transit.
     */
    public int additionalSearchDaysInPast() {
        if(arriveBy) {
            var sw = getMaximumSearchWindow();
            var earliestStart= searchDateTime.minus(maxJourneyTime.plus(sw));
            return daysInBetween(searchDateTime, earliestStart);
        } else {
            return 0;
        }
    }

    /**
     * How many days that are after to the search date time should be searched for transit.
     */
    public int additionalSearchDaysInFuture() {
        if(arriveBy) {
            return 0;
        } else {
            var sw = getMaximumSearchWindow();
            var requestTime = searchDateTime;
            var lastArrival = requestTime.plus(maxJourneyTime.plus(sw));
            return daysInBetween(requestTime, lastArrival);
        }
    }

    private Duration getMaximumSearchWindow() {
        return searchWindow.orElse(maxSearchWindow);
    }

    private int daysInBetween(ZonedDateTime requestTime, ZonedDateTime earliestStart) {
        return Math.abs(Period.between(requestTime.toLocalDate(), earliestStart.toLocalDate()).getDays());
    }

    public static AdditionalSearchDays defaults(ZonedDateTime time) {
        return new AdditionalSearchDays(
                false,
                time,
                Duration.ofHours(6),
                Duration.ofDays(1),
                Duration.ofDays(1)
        );
    }

}
