package org.opentripplanner.transit.transfer.regular;

import java.util.List;
import java.util.Set;
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
/// TODO: This is not implemented yet, see the module package.md.
///
/// @param <P> the street path type
/// @see RegularTransferGenerator for the graph-build-time counterpart
///
public interface RegularTransferRealtimeUpdater<P> {
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

  /**
   * Close the given facilities, and open all others. A path is not used as a transfer if one
   * of its {@code TransferPath#closableFacilityIds()} is in the set; the cheapest alternative path
   * without a closed facility is used instead, and the pair has no transfer if there is none.
   * <p>
   * The set is the complete state: every facility that is not in the set is open. Calling the method
   * twice with the same set changes nothing (idempotent). The updater never sends a delta. The
   * implementation compares the set with the previous one and only updates the stops that have a
   * path with a changed id. An empty set opens everything. An id that no path has is kept and
   * ignored. A facility can be an elevator or any other part of the street network that can
   * be closed, the transfer module only sees opaque ids.
   * <p>
   * Must be called on the writer thread, inside a transaction. See design F in the
   * {@code _thoughts/elevator-status-runtime} notes.
   *
   * TODO: Not implemented yet.
   *
   * @param closedFacilityIds the ids of all closed facilities, not {@code null}. The set is copied, not retained.
   */
  void closeFacilities(Set<String> closedFacilityIds);
}
