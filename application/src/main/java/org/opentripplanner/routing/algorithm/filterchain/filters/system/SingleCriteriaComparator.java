package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.util.Comparator;
import java.util.function.ToIntFunction;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.transit.model.network.grouppriority.DefaultTransitGroupPriorityCalculator;

/**
 * Comparator used to compare a SINGLE criteria for dominance. The difference between this and the
 * {@link org.opentripplanner.raptor.util.paretoset.ParetoComparator} is that:
 * <ol>
 *   <li>This applies to one criteria, not multiple.</li>
 *   <li>This interface applies to itineraries; It is not generic.</li>
 * </ol>
 * A set of instances of this interface can be used to create a pareto-set. See
 * {@link org.opentripplanner.raptor.util.paretoset.ParetoSet} and
 * {@link org.opentripplanner.raptor.util.paretoset.ParetoComparator}.
 * <p/>
 * This interface extends {@link Comparator} so elements can be sorted as well. Not all criteria
 * can be sorted, if so the {@link #strictOrder()} should return false (this is the default).
 */
@FunctionalInterface
public interface SingleCriteriaComparator {
  DefaultTransitGroupPriorityCalculator GROUP_PRIORITY_CALCULATOR =
    new DefaultTransitGroupPriorityCalculator();

  /**
   * The left criteria dominates the right criteria. Note! The right criteria may dominate
   * the left criteria if there is no {@link #strictOrder()}. If left and right are equals, then
   * there is no dominance.
   */
  boolean leftDominanceExist(Itinerary left, Itinerary right);

  /**
   * Return true if the criteria can be deterministically sorted.
   */
  default boolean strictOrder() {
    return false;
  }

  static SingleCriteriaComparator compareNumTransfers() {
    return compareLessThan(Itinerary::getNumberOfTransfers);
  }

  static SingleCriteriaComparator compareGeneralizedCost() {
    return compareLessThan(Itinerary::getGeneralizedCost);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  static SingleCriteriaComparator compareTransitGroupsPriority() {
    return (left, right) ->
      GROUP_PRIORITY_CALCULATOR.dominanceFunction()
        .leftDominateRight(left.getGeneralizedCost2().get(), right.getGeneralizedCost2().get());
  }

  static SingleCriteriaComparator compareLessThan(final ToIntFunction<Itinerary> op) {
    return new SingleCriteriaComparator() {
      @Override
      public boolean leftDominanceExist(Itinerary left, Itinerary right) {
        return op.applyAsInt(left) < op.applyAsInt(right);
      }

      @Override
      public boolean strictOrder() {
        return true;
      }
    };
  }
}
