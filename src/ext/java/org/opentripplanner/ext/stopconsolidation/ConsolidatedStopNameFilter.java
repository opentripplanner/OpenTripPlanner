package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

public class ConsolidatedStopNameFilter implements ItineraryListFilter {

  private final StopConsolidationModel model;

  public ConsolidatedStopNameFilter(StopConsolidationModel scm) {
    this.model = scm;
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
          model.agencySpecificName(stl.getFrom().stop),
          model.agencySpecificName(stl.getTo().stop)
        );
      } else {
        return leg;
      }
    });
  }

  private boolean needsToRenameStops(ScheduledTransitLeg stl) {
    return (
      model.isSecondaryStop(stl.getFrom().stop) || model.isSecondaryStop(stl.getTo().stop)
    );
  }
}
