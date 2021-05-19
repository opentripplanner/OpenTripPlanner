package org.opentripplanner.routing.core;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * A time restriction for use with edge traversal which limits when an edge may be traversed.
 */
public interface TimeRestriction {
    boolean isTraverseableAt(LocalDateTime now);
    Optional<LocalDateTime> earliestDepartureTime(LocalDateTime now);
    Optional<LocalDateTime> latestArrivalTime(LocalDateTime now);
}
