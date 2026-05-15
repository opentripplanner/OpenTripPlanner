package org.opentripplanner.ext.carpooling.internal;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/**
 * Maps carpooling insertion candidates to OTP itineraries for API responses.
 * <p>
 * This mapper bridges between the carpooling domain model ({@link InsertionCandidate}) and
 * OTP's standard itinerary model ({@link Itinerary}). It extracts the passenger's journey
 * portion from the complete driver route and constructs an itinerary with timing, geometry,
 * and cost information.
 *
 * <h2>Itinerary Shape</h2>
 * <p>
 * Both direct-mode and access/egress itineraries have between one and three legs, emitted in
 * chronological order:
 * <ol>
 *   <li>An optional leading {@link StreetLeg} (WALK) from the passenger-side location (origin
 *       for direct/access, transit stop for egress) to the snapped carpool pickup vertex.</li>
 *   <li>A {@link CarpoolLeg} covering the shared ride between pickup and dropoff. The
 *       boarding dwell at the pickup is included in this leg's duration, not added before it.</li>
 *   <li>An optional trailing {@link StreetLeg} (WALK) from the snapped dropoff vertex to the
 *       dropoff-side location (destination for direct/egress, transit stop for access).</li>
 * </ol>
 *
 * <h2>Time Calculation</h2>
 * <p>
 * The carpool leg's start time is the moment the driver arrives at the pickup
 * (trip start + pickup travel). If a leading walk leg exists, its start is back-shifted by the
 * walk's duration; the trailing walk leg's end is forward-shifted similarly. The itinerary is
 * <em>not</em> shifted to match the passenger's requested departure time — the driver is on a
 * committed schedule and cannot wait. Whether the passenger should show up early or be matched
 * at all is a filtering concern upstream of this mapper.
 *
 * <h2>Package Location</h2>
 * <p>
 * This class is in the {@code internal} package because it's an implementation detail of
 * the carpooling service. API consumers interact with {@link Itinerary} objects, not this mapper.
 *
 * @see InsertionCandidate for the source data structure
 * @see CarpoolLeg for the carpool-specific leg type
 * @see Itinerary for the OTP itinerary model
 */
public class CarpoolItineraryMapper {

