package org.opentripplanner.routing.edgetype;

import java.time.Duration;
import java.time.LocalDateTime;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TimeRestriction;
import org.opentripplanner.routing.core.TimeRestrictionWithOffset;

public interface TimeRestrictedEdge {

    default boolean isTraversalBlockedByTimeRestriction(State s0, boolean allowWaiting, TimeRestriction timeRestriction) {
        final var options = s0.getOptions();
        if (options.ignoreAndCollectTimeRestrictions || timeRestriction == null) {
            return false;
        }

        return isTraversalBlockedByTimeRestriction(s0, s0.getTimeAsLocalDateTime(), allowWaiting, timeRestriction);
    }

    default boolean isTraversalBlockedByTimeRestriction(State s0, LocalDateTime now, boolean allowWaiting, TimeRestriction timeRestriction) {
        final var options = s0.getOptions();
        if (options.ignoreAndCollectTimeRestrictions) {
            return false;
        }
        else {
            if (allowWaiting) {
                var altTime = options.arriveBy
                                ? timeRestriction.latestArrivalTime(now)
                                : timeRestriction.earliestDepartureTime(now);
                return altTime.isEmpty();
            }
            else {
                return !timeRestriction.isTraverseableAt(now);
            }
        }
    }

    /**
     * Add a TimeRestriction using the {@link StateEditor#getElapsedTimeSeconds()} as the offset.
     */
    default void updateEditorWithTimeRestriction(
            State s0,
            StateEditor s1,
            TimeRestriction timeRestriction,
            Object source
    ) {
        updateEditorWithTimeRestriction(s0, s1, s1.getElapsedTimeSeconds(), timeRestriction, source);
    }

    default void updateEditorWithTimeRestriction(
            State s0,
            StateEditor s1,
            long offset,
            TimeRestriction timeRestriction,
            Object source
    ) {
        if (timeRestriction == null) {
            return;
        }

        if (s0.getOptions().ignoreAndCollectTimeRestrictions) {
            s1.addTimeRestriction(TimeRestrictionWithOffset.of(timeRestriction, offset), source);
        }
        else {
            var zoneId = s0.getOptions().rctx.graph.getTimeZone().toZoneId();
            var now = s1.getZonedDateTime();
            var time = s0.getOptions().arriveBy
                    ? timeRestriction.latestArrivalTime(now.toLocalDateTime())
                    : timeRestriction.earliestDepartureTime(now.toLocalDateTime());
            if (time.isPresent()) {
                var waitTime =
                        (int) Math.abs(Duration.between(now, time.get().atZone(zoneId)).getSeconds());
                s1.incrementWeight(waitTime * s0.getOptions().waitReluctance);
                s1.incrementTimeInSeconds(waitTime);
            }
            else {
                throw new IllegalStateException("Missing traversal time!");
            }
        }
    }
}
