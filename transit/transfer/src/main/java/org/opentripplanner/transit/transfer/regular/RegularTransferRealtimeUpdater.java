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
   * Close the given facilities, and open all others. A path that depends on a closed facility,
   * see {@code TransferPath#closableFacilityIds()}, is not used as a transfer: the cheapest
   * alternative path without a closed facility is used instead, and there is no transfer between
   * the two stops if there is none.
   * <p>
   * The set is the complete state: every facility that is not in it is open, and an empty set
   * opens all facilities. Calling the method again with the same set changes nothing. An id that
   * no path depends on is ignored. A facility can be an elevator or any other part of the street
   * network that can be closed; the ids are opaque.
   * <p>
   * Must be called on the writer thread, inside a transaction.
   *
   * @param closedFacilityIds the ids of all closed facilities. The set is copied, not retained.
   */
  void closeFacilities(Set<String> closedFacilityIds);
}
