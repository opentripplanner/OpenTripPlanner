package org.opentripplanner.ext.emission.internal.itinerary;

import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
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
    var sum = Emission.ZERO;

    for (var l : itinerary.legs()) {
      Emission value;
      if (l instanceof TransitLeg tl) {
        value = calculateCo2EmissionsForTransit(tl);
      } else if (l instanceof StreetLeg sl && sl.getMode() == TraverseMode.CAR) {
        value = calculateCo2EmissionsForCar(sl);
      } else {
        continue;
      }

      // Note! Partial results would not give an accurate representation of the emissions, not all
      // legs have emissions. This should be fixed by setting the emission on each leg and mark
      // legs witch have UNKNOWN emissions.
      if (value.isZero()) {
        return itinerary;
      }
      sum = sum.plus(value);
    }
    return sum.isZero() ? itinerary : itinerary.copyOf().withEmissionPerPerson(sum).build();
  }

  private Emission calculateCo2EmissionsForTransit(TransitLeg leg) {
    return emissionService.calculateTransitPassengerEmissionForTripLeg(
      leg.getTrip(),
      leg.getBoardStopPosInPattern(),
      leg.getAlightStopPosInPattern(),
      leg.getDistanceMeters()
    );
  }

  private Emission calculateCo2EmissionsForCar(StreetLeg carLeg) {
    return emissionService.calculateCarPassengerEmission(carLeg.getDistanceMeters());
  }
}
