package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class FlexAccessFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final FlexTemplateFactory templateFactory;

  public FlexAccessFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator pathCalculator,
    Duration maxTransferDuration
  ) {
    this.callbackService = callbackService;
    this.templateFactory = FlexTemplateFactory.of(pathCalculator, maxTransferDuration);
  }

  public Collection<FlexAccessEgress> createFlexAccesses(
    Collection<NearbyStop> streetAccesses,
    List<FlexServiceDate> dates
  ) {
    var flexAccessTemplates = calculateFlexAccessTemplates(streetAccesses, dates);

    return flexAccessTemplates
      .stream()
      .flatMap(template -> template.createFlexAccessEgressStream(callbackService))
      .toList();
  }

  List<FlexAccessTemplate> calculateFlexAccessTemplates(
    Collection<NearbyStop> streetAccesses,
    List<FlexServiceDate> dates
  ) {
    var result = new ArrayList<FlexAccessTemplate>();
    var closestFlexTrips = ClosestTrip.of(callbackService, streetAccesses, dates, true);

    for (var it : closestFlexTrips) {
      for (var date : it.activeDates()) {
        result.addAll(templateFactory.createAccessTemplates(date, it.flexTrip(), it.nearbyStop()));
      }
    }
    return result;
  }
}
