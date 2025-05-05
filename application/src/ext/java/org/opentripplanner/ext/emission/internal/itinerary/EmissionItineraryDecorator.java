package org.opentripplanner.ext.emission.internal.itinerary;

import java.util.ArrayList;
import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * Calculates the emissions for the itineraries and adds them.
 */
@Sandbox
public class EmissionItineraryDecorator implements ItineraryDecorator {

  private final EmissionService emissionService;

  public EmissionItineraryDecorator(EmissionService emissionService) {
    this.emissionService = emissionService;
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    boolean partialResults = false;
    var sum = Emission.ZERO;
    var newLegs = new ArrayList<Leg>();

    for (var l : itinerary.legs()) {
      Emission value;

      if (l instanceof TransitLeg tl) {
        value = calculateCo2EmissionsForTransit(tl);
      } else if (l instanceof StreetLeg sl && sl.getMode() == TraverseMode.CAR) {
        value = calculateCo2EmissionsForCar(sl);
      } else {
        newLegs.add(l);
        continue;
      }

      if (value.isZero()) {
        partialResults = true;
      } else {
        l = l.withEmissionPerPerson(value);
        sum = sum.plus(value);
      }
      newLegs.add(l);
    }

    if (sum.isZero()) {
      return itinerary;
    }
    var builder = itinerary.copyOf();
    builder.withLegs(newLegs);

    if (!partialResults) {
      builder.withEmissionPerPerson(sum);
    }
    return builder.build();
  }

  private Emission calculateCo2EmissionsForTransit(TransitLeg leg) {
    return emissionService.calculateTransitPassengerEmissionForTripHops(
      leg.trip(),
      leg.boardStopPosInPattern(),
      leg.alightStopPosInPattern(),
      leg.distanceMeters()
    );
  }

  private Emission calculateCo2EmissionsForCar(StreetLeg carLeg) {
    return emissionService.calculateCarPassengerEmission(carLeg.distanceMeters());
  }
}
