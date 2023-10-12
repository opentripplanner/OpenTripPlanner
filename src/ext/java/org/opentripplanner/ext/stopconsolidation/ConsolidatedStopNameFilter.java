package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * A decorating filter that checks if a transit legs contains any primary stops and if it does,
 * then
 */
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
        var agency = leg.getAgency();
        return new ConsolidatedStopLeg(
          stl,
          service.agencySpecificName(stl.getFrom().stop, agency),
          service.agencySpecificName(stl.getTo().stop, agency)
        );
      } else {
        return leg;
      }
    });
  }

  private boolean needsToRenameStops(ScheduledTransitLeg stl) {
    return (service.isPrimaryStop(stl.getFrom().stop) || service.isPrimaryStop(stl.getTo().stop));
  }
}
