package org.opentripplanner.raptor.util.paretoset;

/**
 * Comparator used by the {@link ParetoSet} to compare to elements for dominance. There is 4
 * outcomes of a comparison between a left and right vector:
 * <ul>
 *     <li>Left dominates right - At least one left criteria dominates, and no right dominance exist
 *     <li>Right dominates left - At least one right criteria dominates, and no left dominance exist
 *     <li>Mutual dominance - At least one left criteria dominates right and at least one right criteria dominates left
 *     <li>No dominance - all criteria is equals or no dominance exist
 * </ul>
 * To implement the comparator you only need to implement the comparison in one direction - if dominance exist.
 *
 * @param <T> The pareto set element type
 */
@FunctionalInterface
public interface ParetoComparator<T> {
  /**
   * At least one of the left criteria dominates one of the corresponding right criteria.
   */
  boolean leftDominanceExist(T left, T right);
}
