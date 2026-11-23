package org.opentripplanner.updater.trip.stoplistdiff;

import java.util.OptionalLong;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A stop in one of the lists compared by {@link StopListDiff}.
 * <p>
 * The times are used to resolve ambiguous alignments and to order removed and added stops. Times
 * are compared like for like: arrival with arrival, or departure with departure if not both stops
 * have an arrival. The unit does not matter as long as it is the same for both lists.
 *
 * @param arrivalTime   the scheduled arrival time of the stop. Empty if not known.
 * @param departureTime the scheduled departure time of the stop. Empty if not known.
 */
public record StopListDiffComparand(
  StopLocation stop,
  OptionalLong arrivalTime,
  OptionalLong departureTime
) {}
