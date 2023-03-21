package org.opentripplanner.ext.ridehailing;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter decorates car dropoff/pickup legs with information from ride hailing services and
 * adds information about price and arrival time of the vehicle.
 */
public class RideHailingFilter implements ItineraryListFilter {

  private static final Logger LOG = LoggerFactory.getLogger(RideHailingFilter.class);

  public static final String NO_RIDE_HAILING_AVAILABLE = "no-ride-hailing-available";
  private final List<RideHailingService> rideHailingServices;

  public RideHailingFilter(List<RideHailingService> rideHailingServices) {
    this.rideHailingServices = rideHailingServices;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return rideHailingServices
      .parallelStream()
      .flatMap(service ->
        itineraries.parallelStream().map(i -> addRideHailingInformation(i, service))
      )
      .toList();
  }

  private static void flagForDeletion(Itinerary i) {
    i.flagForDeletion(
      new SystemNotice(
        NO_RIDE_HAILING_AVAILABLE,
        "This itinerary is marked as deleted by the " + NO_RIDE_HAILING_AVAILABLE + " filter."
      )
    );
  }

  private Itinerary addRideHailingInformation(Itinerary i, RideHailingService service) {
    if (!i.isFlaggedForDeletion()) {
      var legs = i
        .getLegs()
        .parallelStream()
        .map(leg -> decorateLegWithRideEstimate(i, leg, service))
        .toList();

      i.setLegs(legs);
    }
    return i;
  }

  private Leg decorateLegWithRideEstimate(Itinerary i, Leg leg, RideHailingService service) {
    try {
      if (leg instanceof StreetLeg sl && sl.getMode().isDriving()) {
        var estimates = service.rideEstimates(leg.getFrom().coordinate, leg.getTo().coordinate);
        if (estimates.isEmpty()) {
          flagForDeletion(i);
          return leg;
        } else {
          return new RideHailingLeg(sl, RideHailingProvider.UBER, estimates.get(0));
        }
      } else {
        return leg;
      }
    } catch (ExecutionException e) {
      LOG.error("Could not get ride hailing estimate for {}", service.provider(), e);
      flagForDeletion(i);
      return leg;
    }
  }
}
