package org.opentripplanner.routing.algorithm.filterchain.filter;

import java.util.List;
import java.util.function.Function;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;

public class TransitAlertFilter implements ItineraryListFilter {

  private final AlertToLegMapper alertToLegMapper;

  public TransitAlertFilter(
    TransitAlertService transitAlertService,
    Function<Station, MultiModalStation> getMultiModalStation
  ) {
    alertToLegMapper = new AlertToLegMapper(transitAlertService, getMultiModalStation);
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    for (Itinerary itinerary : itineraries) {
      boolean firstLeg = true;
      for (Leg leg : itinerary.getLegs()) {
        if (leg.isTransitLeg()) {
          alertToLegMapper.addTransitAlertsToLeg(leg, firstLeg);
          firstLeg = false;
        }
      }
    }

    return itineraries;
  }
}
