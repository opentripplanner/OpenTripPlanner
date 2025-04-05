package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.timetable.Trip;

public class FlexEgressFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final Matcher<Trip> matcher;
  private final FlexTemplateFactory templateFactory;

  public FlexEgressFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator pathCalculator,
    Duration maxTransferDuration,
    Matcher<Trip> matcher
  ) {
    this.callbackService = callbackService;
    this.matcher = matcher;
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
    var closestFlexTrips = ClosestTrip.of(callbackService, streetEgresses, matcher, dates, false);
    return closestFlexTrips
      .stream()
      .flatMap(it -> templateFactory.createEgressTemplates(it).stream())
      .toList();
  }
}
