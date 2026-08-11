package org.opentripplanner.ext.carpickupzone.internal.itinerary;

import java.util.List;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneIndex;
import org.opentripplanner.ext.carpickupzone.model.CarPickupZoneLeg;
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
 *   querying the {@link CarPickupZoneIndex}.
 *   <li>If no provider covers both endpoints the itinerary is flagged for deletion.
 *   <li>Otherwise the driving leg is replaced by a {@link CarPickupZoneLeg} decorated with the
 *   first matching flex trip.
 * </ol>
 *
 * <p>TODO: Multi-provider support. Currently only the first matching zone is used.
 * In the future all matching providers should be available so users can choose.
 */
@Sandbox
public class CarPickupZoneItineraryDecorator implements ItineraryListFilter {

  public static final String NO_CAR_PICKUP_ZONE_AVAILABLE = "no-car-pickup-zone-available";

  private final CarPickupZoneIndex zoneIndex;

  public CarPickupZoneItineraryDecorator(CarPickupZoneIndex zoneIndex) {
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

    var carPickupZoneOptional = zoneIndex.findFirstZone(
      streetLeg.from().coordinate,
      streetLeg.to().coordinate
    );
    if (carPickupZoneOptional.isEmpty()) {
      flagForDeletion(itinerary);
      return leg;
    }

    return new CarPickupZoneLeg(streetLeg, carPickupZoneOptional.get());
  }

  private static void flagForDeletion(Itinerary itinerary) {
    itinerary.flagForDeletion(
      new SystemNotice(
        NO_CAR_PICKUP_ZONE_AVAILABLE,
        "Itinerary removed: car leg does not fall within any configured car pickup zones."
      )
    );
  }
}
