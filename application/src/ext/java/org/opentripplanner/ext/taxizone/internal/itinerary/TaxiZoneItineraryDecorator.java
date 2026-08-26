package org.opentripplanner.ext.taxizone.internal.itinerary;

import java.util.List;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * Filter that operates on car pickup itineraries. For each driving-ish {@link StreetLeg} it:
 * <ol>
 *   <li>Looks up which flex trip covers the leg's pickup and drop-off coordinates by
 *   querying the {@link TaxiZoneIndex}.
 *   <li>If no provider covers both endpoints the itinerary is flagged for deletion.
 *   <li>Otherwise the driving leg is replaced by a {@link TaxiZoneLeg} decorated with the
 *   first matching flex trip.
 * </ol>
 *
 * <p>TODO: Multi-provider support. Currently only the first matching zone is used.
 * In the future all matching providers should be available so users can choose.
 */
@Sandbox
public class TaxiZoneItineraryDecorator implements ItineraryListFilter {

  public static final String NO_TAXI_ZONE_AVAILABLE = "no-taxi-zone-available";

  private final TaxiZoneIndex zoneIndex;

  public TaxiZoneItineraryDecorator(TaxiZoneIndex zoneIndex) {
    this.zoneIndex = zoneIndex;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
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

    var taxiZoneOptional = zoneIndex.findFirstZone(
      streetLeg.from().coordinate,
      streetLeg.to().coordinate
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