  /**
   * Converts an insertion candidate into an OTP itinerary representing the passenger's journey.
   * The itinerary contains a {@link CarpoolLeg} for the shared ride, optionally preceded by a
   * WALK {@link StreetLeg} from the passenger's origin to the snapped pickup vertex and
   * optionally followed by a WALK leg from the snapped dropoff vertex to the destination.
   *
   * @param candidate the insertion candidate containing route segments, trip details, and
   *        optional walk paths around the carpool pickup/dropoff
   * @param carpoolReluctance multiplier applied to ride seconds when computing the carpool leg's
   *        {@code generalizedCost}.
   * @return an itinerary for the passenger's journey, or {@code null} if the candidate has no
   *         shared segments (a safety check that should not trigger for valid candidates)
   */
  @Nullable
  public Itinerary toItinerary(InsertionCandidate candidate, double carpoolReluctance) {
    var sharedSegments = candidate.getSharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }
    var carpoolStart = candidate.trip().startTime().plus(candidate.getDurationUntilPickupArrival());
    var carpoolEnd = carpoolStart.plus(candidate.getPassengerRideDuration());
    return buildItinerary(
      sharedSegments,
      candidate.walkToPickup(),
      candidate.walkFromDropoff(),
      carpoolStart,
      carpoolEnd,
      candidate.getPassengerRideWeight(carpoolReluctance)
    );
  }

  private static CarpoolLeg buildCarpoolLeg(
    List<GraphPath<State, Edge, Vertex>> sharedSegments,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    double rideWeight
  ) {
    var firstSegment = sharedSegments.getFirst();
    var lastSegment = sharedSegments.getLast();

    Vertex fromVertex = firstSegment.states.getFirst().getVertex();
    Vertex toVertex = lastSegment.states.getLast().getVertex();

    var allEdges = sharedSegments
      .stream()
      .flatMap(seg -> seg.edges.stream())
      .toList();

    return CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(Place.normal(fromVertex, new NonLocalizedString("Carpool boarding")))
      .withTo(Place.normal(toVertex, new NonLocalizedString("Carpool alighting")))
      .withGeometry(GeometryUtils.concatenateLineStrings(allEdges, Edge::getGeometry))
      .withDistanceMeters(allEdges.stream().mapToDouble(Edge::getDistanceMeters).sum())
      .withGeneralizedCost((int) rideWeight)
      .build();
  }

  private static StreetLeg buildWalkLeg(
    GraphPath<State, Edge, Vertex> walkPath,
    ZonedDateTime startTime,
    ZonedDateTime endTime
  ) {
    Vertex fromVertex = walkPath.states.getFirst().getVertex();
    Vertex toVertex = walkPath.states.getLast().getVertex();

    LineString geometry = GeometryUtils.concatenateLineStrings(walkPath.edges, Edge::getGeometry);
    double distance = walkPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();

    return StreetLeg.of()
      .withMode(TraverseMode.WALK)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(Place.normal(fromVertex, fromVertex.getName()))
      .withTo(Place.normal(toVertex, toVertex.getName()))
      .withGeometry(geometry)
      .withDistanceMeters(distance)
      .withGeneralizedCost((int) walkPath.getWeight())
      .build();
  }

  /**
   * Converts a Raptor carpool access/egress leg back into an OTP itinerary for the response. Same
   * shape as {@link #toItinerary(InsertionCandidate, double)} — an optional WALK leg, the carpool
   * leg, and an optional WALK leg — but driven from the {@link CarpoolAccessEgress} accessors so
   * the displayed times and ride cost agree with the values Raptor used during the search.
   *
   * @return the itinerary, or {@code null} if the access/egress has no shared segments (a safety
   *         check that should not trigger for valid legs)
   */
  @Nullable
  public Itinerary toItinerary(CarpoolAccessEgress accessEgress) {
    var sharedSegments = accessEgress.sharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }
    return buildItinerary(
      sharedSegments,
      accessEgress.walkToPickup(),
      accessEgress.walkFromDropoff(),
      accessEgress.getCarpoolStart(),
      accessEgress.getCarpoolEnd(),
      accessEgress.getPassengerRideWeight()
    );
  }

  /**
   * Assembles a WALK + CARPOOL + WALK itinerary around the shared ride. Walk legs are omitted
   * when their corresponding path is {@code null}; the walk legs' outer times are derived from
   * {@code carpoolStart}/{@code carpoolEnd} plus the walk durations.
   */
  private static Itinerary buildItinerary(
    List<GraphPath<State, Edge, Vertex>> sharedSegments,
    @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
    @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff,
    ZonedDateTime carpoolStart,
    ZonedDateTime carpoolEnd,
    double rideWeight
  ) {
    List<Leg> legs = new ArrayList<>(3);
    if (walkToPickup != null) {
      var walkStart = carpoolStart.minusSeconds(walkToPickup.getDuration());
      legs.add(buildWalkLeg(walkToPickup, walkStart, carpoolStart));
    }
    legs.add(buildCarpoolLeg(sharedSegments, carpoolStart, carpoolEnd, rideWeight));
    if (walkFromDropoff != null) {
      var walkEnd = carpoolEnd.plusSeconds(walkFromDropoff.getDuration());
      legs.add(buildWalkLeg(walkFromDropoff, carpoolEnd, walkEnd));
    }

    int totalCost = legs.stream().mapToInt(Leg::generalizedCost).sum();
    return Itinerary.ofDirect(legs).withGeneralizedCost(Cost.costOfSeconds(totalCost)).build();
  }
}
