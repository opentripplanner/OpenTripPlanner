package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Maps carpooling insertion candidates to OTP itineraries for API responses.
 * <p>
 * This mapper bridges between the carpooling domain model ({@link InsertionCandidate}) and
 * OTP's standard itinerary model ({@link Itinerary}). It extracts the passenger's journey
 * portion from the complete driver route and constructs an itinerary with timing, geometry,
 * and cost information.
 *
 * <h2>Mapping Strategy</h2>
 * <p>
 * An {@link InsertionCandidate} contains:
 * <ul>
 *   <li><strong>Pickup segments:</strong> Driver's route from start to passenger pickup</li>
 *   <li><strong>Shared segments:</strong> Passenger's journey from pickup to dropoff</li>
 *   <li><strong>Dropoff segments:</strong> Driver's route from dropoff to end</li>
 * </ul>
 * <p>
 * This mapper focuses on the <strong>shared segments</strong>, which represent the passenger's
 * actual carpool ride. The pickup segments are used only to calculate when the driver arrives
 * at the pickup location.
 *
 * <h2>Time Calculation</h2>
 * <p>
 * The passenger's start time is the later of:
 * <ol>
 *   <li>The passenger's requested departure time</li>
 *   <li>When the driver arrives at the pickup location</li>
 * </ol>
 * <p>
 * This ensures the itinerary reflects realistic timing: passengers can't board before the
 * driver arrives, but they also won't board earlier than they wanted to depart.
 *
 * <h2>Geometry and Cost</h2>
 * <p>
 * The itinerary includes:
 * <ul>
 *   <li><strong>Geometry:</strong> Concatenated line strings from all shared route edges</li>
 *   <li><strong>Distance:</strong> Sum of all shared segment edge distances</li>
 *   <li><strong>Generalized cost:</strong> A* path weight from routing (time + penalties)</li>
 * </ul>
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

  private final ZoneId timeZone;

  /**
   * Creates a new carpool itinerary mapper with the specified timezone.
   * <p>
   * The timezone is used to convert passenger requested departure times from Instant to
   * ZonedDateTime for comparison with driver pickup times.
   * <p>
   * @param timeZone the timezone for time conversions, typically from TransitService.getTimeZone()
   */
  public CarpoolItineraryMapper(ZoneId timeZone) {
    this.timeZone = ZoneIdFallback.zoneId(timeZone);
  }

  /**
   * Converts an insertion candidate into an OTP itinerary representing the passenger's journey.
   * <p>
   * Extracts the passenger's portion of the journey (shared segments) and constructs an itinerary
   * with accurate timing, geometry, and cost information. The resulting itinerary contains a
   * single {@link CarpoolLeg} representing the ride from pickup to dropoff.
   *
   * <h3>Time Calculation Details</h3>
   * <p>
   * The method calculates three key times:
   * <ol>
   *   <li><strong>Driver pickup arrival:</strong> Driver's start time + pickup segment durations</li>
   *   <li><strong>Passenger start:</strong> max(requested time, driver arrival time)</li>
   *   <li><strong>Passenger end:</strong> start time + shared segment durations</li>
   * </ol>
   *
   * <h3>Null Return Cases</h3>
   * <p>
   * Returns {@code null} if the candidate has no shared segments, which should never happen
   * for valid insertion candidates but serves as a safety check.
   *
   * @param request the original routing request containing passenger preferences and timing
   * @param candidate the insertion candidate containing route segments and trip details
   * @return an itinerary with a single carpool leg, or null if shared segments are empty
   *         (should not occur for valid candidates)
   */
  @Nullable
  public Itinerary toItinerary(RouteRequest request, InsertionCandidate candidate) {
    var sharedSegments = candidate.getSharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }

    var pickupSegments = candidate.getPickupSegments();
    Duration pickupDuration = Duration.ZERO;
    for (var segment : pickupSegments) {
      pickupDuration = pickupDuration.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    var driverPickupTime = candidate.trip().startTime().plus(pickupDuration);

    var startTime = request.dateTime().isAfter(driverPickupTime.toInstant())
      ? request.dateTime().atZone(timeZone)
      : driverPickupTime;

    // Calculate shared journey duration
    Duration carpoolDuration = Duration.ZERO;
    for (var segment : sharedSegments) {
      carpoolDuration = carpoolDuration.plus(
        Duration.between(segment.states.getFirst().getTime(), segment.states.getLast().getTime())
      );
    }

    var endTime = startTime.plus(carpoolDuration);

    var firstSegment = sharedSegments.getFirst();
    var lastSegment = sharedSegments.getLast();

    Vertex fromVertex = firstSegment.states.getFirst().getVertex();
    Vertex toVertex = lastSegment.states.getLast().getVertex();

    var allEdges = sharedSegments.stream().flatMap(seg -> seg.edges.stream()).toList();

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
