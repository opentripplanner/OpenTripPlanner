package org.opentripplanner.routing.algorithm.filterchain.framework.filter;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;

public class DecorateFilter implements ItineraryListFilter {

  private final ItineraryDecorator decorator;

  public DecorateFilter(ItineraryDecorator decorator) {
    this.decorator = decorator;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    for (var it : itineraries) {
      decorator.decorate(it);
    }
    return itineraries;
  }
}
