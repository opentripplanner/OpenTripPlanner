package org.opentripplanner.routing.core;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Data;

/**
 * A {@link TimeRestriction} which has a time span. This may be used when a time restriction must be
 * true for a longer period of time. For example when using when parking a car, the VehicleParking
 * must be open from entering until exiting.
 */
@Data(staticConstructor = "of")
public class TimeRestrictionWithTimeSpan implements TimeRestriction {

    private final TimeRestriction timeRestriction;
    private final int spanInSeconds;

    @Override
    public boolean isTraverseableAt(LocalDateTime now) {
        return timeRestriction.isTraverseableAt(now)
                && timeRestriction.isTraverseableAt(now.plusSeconds(spanInSeconds));
    }

    @Override
    public Optional<LocalDateTime> earliestDepartureTime(LocalDateTime now) {
        var time = now;
        do {
            var next = timeRestriction.earliestDepartureTime(time);
            if (next.isEmpty()) {
                return Optional.empty();
            }

            time = next.get();
            var timeAtEnd = time.plusSeconds(spanInSeconds);
            if (timeRestriction.isTraverseableAt(timeAtEnd)) {
                return next;
            } else {
                time = timeAtEnd;
            }
        } while (true);
    }

    @Override
    public Optional<LocalDateTime> latestArrivalTime(LocalDateTime now) {
        var time = now;
        do {
            var previous = timeRestriction.latestArrivalTime(time);
            if (previous.isEmpty()) {
                return Optional.empty();
            }

            time = previous.get();
            var timeAtEnd = time.plusSeconds(spanInSeconds);
            if (timeRestriction.isTraverseableAt(timeAtEnd)) {
                return previous;
            } else {
                time = time.minusSeconds(spanInSeconds);
            }
        } while (true);
    }
}
