package org.opentripplanner.transit.transfer.regular;

import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;

///
/// Incrementally (re)generates a profile's regular transfer path templates in response to a
/// runtime change, instead of rebuilding transfers for the whole graph. Not implemented yet - see
/// [opentripplanner/OpenTripPlanner#7074](https://github.com/opentripplanner/OpenTripPlanner/issues/7074).
///
/// This interface covers the following use-cases:
///
/// 1. New stop(s) used - Build transfers to/from the new stop for all nearby stops
/// 2. Elevator (or any Vertex) is closed/opened:
///    1. If part of a station - rebuild transfers for all stops in the station
///    2. Else - rebuild transfers for all stops nearby (within half the max transfer duration)
///
/// @param <P> the transfer path/template type
/// @see TransferGenerator for the graph-build-time counterpart
///
public interface TransferRealtimeUpdater<P> {
  /**
   * Typically done when a new stop is used by a new pattern as part of an extra journey or
   * platform change. Lightweight no-op if the stop already has transfers.
   */
  void updateTransfersForStop(FeedScopedId stopId);

  /**
   * Update all transfers between two stops that both are in the given list of stops, if the
   * given filter predicate is true.
   */
  void updateTransfersForStops(List<FeedScopedId> stops, Predicate<P> filterPaths);
}
