package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.function.Function;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.utils.lang.Box;

public class DecorateTransitAlert implements ItineraryDecorator {

  private final AlertToLegMapper alertToLegMapper;

  public DecorateTransitAlert(
    TransitAlertService transitAlertService,
    Function<Station, MultiModalStation> getMultiModalStation
  ) {
    alertToLegMapper = new AlertToLegMapper(transitAlertService, getMultiModalStation);
  }

  @Override
  public void decorate(Itinerary itinerary) {
    final var firstLeg = Box.of(true);
    itinerary.transformTransitLegs(leg -> {
      if (leg.isTransitLeg()) {
        var l = alertToLegMapper.decorateWithAlerts(leg, firstLeg.get());
        firstLeg.set(false);
        return l;
      } else {
        return leg;
      }
    });
  }
}
