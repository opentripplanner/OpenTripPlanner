package org.opentripplanner.transfer.constrained.model;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A constrained-transfer endpoint as produced by mapping, before it is known whether it needs a
 * stop position within a trip's stop-time sequence. {@link Resolved} wraps a point that needed no
 * such lookup. {@link PendingTripPosition} still needs one - it is resolved by
 * {@link org.opentripplanner.model.impl.TransitDataImportBuilder} once stop times have settled
 * into their final, post-filtering shape - resolving it any earlier risks the position no longer
 * matching the stop pattern the trip's {@code TripPattern} is eventually built from.
 */
public sealed interface DeferredConstrainedTransferPoint {
  record Resolved(TransferPoint point) implements DeferredConstrainedTransferPoint {}

  record PendingTripPosition(
    Trip trip,
    @Nullable RegularStop stop,
    @Nullable Station station,
    boolean boardTrip
  ) implements DeferredConstrainedTransferPoint {}
}
