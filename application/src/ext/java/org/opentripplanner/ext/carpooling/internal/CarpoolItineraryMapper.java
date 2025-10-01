package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.edge.Edge;

public class CarpoolItineraryMapper {

  /**
   * Creates an itinerary from a viable via-route carpool candidate.
   * This uses the shared route segment as the main carpool leg.
   */
  public static Itinerary mapViaRouteToItinerary(
    RouteRequest request,
    ViaCarpoolCandidate candidate
  ) {
    var pickupDuration = Duration.between(
      candidate.pickupRoute().states.getFirst().getTime(),
      candidate.pickupRoute().states.getLast().getTime()
    );

    var driverPickupTime = candidate.trip().startTime().plus(pickupDuration);

    // Main carpool leg (passenger origin to destination via shared route)
    // Start time is max of request dateTime and driverPickupTime
    var startTime = request.dateTime().isAfter(driverPickupTime.toInstant())
      ? request.dateTime().atZone(ZoneId.of("Europe/Oslo"))
      : driverPickupTime;

    var carpoolDuration = Duration.between(
      candidate.sharedRoute().states.getFirst().getTime(),
      candidate.sharedRoute().states.getLast().getTime()
    );

    var endTime = startTime.plus(carpoolDuration);

    var fromVertex = candidate.passengerOrigin().iterator().next();
    var toVertex = candidate.passengerDestination().iterator().next();

    CarpoolLeg carpoolLeg = CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(Place.normal(fromVertex, new NonLocalizedString("Carpool boarding")))
      .withTo(Place.normal(toVertex, new NonLocalizedString("Carpool alighting")))
      .withGeometry(
        GeometryUtils.concatenateLineStrings(candidate.sharedRoute().edges, Edge::getGeometry)
      )
      .withDistanceMeters(
        candidate.sharedRoute().edges.stream().mapToDouble(Edge::getDistanceMeters).sum()
      )
      .withGeneralizedCost((int) candidate.sharedRoute().getWeight())
      .build();

    return Itinerary.ofDirect(List.of(carpoolLeg))
      .withGeneralizedCost(Cost.costOfSeconds(carpoolLeg.generalizedCost()))
      .build();
  }
}
