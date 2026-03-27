package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.MultiCriteriaRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * The implementation of this interface, a pattern-ride, represent the STATE for one possible path
 * up until the point where a given trip is boarded(onboard of a vehicle). It represents the STATE
 * of riding a trip, having boarded at a given stop, but not yet alighted.
 * <p>
 * Instances of this class only exist in the context of a given pattern for a given round. Hence,
 * when comparing instances we may assume that they have the same number-of-transfers and the same
 * Pattern. We take advantage of this by excluding all "constant" criteria from the pattern-ride
 * comparator used by the pareto-set of patternRides.
 * <p>
 * This implementation of the multi-criteria Range Raptor keeps all pareto-optimal _rides_ for each
 * pattern while possessing each stop down the line. This class keep the needed state for these
 * rides to avoid recalculating each value more than once and to be able to put them in a
 * {@link ParetoSet}.
 * <p>
 * We do not do this the same way as described in the original Raptor paper. The original McRaptor
 * algorithm keep a bag of labels(stop-arrivals) while traversing the pattern. We keep a "bag" of
 * {@code patternRides} in {@link MultiCriteriaRoutingStrategy} for the given pattern. The main
 * differences are:
 * <ul>
 *  <li>
 *    Alight-/arrival specific cost is not included when comparing pattern-rides. This is
 *    ok, since we add this before adding a path to the stop-arrivals at a given stop. This
 *    assumes that the cost of alighting/arrival is the same for all paths arriving by the same
 *    trip. This allow us to eliminate paths, without doing the actual stop-arrival cost
 *    calculation.
 *  </li>
 *  <li>
 *    An earlier-departing trip (lower {@code tripSortIndex}) dominates a later one when its cost
 *    is equal or better. This reduces the on-board optimal set compared with treating all trips as
 *    incomparable.
 *    <p>
 *    For circular patterns with a "tail" (stops that only appear after the loop completes), a
 *    passenger heading to a tail stop should board on the second pass through a loop stop, thereby
 *    skipping the full circle. On the second pass, an earlier-departing trip is typically
 *    available: the earlier trip's second pass will reach the tail stop no later than the later
 *    trip's first pass. The earlier-departure dominance therefore remains correct even for
 *    tail-stop destinations.
 *  </li>
 *  <li>
 *    We do not have to update all elements in the "pattern-bag" for every stop visited. The
 *    {@code relative-cost} is calculated once - when adding the path to the "pattern-bag"
 *    of pattern-rides.
 *  </li>
 * </ul>
 * This interface extends the {@link PatternRideView} with methods which mutate the PatternRide
 * (a new copy is created, since the rides are immutable). The interface is used by Raptor to
 * perform the routing, while the view is used by other parts; hence only need read access.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface PatternRide<T extends RaptorTripSchedule>
  extends PatternRideView<T, McStopArrival<T>> {
  /**
   * Change the ride by setting a new c2 value. Since the ride is immutable the
   * new ride is copied and returned.
   */
  PatternRide<T> updateC2(int newC2);
}
