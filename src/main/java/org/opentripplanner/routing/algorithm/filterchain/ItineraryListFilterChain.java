package org.opentripplanner.routing.algorithm.filterchain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
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

    routingErrors.addAll(RoutingErrorsAttacher.computeErrors(itineraries, result));

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
