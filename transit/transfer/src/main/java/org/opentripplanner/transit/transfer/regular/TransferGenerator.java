package org.opentripplanner.transit.transfer.regular;

import org.opentripplanner.core.model.id.FeedScopedId;

import java.util.List;
import java.util.function.Predicate;

///
/// This interface is used in the following use-cases.
///
/// 1. Graph Build - Build transfers for all stops
/// 2. New stop(s) used - Build transfers to/from the new stop for all nearby stops
/// 3. Elevator (or any Vertex) is closed/opened:
///    1. If part of a station - rebuild transfers for all stops in the station
///    2. Else - rebuild transfers for all stops nearby (within half the max transfer duration)
///
/// v1 only implements {@link #generateTransfersForAllStops()} (GraphBuilder use-case) - the
/// updater use-cases below are not supported yet.
///
/// @param <P> the transfer path/template type
///
public interface TransferGenerator<P> {
  /**
   * Typically done at graph build time and serialized to reduce startup time.
   */
  void generateTransfersForAllStops();

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
