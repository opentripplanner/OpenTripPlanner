package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolItineraryCandidate;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.mapping.StreetModeToTransferTraverseModeMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class CarpoolItineraryMapper {

  /**
   * Creates a complete itinerary from A* routing results with proper access/egress legs and carpool legs
   */
  public static Itinerary mapToItinerary(
    RouteRequest request,
    CarpoolItineraryCandidate candidate,
    GraphPath<State, Edge, Vertex> carpoolPath
  ) {
    List<Leg> legs = new ArrayList<>();

    // 1. Access walking leg (origin to pickup)
    Leg accessLeg = accessEgressLeg(
      request.journey().access(),
      candidate.boardingStop(),
      null,
      candidate.trip().getStartTime(),
      "Walk to pickup"
    );
    if (accessLeg != null) {
      legs.add(accessLeg);
    }

    var drivingEndTime = candidate
      .trip()
      .getStartTime()
      .plus(
        Duration.between(
          carpoolPath.states.getFirst().getTime(),
          carpoolPath.states.getLast().getTime()
        )
      );

    // 2. Carpool transit leg (pickup to dropoff)
    CarpoolLeg carpoolLeg = CarpoolLeg.of()
      .withStartTime(candidate.trip().getStartTime())
      .withEndTime(drivingEndTime)
      .withFrom(
        createPlaceFromVertex(
          carpoolPath.states.getFirst().getVertex(),
          "Pickup at " + candidate.trip().getBoardingArea().getName()
        )
      )
      .withTo(
        createPlaceFromVertex(
          carpoolPath.states.getLast().getVertex(),
          "Dropoff at " + candidate.trip().getAlightingArea().getName()
        )
      )
      .withGeometry(GeometryUtils.concatenateLineStrings(carpoolPath.edges, Edge::getGeometry))
      .withDistanceMeters(carpoolPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum())
      .withGeneralizedCost((int) carpoolPath.getWeight())
      .build();
    legs.add(carpoolLeg);

    // 3. Egress walking leg (dropoff to destination)
    Leg egressLeg = accessEgressLeg(
      request.journey().egress(),
      candidate.alightingStop(),
      carpoolLeg.endTime(),
      null,
      "Walk from dropoff"
    );
    if (egressLeg != null) {
      legs.add(egressLeg);
    }

    return Itinerary.ofDirect(legs)
      .withGeneralizedCost(
        Cost.costOfSeconds(
          accessLeg.generalizedCost() + carpoolLeg.generalizedCost() + egressLeg.generalizedCost()
        )
      )
      .build();
  }

  /**
   * Creates a walking leg from a GraphPath with proper geometry and timing.
   * This reuses the same pattern as OTP's GraphPathToItineraryMapper but simplified
   * for carpooling service use.
   */
  @Nullable
  private static Leg accessEgressLeg(
    StreetRequest streetRequest,
    NearbyStop nearbyStop,
    ZonedDateTime legStartTime,
    ZonedDateTime legEndTime,
    String name
  ) {
    if (nearbyStop == null || nearbyStop.edges.isEmpty()) {
      return null;
    }

    var graphPath = new GraphPath<>(nearbyStop.state);

    var firstState = graphPath.states.getFirst();
    var lastState = graphPath.states.getLast();

    List<Edge> edges = nearbyStop.edges;

    if (edges.isEmpty()) {
      return null;
    }

    // Create geometry from edges
    LineString geometry = GeometryUtils.concatenateLineStrings(edges, Edge::getGeometry);

    var legDuration = Duration.between(firstState.getTime(), lastState.getTime());
    if (legStartTime != null && legEndTime == null) {
      legEndTime = legStartTime.plus(legDuration);
    } else if (legEndTime != null && legStartTime == null) {
      legStartTime = legEndTime.minus(legDuration);
    }

    // Build the walking leg
    return StreetLeg.of()
      .withMode(
        StreetModeToTransferTraverseModeMapper.map(
          streetRequest.mode() == StreetMode.NOT_SET ? StreetMode.WALK : streetRequest.mode()
        )
      )
      .withStartTime(legStartTime)
      .withEndTime(legEndTime)
      .withFrom(createPlaceFromVertex(firstState.getVertex(), name + " start"))
      .withTo(createPlaceFromVertex(lastState.getVertex(), name + " end"))
      .withDistanceMeters(nearbyStop.distance)
      .withGeneralizedCost((int) (lastState.getWeight() - firstState.getWeight()))
      .withGeometry(geometry)
      .build();
  }

  /**
   * Creates a Place from a State, similar to GraphPathToItineraryMapper.makePlace
   * but simplified for carpooling service use.
   */
  private static Place createPlaceFromVertex(Vertex vertex, String defaultName) {
    I18NString name = vertex.getName();

    // Use intersection name for street vertices to get better names
    if (vertex instanceof StreetVertex && !(vertex instanceof TemporaryStreetLocation)) {
      name = ((StreetVertex) vertex).getIntersectionName();
    }

    // If no name available, use default
    if (name == null || name.toString().trim().isEmpty()) {
      name = new NonLocalizedString(defaultName);
    }

    return Place.normal(vertex, name);
  }
}
