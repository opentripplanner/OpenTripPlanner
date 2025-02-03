package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.filter.FlexTripFilter;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class FlexEgressFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final FlexTripFilter filter;
  private final FlexTemplateFactory templateFactory;

  public FlexEgressFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator pathCalculator,
    Duration maxTransferDuration,
    FlexTripFilter filter
  ) {
    this.callbackService = callbackService;
    this.filter = filter;
    this.templateFactory = FlexTemplateFactory.of(pathCalculator, maxTransferDuration);
  }

  public List<FlexAccessEgress> createFlexEgresses(
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
    var closestFlexTrips = ClosestTrip.of(callbackService, streetEgresses, dates, false);
    return closestFlexTrips
      .stream()
      .filter(ct -> filter.allowsTrip(ct.flexTrip().getTrip()))
      .flatMap(it -> templateFactory.createEgressTemplates(it).stream())
      .toList();
  }
}
