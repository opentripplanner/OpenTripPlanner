package org.opentripplanner.routing.algorithm.filterchain.comparator;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.util.CompositeComparator;

import java.util.Comparator;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

/**
 * This comparator implements the itinerary sort order defined by the {@link SortOrder}.
 * <p>
 * The filter do only sort the itineraries, no other modifications are done.
 * <p>
 * THIS CLASS IS THREAD-SAFE
 */
public class SortOrderComparator extends CompositeComparator<Itinerary> {

  /**
   * This comparator will sort all itineraries with STREET ONLY first. So, if there is an itinerary
   * with walking/bicycle/car from origin all the way to the destination, than it will be sorted
   * before any itineraries with one or more transit legs.
   */
  static final Comparator<Itinerary> STREET_ONLY_FIRST_COMP = (a, b) -> Boolean.compare(
      b.isOnStreetAllTheWay(),
      a.isOnStreetAllTheWay()
  );

  /** Sort latest arrival-time first */
  static final Comparator<Itinerary> ARRIVAL_TIME_COMP = comparing(Itinerary::endTime);
  static final Comparator<Itinerary> DEPARTURE_TIME_COMP = comparing(Itinerary::startTime).reversed();
  static final Comparator<Itinerary> GENERALIZED_COST_COMP = comparingInt(a -> a.generalizedCost);
  static final Comparator<Itinerary> NUM_OF_TRANSFERS_COMP = comparingInt(a -> a.nTransfers);

  private static final SortOrderComparator STREET_AND_ARRIVAL_TIME = new SortOrderComparator(
          STREET_ONLY_FIRST_COMP,
          ARRIVAL_TIME_COMP,
          GENERALIZED_COST_COMP,
          NUM_OF_TRANSFERS_COMP,
          DEPARTURE_TIME_COMP
  );

  private static final SortOrderComparator STREET_AND_DEPARTURE_TIME = new SortOrderComparator(
          STREET_ONLY_FIRST_COMP,
          DEPARTURE_TIME_COMP,
          GENERALIZED_COST_COMP,
          NUM_OF_TRANSFERS_COMP,
          ARRIVAL_TIME_COMP
  );

  private static final SortOrderComparator GENERALIZED_COST = new SortOrderComparator(
          GENERALIZED_COST_COMP,
          NUM_OF_TRANSFERS_COMP
  );

  private static final SortOrderComparator NUM_TRANSFERS = new SortOrderComparator(
          NUM_OF_TRANSFERS_COMP,
          GENERALIZED_COST_COMP
  );

  /** Return the default comparator for a depart-after search. */
  public static SortOrderComparator defaultComparatorDepartAfter() {
    return STREET_AND_ARRIVAL_TIME;
  }

  /** Return the default comparator for an arrive-by search. */
  public static SortOrderComparator defaultComparatorArriveBy() {
    return STREET_AND_DEPARTURE_TIME;
  }

  /**
   * This comparator sorts itineraries based on the generalized-cost. If the cost is the same then
   * the filter pick the itinerary with the lowest number-of-transfers.
   */
  public static SortOrderComparator generalizedCostComparator() {
    return GENERALIZED_COST;
  }

  /**
   * This comparator sorts itineraries based on the fewest number-of-transfers. If the number is the
   * same then the filter pick the itinerary with the lowest generalized-cost.
   */
    public static SortOrderComparator numberOfTransfersComparator() {
    return NUM_TRANSFERS;
  }

  public static SortOrderComparator comparator(SortOrder sortOrder) {
    switch (sortOrder) {
      case STREET_AND_ARRIVAL_TIME: return STREET_AND_ARRIVAL_TIME;
      case STREET_AND_DEPARTURE_TIME: return STREET_AND_DEPARTURE_TIME;
    }
    throw new IllegalArgumentException();
  }

  @SafeVarargs
  private SortOrderComparator(Comparator<Itinerary> ... compareVector) {
    super(compareVector);
  }
}
