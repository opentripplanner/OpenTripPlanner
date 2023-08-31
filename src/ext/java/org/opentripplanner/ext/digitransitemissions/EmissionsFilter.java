package org.opentripplanner.ext.digitransitemissions;

import java.util.List;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

@Sandbox
public record EmissionsFilter(EmissionsService emissionsService) implements ItineraryListFilter {
  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    for (Itinerary i : itineraries) {
      Float emissions = emissionsService.getEmissionsForItinerary(i);
      if (emissions != null) {
        i.setEmissions(emissions);
      }
    }
    return itineraries;
  }
}
