package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.MultiCriteriaRoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
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
 *    We do NOT allow one trip to exclude the pattern-rides of another trip in the pareto-set.
 *    Two pattern-rides are both optimal, if they have boarded the same pattern, in the same round,
 *    but on different trips/vehicles. This have no measurable impact on performance, compared with
 *    allowing an earlier trip dominating a later one. But, it allows for a trip to be optimal at
 *    some stops, and another trip to be optimal at other stops. This may happen if the
 *    generalized-cost is not <em>increasing</em> with the same amount for each trip between each
 *    stop.
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
