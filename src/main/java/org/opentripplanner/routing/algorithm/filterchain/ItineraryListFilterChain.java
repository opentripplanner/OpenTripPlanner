package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.LatestDepartureTimeFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveTransitIfStreetOnlyIsBetterFilter;
import org.opentripplanner.routing.api.response.RoutingError;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.api.response.InputField.DATE_TIME;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW;
import static org.opentripplanner.routing.api.response.RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT;

public class ItineraryListFilterChain {
    private final List<ItineraryListFilter> filters;
    private final boolean debug;

    public ItineraryListFilterChain(List<ItineraryListFilter> filters, boolean debug) {
        this.filters = filters;
        this.debug = debug;
    }

    public List<Itinerary> filter(List<Itinerary> itineraries, Collection<RoutingError> routingErrors) {
        List<Itinerary> result = itineraries;
        for (ItineraryListFilter filter : filters) {
            result = filter.filter(result);
        }

        boolean allDeleted = result.stream().allMatch(Itinerary::isFlaggedForDeletion);

        // Add errors, if there were any itineraries, but they were all filtered away
        if (!itineraries.isEmpty() && allDeleted) {
            Predicate<Itinerary> isOnStreetAllTheWay = Itinerary::isOnStreetAllTheWay;
            Predicate<Itinerary> isWorseThanStreet = it -> it.systemNotices.stream().anyMatch(
                notice -> notice.tag.equals(RemoveTransitIfStreetOnlyIsBetterFilter.NAME));
            Predicate<Itinerary> isOutsideSearchWindow = it -> it.systemNotices.stream().anyMatch(
                notice -> notice.tag.equals(LatestDepartureTimeFilter.NAME));
            if (result.stream().allMatch(isOnStreetAllTheWay.or(isWorseThanStreet))) {
                routingErrors.add(new RoutingError(WALKING_BETTER_THAN_TRANSIT, null));
            } else if (result.stream().allMatch(isOnStreetAllTheWay.or(isOutsideSearchWindow))) {
                routingErrors.add(new RoutingError(NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW, DATE_TIME));
            }
        }

        if (debug) {
          return result;
        }  
        return result.stream()
                .filter(Predicate.not(Itinerary::isFlaggedForDeletion))
                .collect(Collectors.toList());
    }
}
