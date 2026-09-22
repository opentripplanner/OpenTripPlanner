package org.opentripplanner.raptor.rangeraptor.internalapi;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Holds all on-board trip access arrivals for a single route, indexed by the stop position in the
 * route's stop pattern where the ride must be injected. This is not the same as the board
 * position. For example for a pass-through stop, the board position is where the route was
 * boarded, but the ride is not injected into the algorithm before the pass-through stop. This
 * prevents alighting the trip before the pass-through stop is visited.
 *
 * <p>An on-board trip access represents a passenger who is already on board a vehicle at the
 * start of the search — i.e., their "access leg" is the ride itself rather than a walk to a
 * stop. Each such arrival carries a {@code RaptorTripScheduleStopPosition}
 * that identifies exactly which route, trip, and stop position the passenger must board from.
 */
public final class OnTripAccessArrivals<T extends RaptorTripSchedule> {

  private final TIntObjectMap<List<OnTripAccessArrival<T>>> arrivalsForStopPosition =
    new TIntObjectHashMap<>();

  /** Returns {@code true} if there are on-board arrivals waiting to board at {@code stopPos}. */
  public boolean arrivalExistForStopPosition(int stopPos) {
    return arrivalsForStopPosition.containsKey(stopPos);
  }

  /** Returns all on-board arrivals that should board at {@code stopPos}. */
  public Iterable<OnTripAccessArrival<T>> listArrivals(int stopPos) {
    return arrivalsForStopPosition.get(stopPos);
  }

  /// Adds an on-board arrival, indexing it by its boarding stop position in the pattern.
  ///
  /// @param startRoutingAtStopPosition The stop position in trip pattern where the algorithm will
  ///   inject the boarding. The boarding position can be any position BEFORE or equal to this. The
  ///   effect is that the algorithm will not alight before the next stop after this. This is used
  ///   to support pass-through where we do not want to alight before the pass-through stop.
  public void add(
    ArrivalView<T> accessStopArrival,
    int startRoutingAtStopPosition,
    RaptorTripScheduleStopPosition boardingConstraint
  ) {
    var list = arrivalsForStopPosition.get(startRoutingAtStopPosition);

    if (list == null) {
      list = new ArrayList<>();
      arrivalsForStopPosition.put(startRoutingAtStopPosition, list);
    }
    list.add(new OnTripAccessArrival<>(accessStopArrival, boardingConstraint));
  }
}
