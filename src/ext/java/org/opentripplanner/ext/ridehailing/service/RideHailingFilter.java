package org.opentripplanner.ext.ridehailing.service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

public class RideHailingFilter implements ItineraryListFilter {

  private final List<RideHailingService> rideHailingServices;

  public RideHailingFilter(List<RideHailingService> rideHailingServices) {
    this.rideHailingServices = rideHailingServices;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.parallelStream().map(this::addCarHailingInformation).toList();
  }

  private Itinerary addCarHailingInformation(Itinerary i) {
    if (!i.isFlaggedForDeletion()) {
      var legs = i
        .getLegs()
        .parallelStream()
        .map(leg -> {
          try {
            if (leg instanceof RideHailingLeg carHailingLeg) {
              var service = findService(carHailingLeg.provider());
              var estimate = service
                .rideEstimates(leg.getFrom().coordinate, leg.getTo().coordinate)
                .get(0);
              return new RideHailingLeg(carHailingLeg, carHailingLeg.provider(), estimate);
            } else if (leg instanceof StreetLeg sl && sl.getMode().isDriving()) {
              var service = findService(RideHailingProvider.UBER);
              var estimate = service
                .rideEstimates(leg.getFrom().coordinate, leg.getTo().coordinate)
                .get(0);
              return new RideHailingLeg(sl, RideHailingProvider.UBER, estimate);
            } else {
              return leg;
            }
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();

      i.setLegs(legs);
    }
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
