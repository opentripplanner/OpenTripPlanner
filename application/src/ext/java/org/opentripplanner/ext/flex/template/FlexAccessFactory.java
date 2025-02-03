package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.filter.FlexTripFilter;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class FlexAccessFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final FlexTripFilter filter;
  private final FlexTemplateFactory templateFactory;

  public FlexAccessFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator pathCalculator,
    Duration maxTransferDuration,
    FlexTripFilter filter
  ) {
    this.callbackService = callbackService;
    this.filter = filter;
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
      .filter(ct -> filter.allowsTrip(ct.flexTrip().getTrip()))
      .flatMap(it -> templateFactory.createAccessTemplates(it).stream())
      .toList();
  }
}
