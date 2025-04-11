package org.opentripplanner.ext.emission.internal.itinerary;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * Calculates the emissions for the itineraries and adds them.
 * @param emissionsService
 */
@Sandbox
public class EmissionItineraryDecorator implements ItineraryDecorator {

  private final EmissionService emissionService;

  public EmissionItineraryDecorator(EmissionService emissionService) {
    this.emissionService = emissionService;
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    List<TransitLeg> transitLegs = itinerary
      .legs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(TransitLeg.class::cast)
      .toList();

    Optional<Gram> co2ForTransit = calculateCo2EmissionsForTransit(transitLegs);

    if (!transitLegs.isEmpty() && co2ForTransit.isEmpty()) {
      return itinerary;
    }

    List<StreetLeg> carLegs = itinerary
      .legs()
      .stream()
      .filter(l -> l instanceof StreetLeg)
      .map(StreetLeg.class::cast)
      .filter(leg -> leg.getMode() == TraverseMode.CAR)
      .toList();

    Optional<Gram> co2ForCar = calculateCo2EmissionsForCar(carLegs);

    var builder = itinerary.copyOf();

    if (co2ForTransit.isPresent() && co2ForCar.isPresent()) {
      builder.withEmissionPerPerson(new Emission(co2ForTransit.get().plus(co2ForCar.get())));
    } else if (co2ForTransit.isPresent()) {
      builder.withEmissionPerPerson(new Emission(co2ForTransit.get()));
    } else if (co2ForCar.isPresent()) {
      builder.withEmissionPerPerson(new Emission(co2ForCar.get()));
    }
    return builder.build();
  }

  private Optional<Gram> calculateCo2EmissionsForTransit(List<TransitLeg> transitLegs) {
    if (transitLegs.isEmpty()) {
      return Optional.empty();
    }
    Gram co2Emissions = new Gram(0.0);
    for (TransitLeg leg : transitLegs) {
      FeedScopedId feedScopedRouteId = new FeedScopedId(
        leg.getAgency().getId().getFeedId(),
        leg.getRoute().getId().getId()
      );
      Optional<Emission> co2EmissionsForRoute = emissionService.getEmissionPerMeterForRoute(
        feedScopedRouteId
      );
      if (co2EmissionsForRoute.isPresent()) {
        co2Emissions = co2Emissions.plus(
          co2EmissionsForRoute.get().getCo2().multiply(leg.getDistanceMeters())
        );
      } else {
        // Partial results would not give an accurate representation of the emissions.
        return Optional.empty();
      }
    }
    return Optional.ofNullable(co2Emissions);
  }

  private Optional<Gram> calculateCo2EmissionsForCar(List<StreetLeg> carLegs) {
    if (carLegs.isEmpty()) {
      return Optional.empty();
    }
    return emissionService
      .getEmissionPerMeterForCar()
      .map(emissions ->
        new Gram(
          carLegs
            .stream()
            .mapToDouble(leg -> emissions.getCo2().multiply(leg.getDistanceMeters()).asDouble())
            .sum()
        )
      );
  }
}
