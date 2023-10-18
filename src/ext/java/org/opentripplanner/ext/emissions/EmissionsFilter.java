package org.opentripplanner.ext.emissions;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Sandbox
public record EmissionsFilter(EmissionsService emissionsService) implements ItineraryListFilter {
  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    for (Itinerary i : itineraries) {
      Emissions emissions = new Emissions();

      Optional<Double> carbonDioxide = this.getEmissionsForItinerary(i, EmissionType.CO2);
      if (carbonDioxide.isPresent()) {
        emissions.setCo2grams(carbonDioxide.get());
      }

      i.setEmissions(emissions);
    }
    return itineraries;
  }

  public Optional<Double> getEmissionsForItinerary(Itinerary itinerary, EmissionType emissionType) {
    List<TransitLeg> transitLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(TransitLeg.class::cast)
      .toList();

    if (!transitLegs.isEmpty()) {
      return Optional.ofNullable(calculateEmissionsForTransit(transitLegs, emissionType));
    }

    List<StreetLeg> carLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof StreetLeg)
      .map(StreetLeg.class::cast)
      .filter(leg -> leg.getMode() == TraverseMode.CAR)
      .toList();

    if (!carLegs.isEmpty()) {
      return calculateEmissionsForCar(carLegs, emissionType);
    }
    return Optional.empty();
  }

  private Double calculateEmissionsForTransit(
    List<TransitLeg> transitLegs,
    EmissionType emissionType
  ) {
    Double emissions = 0.0;
    for (TransitLeg leg : transitLegs) {
      FeedScopedId feedScopedRouteId = new FeedScopedId(
        leg.getAgency().getId().getFeedId(),
        leg.getRoute().getId().getId()
      );
      Optional<Double> emissionsForRoute = emissionsService.getEmissionsPerMeterForRoute(
        feedScopedRouteId,
        emissionType
      );
      if (emissionsForRoute.isPresent()) {
        emissions += emissionsForRoute.get() * leg.getDistanceMeters();
      } else {
        // Partial results would not give an accurate representation of the emissions.
        return null;
      }
    }
    return emissions;
  }

  private Optional<Double> calculateEmissionsForCar(
    List<StreetLeg> carLegs,
    EmissionType emissionType
  ) {
    Optional<Double> emissionsForCar = emissionsService.getCarEmissionsPerMeter(emissionType);
    if (emissionsForCar.isPresent()) {
      return Optional.of(
        carLegs.stream().mapToDouble(leg -> emissionsForCar.get() * leg.getDistanceMeters()).sum()
      );
    }
    return Optional.empty();
  }
}
