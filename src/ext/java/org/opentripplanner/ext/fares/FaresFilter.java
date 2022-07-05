package org.opentripplanner.ext.fares;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.fares.FareService;

public record FaresFilter(FareService fareService) implements ItineraryListFilter {
  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .peek(i -> {
        var fare = fareService.getCost(i);
        if (Objects.nonNull(fare)) {
          i.setFare(fare);
        }
      })
      .toList();
  }
}
