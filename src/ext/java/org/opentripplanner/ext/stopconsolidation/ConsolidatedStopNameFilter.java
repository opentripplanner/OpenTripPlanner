package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

public class ConsolidatedStopNameFilter implements ItineraryListFilter {

  //private final StopConsolidationModel model;

  public ConsolidatedStopNameFilter() {
    //this.model = model;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::changeNames).toList();
  }

  private Itinerary changeNames(Itinerary i) {
    return i.transformTransitLegs(leg -> {

      if (leg instanceof ScheduledTransitLeg stl) {

        return new ConsolidatedStopLeg(
          stl,
          I18NString.of("CONSOLIDATED START"),
          I18NString.of("CONSOLIDATED END")
        );
      } else {
        return leg;
      }
    });

  }
}
