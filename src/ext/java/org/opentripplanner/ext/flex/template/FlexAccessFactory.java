package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class FlexAccessFactory {

  private final FlexAccessEgressCallbackService callbackService;
  private final FlexPathCalculator pathCalculator;
  private final Duration maxTransferDuration;

  public FlexAccessFactory(
    FlexAccessEgressCallbackService callbackService,
    FlexPathCalculator pathCalculator,
    Duration maxTransferDuration
  ) {
    this.callbackService = callbackService;
    this.pathCalculator = pathCalculator;
    this.maxTransferDuration = maxTransferDuration;
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
    var templateFactory = FlexTemplateFactory.of(pathCalculator, maxTransferDuration);

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
