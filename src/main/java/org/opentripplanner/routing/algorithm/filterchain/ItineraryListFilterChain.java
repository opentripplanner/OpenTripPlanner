package org.opentripplanner.routing.algorithm.filterchain;

import static org.opentripplanner.routing.api.response.InputField.DATE_TIME;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.LatestDepartureTimeFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveTransitIfStreetOnlyIsBetterFilter;
import org.opentripplanner.routing.api.response.RoutingError;

public class ItineraryListFilterChain {

  private final List<ItineraryListFilter> filters;

  private final boolean debug;

  private final List<RoutingError> routingErrors = new ArrayList<>();

  public ItineraryListFilterChain(List<ItineraryListFilter> filters, boolean debug) {
    this.filters = filters;
    this.debug = debug;
  }

  public List<Itinerary> filter(List<Itinerary> itineraries) {
    List<Itinerary> result = itineraries;
    for (ItineraryListFilter filter : filters) {
      result = filter.filter(result);
    }

    Predicate<Itinerary> isOnStreetAllTheWay = Itinerary::isOnStreetAllTheWay;

    boolean hasTransitItineraries = itineraries
      .stream()
      .anyMatch(Predicate.not(isOnStreetAllTheWay));

    boolean allTransitItinerariesDeleted = result
      .stream()
      .filter(Predicate.not(isOnStreetAllTheWay))
      .allMatch(Itinerary::isFlaggedForDeletion);

    // Add errors, if there were any itineraries, but they were all filtered away
    if (hasTransitItineraries && allTransitItinerariesDeleted) {
      Predicate<Itinerary> isWorseThanStreet = it ->
        it
          .getSystemNotices()
          .stream()
          .anyMatch(notice -> notice.tag.equals(RemoveTransitIfStreetOnlyIsBetterFilter.TAG));
      Predicate<Itinerary> isOutsideSearchWindow = it ->
        it
          .getSystemNotices()
          .stream()
          .anyMatch(notice -> notice.tag.equals(LatestDepartureTimeFilter.TAG));
      if (result.stream().allMatch(isOnStreetAllTheWay.or(isWorseThanStreet))) {
        var nonTransitIsWalking = result.stream().flatMap(Itinerary::getStreetLegs).allMatch(StreetLeg::isWalkingLeg);
        if(nonTransitIsWalking) {
          routingErrors.add(new RoutingError(WALKING_BETTER_THAN_TRANSIT, null));
        }
      } else if (result.stream().allMatch(isOnStreetAllTheWay.or(isOutsideSearchWindow))) {
        routingErrors.add(new RoutingError(NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW, DATE_TIME));
      }
    }

    if (debug) {
      return result;
    }
    return result
      .stream()
      .filter(Predicate.not(Itinerary::isFlaggedForDeletion))
      .collect(Collectors.toList());
  }

  public List<RoutingError> getRoutingErrors() {
    return routingErrors;
  }
}
