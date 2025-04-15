package org.opentripplanner.ext.ridehailing;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter decorates car dropoff/pickup legs with information from ride hailing services and
 * adds information about the price and arrival time of the vehicle.
 */
public class DecorateWithRideHailing implements ItineraryListFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DecorateWithRideHailing.class);

  public static final String NO_RIDE_HAILING_AVAILABLE = "no-ride-hailing-available";
  private final List<RideHailingService> rideHailingServices;
  private final boolean wheelchairAccessible;

  public DecorateWithRideHailing(
    List<RideHailingService> rideHailingServices,
    boolean wheelchairAccessible
  ) {
    this.rideHailingServices = rideHailingServices;
    this.wheelchairAccessible = wheelchairAccessible;
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
      ItineraryBuilder builder = i.copyOf();
      var legs = builder
        .legs()
        .parallelStream()
        .map(leg -> decorateLegWithRideEstimate(i, leg, service))
        .toList();
      return builder.withLegs(ignore -> legs).build();
    }
    return i;
  }

  private Leg decorateLegWithRideEstimate(Itinerary i, Leg leg, RideHailingService service) {
    try {
      if (leg instanceof StreetLeg sl && sl.getMode().isInCar()) {
        var estimates = service.rideEstimates(
          leg.getFrom().coordinate,
          leg.getTo().coordinate,
          wheelchairAccessible
        );
        if (estimates.isEmpty()) {
          flagForDeletion(i);
          return leg;
        } else {
          return new RideHailingLeg(sl, service.provider(), estimates.get(0));
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
