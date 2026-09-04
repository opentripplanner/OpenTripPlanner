package org.opentripplanner.ext.taxizone.routing;

import java.util.List;
import org.opentripplanner.ext.taxizone.TaxiZoneService;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * Decorates itineraries produced by street routing (direct routing and transit access/egress)
 * with taxi zone information. For each driving-ish {@link StreetLeg} it:
 * <ol>
 *   <li>Looks up which taxi zone provider covers the leg's pickup and drop-off coordinates by
 *   querying the {@link TaxiZoneService}.
 *   <li>If no provider covers both endpoints the itinerary is flagged for deletion.
 *   <li>Otherwise the driving leg is replaced by a {@link TaxiZoneLeg} decorated with the first
 *   matching zone.
 * </ol>
 *
 * <p>TODO: Multi-provider support. Currently only the first matching zone is used. In the future
 * all matching providers should be available so users can choose.
 */
@Sandbox
public class TaxiRouter {

  public static final String NO_TAXI_ZONE_AVAILABLE = "no-taxi-zone-available";

  private final TaxiZoneService taxiZoneService;

  public TaxiRouter(TaxiZoneService taxiZoneService) {
    this.taxiZoneService = taxiZoneService;
  }

  public List<Itinerary> decorate(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::decorateItinerary).toList();
  }

  private Itinerary decorateItinerary(Itinerary itinerary) {
    if (itinerary.isFlaggedForDeletion()) {
      return itinerary;
    }

    ItineraryBuilder builder = itinerary.copyOf();
    var newLegs = builder
      .legs()
      .stream()
      .map(leg -> decorateLeg(itinerary, leg))
      .toList();
    return builder.withLegs(newLegs).build();
  }

  private Leg decorateLeg(Itinerary itinerary, Leg leg) {
    if (!(leg instanceof StreetLeg streetLeg) || !streetLeg.getMode().isDrivingIsh()) {
      return leg;
    }

    var taxiZoneOptional = taxiZoneService.findZone(
      streetLeg.from().coordinate,
      streetLeg.to().coordinate,
      streetLeg.startTime().toLocalDate()
    );
    if (taxiZoneOptional.isEmpty()) {
      flagForDeletion(itinerary);
      return leg;
    }

    return new TaxiZoneLeg(streetLeg, taxiZoneOptional.get());
  }

  private static void flagForDeletion(Itinerary itinerary) {
    if (!itinerary.hasSystemNoticeTag(NO_TAXI_ZONE_AVAILABLE)) {
      itinerary.flagForDeletion(
        new SystemNotice(
          NO_TAXI_ZONE_AVAILABLE,
          "This itinerary is marked as deleted by the " + NO_TAXI_ZONE_AVAILABLE + " filter."
        )
      );
    }
  }
}
