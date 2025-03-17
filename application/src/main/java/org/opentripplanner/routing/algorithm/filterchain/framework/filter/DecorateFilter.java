package org.opentripplanner.routing.algorithm.filterchain.framework.filter;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;

/**
 * This is the decorator filter implementation. To add a decorator, you should implement
 * the {@link ItineraryDecorator}.
 */
public final class DecorateFilter implements ItineraryListFilter {

  private final ItineraryDecorator decorator;

  public DecorateFilter(ItineraryDecorator decorator) {
    this.decorator = decorator;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().map(decorator::decorate).toList();
  }
}
