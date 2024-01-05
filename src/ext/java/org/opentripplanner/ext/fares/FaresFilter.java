package org.opentripplanner.ext.fares;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.spi.ItineraryListFilter;
import org.opentripplanner.routing.fares.FareService;

/**
 * Computes the fares of an itinerary and adds them.
 */
public record FaresFilter(FareService fareService) implements ItineraryListFilter {
  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .peek(i -> {
        var fare = fareService.calculateFares(i);
        if (Objects.nonNull(fare)) {
          i.setFare(fare);
          FaresToItineraryMapper.addFaresToLegs(fare, i);
        }
      })
      .toList();
  }
}
