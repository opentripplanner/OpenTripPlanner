package org.opentripplanner.ext.emissions;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.framework.model.Grams;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Calculates the emissions for the itineraries and adds them.
 * @param emissionsService
 */
@Sandbox
public record EmissionsFilter(EmissionsService emissionsService) implements ItineraryListFilter {
  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    for (Itinerary itinerary : itineraries) {
      List<TransitLeg> transitLegs = itinerary
        .getLegs()
        .stream()
        .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
        .map(TransitLeg.class::cast)
        .toList();

      calculateCo2EmissionsForTransit(transitLegs)
        .ifPresent(co2 -> {
          itinerary.setEmissionsPerPerson(new Emissions(co2));
        });

      List<StreetLeg> carLegs = itinerary
        .getLegs()
        .stream()
        .filter(l -> l instanceof StreetLeg)
        .map(StreetLeg.class::cast)
        .filter(leg -> leg.getMode() == TraverseMode.CAR)
        .toList();

      calculateCo2EmissionsForCar(carLegs)
        .ifPresent(co2 -> {
          if (transitLegs.isEmpty()) {
            itinerary.setEmissionsPerPerson(new Emissions(co2));
          } else if (
            itinerary.getEmissionsPerPerson() != null &&
            itinerary.getEmissionsPerPerson().getCo2() != null
          ) {
            // Calculate park and ride emissions.
            itinerary.setEmissionsPerPerson(
              new Emissions(itinerary.getEmissionsPerPerson().getCo2().plus(co2))
            );
          }
        });
    }
    return itineraries;
  }

  private Optional<Grams> calculateCo2EmissionsForTransit(List<TransitLeg> transitLegs) {
    if (transitLegs.isEmpty()) {
      return Optional.empty();
    }
    Grams co2Emissions = new Grams(0.0);
    for (TransitLeg leg : transitLegs) {
      FeedScopedId feedScopedRouteId = new FeedScopedId(
        leg.getAgency().getId().getFeedId(),
        leg.getRoute().getId().getId()
      );
      Optional<Emissions> co2EmissionsForRoute = emissionsService.getEmissionsPerMeterForRoute(
        feedScopedRouteId
      );
      if (co2EmissionsForRoute.isPresent()) {
        co2Emissions =
          co2Emissions.plus(co2EmissionsForRoute.get().getCo2().multiply(leg.getDistanceMeters()));
      } else {
        // Partial results would not give an accurate representation of the emissions.
        return Optional.empty();
      }
    }
    return Optional.ofNullable(co2Emissions);
  }

  private Optional<Grams> calculateCo2EmissionsForCar(List<StreetLeg> carLegs) {
    if (carLegs.isEmpty()) {
      return Optional.empty();
    }
    return emissionsService
      .getEmissionsPerMeterForCar()
      .map(emissions ->
        new Grams(
          carLegs
            .stream()
            .mapToDouble(leg -> emissions.getCo2().multiply(leg.getDistanceMeters()).asDouble())
            .sum()
        )
      );
  }
}
