package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;

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
    final var firstLeg = new AtomicBoolean(true);
    itinerary.transformTransitLegs(leg -> {
      if (leg.isTransitLeg()) {
        var l = alertToLegMapper.copyLegWithAlerts(leg, firstLeg.get());
        firstLeg.set(false);
        return l;
      } else {
        return leg;
      }
    });
  }
}
