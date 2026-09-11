package org.opentripplanner.ext.realtimeresolver;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.refetch.RefetchItineraryService;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TransitService;

public class RealtimeResolver {

  private final RefetchItineraryService refetchItineraryService;
  private final TransitService transitService;
  private final TransitAlertService transitAlertService;

  public RealtimeResolver(
    RefetchItineraryService refetchItineraryService,
    TransitService transitService,
    TransitAlertService transitAlertService
  ) {
    this.refetchItineraryService = refetchItineraryService;
    this.transitService = transitService;
    this.transitAlertService = transitAlertService;
  }

  /**
   * Loop through all itineraries and populate legs with real-time data using legReference from the
   * original leg
   */
  public static List<Itinerary> populateLegsWithRealtime(
    List<Itinerary> itineraries,
    RefetchItineraryService refetchItineraryService,
    TransitService transitService,
    TransitAlertService transitAlertService,
    RouteRequest routeRequest
  ) {
    return new RealtimeResolver(
      refetchItineraryService,
      transitService,
      transitAlertService
    ).addRealtimeInfo(itineraries, routeRequest);
  }

  public List<Itinerary> addRealtimeInfo(List<Itinerary> itineraries, RouteRequest routeRequest) {
    return itineraries
      .stream()
      .map(o -> decorateItinerary(o, routeRequest))
      .toList();
  }

  private Itinerary decorateItinerary(Itinerary itinerary, RouteRequest routeRequest) {
    if (itinerary.isFlaggedForDeletion()) {
      return itinerary;
    }

    List<LegReference> legReferences = itinerary
      .legs()
      .stream()
      .map(Leg::legReference)
      .filter(Objects::nonNull)
      .toList();

    GenericLocation fromLocation = getStreetLocation(itinerary.legs().getFirst(), true);
    GenericLocation toLocation = getStreetLocation(itinerary.legs().getLast(), false);

    boolean hasChanged = hasItineraryStopsChanged(itinerary);

    if (hasChanged) {
      return refetchItineraryService.refetchItinerary(
        fromLocation,
        toLocation,
        legReferences,
        routeRequest
      );
    }
    return itinerary.copyOf().transformLegs(this::mapLeg).build();
  }

  private static boolean hasItineraryStopsChanged(Itinerary itinerary) {
    var legs = itinerary.legs();

    return IntStream.range(1, legs.size()).anyMatch(i -> {
      var legA = legs.get(i - 1);
      var legB = legs.get(i);

      return legA.isTransitLeg() && !Objects.equals(legA.to().name, legB.from().name);
    });
  }

  private Leg mapLeg(Leg leg) {
    var ref = leg.legReference();
    if (ref == null) {
      return leg;
    }

    // Only ScheduledTransitLeg has leg references atm, so this check is just to be future-proof
    if (!leg.isScheduledTransitLeg()) {
      return leg;
    }
    var realTimeLeg = ref.getLeg(transitService, transitAlertService);
    if (realTimeLeg == null) {
      return leg;
    }
    return combineReferenceWithOriginal(
      realTimeLeg.asScheduledTransitLeg(),
      leg.asScheduledTransitLeg()
    );
  }

  private static Leg combineReferenceWithOriginal(
    ScheduledTransitLeg reference,
    ScheduledTransitLeg original
  ) {
    return new ScheduledTransitLegBuilder<>(reference)
      .withTransferFromPreviousLeg(original.transferFromPrevLeg())
      .withTransferToNextLeg(original.transferToNextLeg())
      .withGeneralizedCost(original.generalizedCost())
      .withAccessibilityScore(original.accessibilityScore())
      .build();
  }

  @Nullable
  private GenericLocation getStreetLocation(Leg leg, boolean from) {
    if (!leg.isStreetLeg()) {
      return null;
    }

    var place = from ? leg.from() : leg.to();

    if (place.stop != null) {
      return GenericLocation.fromCoordinate(place.stop.getCoordinate());
    }

    return GenericLocation.fromCoordinate(
      place.coordinate.latitude(),
      place.coordinate.longitude()
    );
  }
}
