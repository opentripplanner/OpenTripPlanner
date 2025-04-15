package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.function.Function;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;

public final class DecorateTransitAlert implements ItineraryDecorator {

  private final AlertToLegMapper alertToLegMapper;

  public DecorateTransitAlert(
    TransitAlertService transitAlertService,
    Function<Station, MultiModalStation> getMultiModalStation
  ) {
    alertToLegMapper = new AlertToLegMapper(transitAlertService, getMultiModalStation);
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    var d = new LegDecorator();
    return itinerary.copyOf().transformTransitLegs(d::decorate).build();
  }

  private final class LegDecorator {

    boolean isFirstTransitLeg = true;

    TransitLeg decorate(TransitLeg leg) {
      if (leg.isTransitLeg()) {
        var l = alertToLegMapper.decorateWithAlerts(leg, isFirstTransitLeg);
        isFirstTransitLeg = false;
        return l;
      } else {
        return leg;
      }
    }
  }
}
