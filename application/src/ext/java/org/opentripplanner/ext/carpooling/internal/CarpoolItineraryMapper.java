package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;

public class CarpoolItineraryMapper {

  /**
   * Creates an itinerary from an insertion candidate (refactored version).
   * Works with the new InsertionCandidate type from the refactored routing system.
   */
  public Itinerary toItinerary(RouteRequest request, InsertionCandidate candidate) {
    // Get shared route segments (passenger pickup to dropoff)
    var sharedSegments = candidate.getSharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }

    // Calculate times
    var pickupSegments = candidate.getPickupSegments();
    Duration pickupDuration = Duration.ZERO;
    for (var segment : pickupSegments) {
      pickupDuration = pickupDuration.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    var driverPickupTime = candidate.trip().startTime().plus(pickupDuration);

    // Passenger start time is max of request time and when driver arrives
    var startTime = request.dateTime().isAfter(driverPickupTime.toInstant())
      ? request.dateTime().atZone(ZoneId.of("Europe/Oslo"))
      : driverPickupTime;

    // Calculate shared journey duration
    Duration carpoolDuration = Duration.ZERO;
    for (var segment : sharedSegments) {
      carpoolDuration = carpoolDuration.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    var endTime = startTime.plus(carpoolDuration);

    // Get vertices from first and last segment
    var firstSegment = sharedSegments.get(0);
    var lastSegment = sharedSegments.get(sharedSegments.size() - 1);

    Vertex fromVertex = firstSegment.states.getFirst().getVertex();
    Vertex toVertex = lastSegment.states.getLast().getVertex();

    // Collect all edges from shared segments
    var allEdges = sharedSegments.stream().flatMap(seg -> seg.edges.stream()).toList();

    // Create carpool leg
    CarpoolLeg carpoolLeg = CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(Place.normal(fromVertex, new NonLocalizedString("Carpool boarding")))
      .withTo(Place.normal(toVertex, new NonLocalizedString("Carpool alighting")))
      .withGeometry(GeometryUtils.concatenateLineStrings(allEdges, Edge::getGeometry))
      .withDistanceMeters(allEdges.stream().mapToDouble(Edge::getDistanceMeters).sum())
      .withGeneralizedCost((int) lastSegment.getWeight())
      .build();

    return Itinerary.ofDirect(List.of(carpoolLeg))
      .withGeneralizedCost(Cost.costOfSeconds(carpoolLeg.generalizedCost()))
      .build();
  }
}
