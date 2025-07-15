package org.opentripplanner.routing.alternativelegs;

import java.util.function.Function;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.network.TripPattern;

public enum AlternativeLegsFilter {
  NO_FILTER((Leg leg) -> (TripPattern tripPattern) -> true),
  SAME_AGENCY(
    (Leg leg) ->
      (TripPattern tripPattern) -> leg.agency().equals(tripPattern.getRoute().getAgency())
  ),
  SAME_ROUTE((Leg leg) -> (TripPattern tripPattern) -> leg.route().equals(tripPattern.getRoute())),
  SAME_MODE(
    (Leg leg) -> (TripPattern tripPattern) -> leg.trip().getMode().equals(tripPattern.getMode())
  ),
  SAME_SUBMODE(
    (Leg leg) ->
      (TripPattern tripPattern) ->
        leg.trip().getNetexSubMode().equals(tripPattern.getNetexSubmode())
  );

  public final Function<Leg, Predicate<TripPattern>> predicateGenerator;

  AlternativeLegsFilter(Function<Leg, Predicate<TripPattern>> predicateGenerator) {
    this.predicateGenerator = predicateGenerator;
  }

  public Predicate<TripPattern> getFilter(Leg leg) {
    return predicateGenerator.apply(leg);
  }
}
