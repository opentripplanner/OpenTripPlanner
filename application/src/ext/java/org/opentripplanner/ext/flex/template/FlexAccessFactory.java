package org.opentripplanner.ext.flex.template;

import java.time.Duration;
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

  public List<FlexAccessEgress> createFlexAccesses(
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
    var closestFlexTrips = ClosestTrip.of(callbackService, streetAccesses, dates, true);
    return closestFlexTrips
      .stream()
      .flatMap(it -> templateFactory.createAccessTemplates(it).stream())
      .toList();
  }
}
