package org.opentripplanner.routing.algorithm.filterchain;

import static org.opentripplanner.routing.api.response.InputField.DATE_TIME;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.OutsideSearchWindowFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveTransitIfStreetOnlyIsBetterFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveTransitIfWalkingIsBetterFilter;
import org.opentripplanner.routing.api.response.RoutingError;

/**
 * Computes {@link org.opentripplanner.routing.api.response.RoutingError} instances from itinerary
 * before and after they have been through the filter chain.
 */
class RoutingErrorsAttacher {

  /**
   * Computes error codes from the itineraries.
   *
   * @param originalItineraries The original result of the routing before the filter chain.
   * @param filteredItineraries The itineraries after being run through the filter chain so they
   *                            have the {@link org.opentripplanner.model.SystemNotice}s to look up
   *                            the error from.
   */
  static List<RoutingError> computeErrors(
    List<Itinerary> originalItineraries,
    List<Itinerary> filteredItineraries
  ) {
    final List<RoutingError> routingErrors = new ArrayList<>();
    Predicate<Itinerary> isOnStreetAllTheWay = Itinerary::isOnStreetAllTheWay;

    boolean hasTransitItineraries = originalItineraries
      .stream()
      .anyMatch(Predicate.not(isOnStreetAllTheWay));

    boolean allTransitItinerariesDeleted = filteredItineraries
      .stream()
      .filter(Predicate.not(isOnStreetAllTheWay))
      .allMatch(Itinerary::isFlaggedForDeletion);

    // Add errors, if there were any itineraries, but they were all filtered away
    if (hasTransitItineraries && allTransitItinerariesDeleted) {
      Predicate<Itinerary> isWorseThanStreet = it ->
        it
          .getSystemNotices()
          .stream()
          .anyMatch(notice -> notice.tag().equals(RemoveTransitIfStreetOnlyIsBetterFilter.TAG));
      Predicate<Itinerary> isWorseThanWalking = it ->
        it
          .getSystemNotices()
          .stream()
          .anyMatch(notice -> notice.tag().equals(RemoveTransitIfWalkingIsBetterFilter.TAG));
      if (
        filteredItineraries
          .stream()
          .allMatch(isOnStreetAllTheWay.or(isWorseThanStreet).or(isWorseThanWalking))
      ) {
        var nonTransitIsWalking = filteredItineraries
          .stream()
          .flatMap(Itinerary::getStreetLegs)
          .allMatch(StreetLeg::isWalkingLeg);
        if (nonTransitIsWalking) {
          routingErrors.add(new RoutingError(WALKING_BETTER_THAN_TRANSIT, null));
        }
      } else if (
        filteredItineraries
          .stream()
          .allMatch(isOnStreetAllTheWay.or(OutsideSearchWindowFilter::taggedBy))
      ) {
        routingErrors.add(new RoutingError(NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW, DATE_TIME));
      }
    }
    return routingErrors;
  }
}
