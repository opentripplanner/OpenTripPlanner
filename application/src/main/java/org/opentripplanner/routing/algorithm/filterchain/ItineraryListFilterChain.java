package org.opentripplanner.routing.algorithm.filterchain;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.filterchain.DeleteResultHandler;
import org.opentripplanner.routing.algorithm.filterchain.framework.filterchain.RoutingErrorsAttacher;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;
import org.opentripplanner.routing.api.response.RoutingError;

public class ItineraryListFilterChain {

  private final List<ItineraryListFilter> filters;
  private final DeleteResultHandler debugHandler;
  private final PageCursorInputAggregator pageCursorInputAggregator;

  private final List<RoutingError> routingErrors = new ArrayList<>();

  public ItineraryListFilterChain(
    List<ItineraryListFilter> filters,
    DeleteResultHandler debugHandler,
    PageCursorInputAggregator pageCursorInputAggregator
  ) {
    this.debugHandler = debugHandler;
    this.filters = filters;
    this.pageCursorInputAggregator = pageCursorInputAggregator;
  }

  public List<Itinerary> filter(List<Itinerary> itineraries) {
    List<Itinerary> result = itineraries;
    for (ItineraryListFilter filter : filters) {
      result = filter.filter(result);
    }

    pageCursorInputAggregator.providePageCursorInput();

    routingErrors.addAll(RoutingErrorsAttacher.computeErrors(itineraries, result));

    return debugHandler.filter(result);
  }

  public List<RoutingError> getRoutingErrors() {
    return routingErrors;
  }
}
