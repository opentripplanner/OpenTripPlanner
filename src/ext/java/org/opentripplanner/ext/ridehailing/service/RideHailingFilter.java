package org.opentripplanner.ext.ridehailing.service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

public class RideHailingFilter implements ItineraryListFilter {

  private final List<RideHailingService> rideHailingServices;

  public RideHailingFilter(List<RideHailingService> rideHailingServices) {
    this.rideHailingServices = rideHailingServices;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::addCarHailingInformation).toList();
  }

  private Itinerary addCarHailingInformation(Itinerary i) {
    var legs = i
      .getLegs()
      .stream()
      .map(leg -> {
        if (leg instanceof RideHailingLeg carHailingLeg) {
          try {
            var service = findService(carHailingLeg.provider());
            var estimate = service
              .rideEstimates(leg.getFrom().coordinate, leg.getTo().coordinate)
              .get(0);
            return new RideHailingLeg(carHailingLeg, carHailingLeg.provider(), estimate);
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }
        } else {
          return leg;
        }
      })
      .toList();

    i.setLegs(legs);
    return i;
  }

  private RideHailingService findService(RideHailingProvider provider) {
    return rideHailingServices
      .stream()
      .filter(s -> s.carHailingCompany().equals(provider))
      .findAny()
      .orElseThrow();
  }
}
