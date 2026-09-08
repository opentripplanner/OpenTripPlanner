package org.opentripplanner.ext.realtimeresolver;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.refetch.RefetchItineraryService;

public class RealtimeResolver {

  private final RefetchItineraryService refetchItineraryService;

  public RealtimeResolver(RefetchItineraryService refetchItineraryService) {
    this.refetchItineraryService = refetchItineraryService;
  }

  /**
   * Loop through all itineraries and populate legs with real-time data using legReference from the
   * original leg
   */
  public static List<Itinerary> populateLegsWithRealtime(
    List<Itinerary> itineraries,
    RefetchItineraryService refetchItineraryService,
    RouteRequest routeRequest
  ) {
    return new RealtimeResolver(refetchItineraryService).addRealtimeInfo(itineraries, routeRequest);
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

    return refetchItineraryService.refetchItinerary(
      fromLocation,
      toLocation,
      legReferences,
      routeRequest
    );
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
