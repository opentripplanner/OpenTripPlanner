package org.opentripplanner.ext.fares;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.fares.FareService;

public record FaresFilter(FareService fareService) implements ItineraryListFilter {
  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .map(i -> {
        i.setFare(fareService.getCost(i));
        return i;
      })
      .toList();
  }
}
