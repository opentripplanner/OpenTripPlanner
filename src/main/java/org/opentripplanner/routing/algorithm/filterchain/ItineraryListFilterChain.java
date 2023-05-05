package org.opentripplanner.routing.algorithm.filterchain;

import static org.opentripplanner.routing.api.response.InputField.DATE_TIME;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.OutsideSearchWindowFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveTransitIfStreetOnlyIsBetterFilter;
import org.opentripplanner.routing.api.response.RoutingError;

public class ItineraryListFilterChain {

  private final List<ItineraryListFilter> filters;
  private final DeleteResultHandler debugHandler;

  private final List<RoutingError> routingErrors = new ArrayList<>();

  public ItineraryListFilterChain(
    List<ItineraryListFilter> filters,
    DeleteResultHandler debugHandler
  ) {
    this.debugHandler = debugHandler;
    this.filters = filters;
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
      if (result.stream().allMatch(isOnStreetAllTheWay.or(isWorseThanStreet))) {
        routingErrors.add(new RoutingError(WALKING_BETTER_THAN_TRANSIT, null));
      } else if (
        result.stream().allMatch(isOnStreetAllTheWay.or(OutsideSearchWindowFilter::taggedBy))
      ) {
        routingErrors.add(new RoutingError(NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW, DATE_TIME));
      }
    }
    return debugHandler.filter(result);
  }

  public List<RoutingError> getRoutingErrors() {
    return routingErrors;
  }
}
