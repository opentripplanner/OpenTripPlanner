package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * This class computes the days that should be searched in addition to the search date time.
 *
 * For example, if you want to arrive at 0:15 you also need to take the timetable from the previous
 * day into account as your train could arrive at the destination at 23:59.
 *
 * It's similar when you start at 23:59 where you want to take the next day into account, but not
 * the previous one.
 *
 * Another case are trips that take more than 24 hours, where you want to search more than 1 day
 * into the future.
 *
 * Since searching through an entire day's schedule is a little expensive it's best to keep the
 * number of additional days as low as possible.
 */
public class AdditionalSearchDays {

    private final boolean arriveBy;
    private final ZonedDateTime searchDateTime;
    private final Optional<Duration> searchWindow;
    private final Duration maxSearchWindow;
    private final Duration maxJourneyDuration;

    public AdditionalSearchDays(
            boolean arriveBy,
            ZonedDateTime searchDateTime,
            Duration searchWindow,
            Duration maxSearchWindow,
            Duration maxJourneyDuration
    ) {
        this.arriveBy = arriveBy;
        this.searchDateTime = searchDateTime;
        this.searchWindow = Optional.ofNullable(searchWindow);
        this.maxSearchWindow = maxSearchWindow;
        this.maxJourneyDuration = maxJourneyDuration;
    }


    /**
     * How many days that are prior to the search date time should be searched for transit.
     */
    public int additionalSearchDaysInPast() {
        if(arriveBy) {
            var sw = getMaximumSearchWindow();
            var earliestStart= searchDateTime.minus(maxJourneyDuration.plus(sw));
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
            var lastArrival = requestTime.plus(maxJourneyDuration.plus(sw));
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
