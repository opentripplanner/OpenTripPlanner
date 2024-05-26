package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class FlexEgressFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final FlexPathCalculator pathCalculator;
  private final Duration maxTransferDuration;

  public FlexEgressFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator pathCalculator,
    Duration maxTransferDuration
  ) {
    this.callbackService = callbackService;
    this.pathCalculator = pathCalculator;
    this.maxTransferDuration = maxTransferDuration;
  }

  public Collection<FlexAccessEgress> createFlexEgresses(
    Collection<NearbyStop> streetEgresses,
    List<FlexServiceDate> dates
  ) {
    var flexEgressTemplates = calculateFlexEgressTemplates(streetEgresses, dates);

    return flexEgressTemplates
      .stream()
      .flatMap(template -> template.createFlexAccessEgressStream(callbackService))
      .toList();
  }

  List<FlexEgressTemplate> calculateFlexEgressTemplates(
    Collection<NearbyStop> streetEgresses,
    List<FlexServiceDate> dates
  ) {
    var templateFactory = FlexTemplateFactory.of(pathCalculator, maxTransferDuration);

    var result = new ArrayList<FlexEgressTemplate>();
    var closestFlexTrips = ClosestTrip.of(callbackService, streetEgresses, dates, false);

    for (var it : closestFlexTrips) {
      for (var date : it.activeDates()) {
        result.addAll(templateFactory.createEgressTemplates(date, it.flexTrip(), it.nearbyStop()));
      }
    }
    return result;
  }
}
