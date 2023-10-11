package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

public class ConsolidatedStopNameFilter implements ItineraryListFilter {

  private final StopConsolidationService service;

  public ConsolidatedStopNameFilter(StopConsolidationService service) {
    this.service = service;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::changeNames).toList();
  }

  private Itinerary changeNames(Itinerary i) {
    return i.transformTransitLegs(leg -> {
      if (leg instanceof ScheduledTransitLeg stl && needsToRenameStops(stl)) {
        return new ConsolidatedStopLeg(
          stl,
          service.agencySpecificName(stl.getFrom().stop),
          service.agencySpecificName(stl.getTo().stop)
        );
      } else {
        return leg;
      }
    });
  }

  private boolean needsToRenameStops(ScheduledTransitLeg stl) {
    return (
      service.isSecondaryStop(stl.getFrom().stop) || service.isSecondaryStop(stl.getTo().stop)
    );
  }
}
