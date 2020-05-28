package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.util.CompositeComparator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

 /**
 * This filter will sort the itineraries in OTP default order according to the request
  * {@code arriveBy} flag.
 * <p>
 * The SORT ORDER for a "depart-after-search" is:
 * <ol>
 *     <li>ON-STREET-ONLY before TRANSIT</li>
 *     <li>Earliest arrival time first</li>
 *     <li>Generalized cost, lowest first</li>
 *     <li>Number of transfers, lowest first</li>
 *     <li>Latest departure time first</li>
 * </ol>
  * <p>
  * The SORT ORDER for a "arrive-by-search" is:
  * <ol>
 *     <li>ON-STREET-ONLY before TRANSIT</li>
  *     <li>Latest departure time first</li>
 *     <li>Generalized cost, lowest cost first</li>
 *     <li>Number of transfers, lowest first</li>
  *     <li>Earliest arrival time first</li>
  * </ol>
 * <p>
 * The filter do only sort the itineraries, no other modifications are done.
 */
public class OtpDefaultSortOrder implements ItineraryFilter {

  /**
   * This comparator will sort all itineraries with STREET ONLY first. So, if there is an itinerary
   * with walking/bicycle/car from origin all the way to the destination, than it will be sorted
   * before any itineraries with one or more transit legs.
   */
  static final Comparator<Itinerary> STREET_ONLY_FIRST
      = (a, b) -> Boolean.compare(b.isOnStreetAllTheWay(), a.isOnStreetAllTheWay());

  /** Sort latest arrival-time first */
  static final Comparator<Itinerary> ARRIVAL_TIME = Comparator.comparing(Itinerary::endTime);
  static final Comparator<Itinerary> DEPARTURE_TIME = (a, b) ->  b.startTime().compareTo(a.startTime());
  static final Comparator<Itinerary> GENERALIZED_COST = Comparator.comparingInt(a -> a.generalizedCost);
  static final Comparator<Itinerary> NUM_OF_TRANSFERS = Comparator.comparingInt(a -> a.nTransfers);

  private final Comparator<Itinerary> sortComparator;

  public OtpDefaultSortOrder(boolean arriveBy) {
        // Put walking first - encourage healthy lifestyle
    sortComparator = new CompositeComparator<>(
        STREET_ONLY_FIRST,
        arriveBy ? DEPARTURE_TIME : ARRIVAL_TIME,
        GENERALIZED_COST,
        NUM_OF_TRANSFERS,
        arriveBy ? ARRIVAL_TIME : DEPARTURE_TIME
    );
  }

  @Override
  public String name() {
        return "otp-default-sort-order";
    }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().sorted(sortComparator).collect(Collectors.toList());
  }

  @Override
  public boolean removeItineraries() { return false; }
}
