package org.opentripplanner.ext.emissions;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sandbox
public record EmissionsFilter(EmissionsService emissionsService) implements ItineraryListFilter {
  private static final Logger LOG = LoggerFactory.getLogger(EmissionsFilter.class);

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .peek(i -> {
        float emissions = emissionsService.getEmissionsForRoute(i);
        if (Objects.nonNull(emissions)) {
          i.setEmissions(emissions);
        }
      })
      .toList();
  }
}
