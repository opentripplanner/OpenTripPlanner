package org.opentripplanner.ext.carpooling.updater;

import static org.opentripplanner.ext.carpooling.model.CarpoolStop.DEFAULT_ONBOARD_COUNT;
import static org.opentripplanner.ext.carpooling.model.CarpoolTrip.DEFAULT_TOTAL_CAPACITY;

import java.math.BigInteger;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.opengis.gml.siri.LinearRingType;
import net.opengis.gml.siri.PolygonType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.AimedFlexibleArea;
import uk.org.siri.siri21.CircularAreaStructure;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;

/**
 * Maps SIRI EstimatedVehicleJourney messages to {@link CarpoolTrip} instances.
 * Extracts stop geometry, timing, capacity and occupancy from the SIRI data.
 */
public class CarpoolSiriMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolSiriMapper.class);
  private static final String FEED_ID = "ENT";
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Returns the trip id for the given journey.
   */
  FeedScopedId tripId(EstimatedVehicleJourney journey) {
    return new FeedScopedId(FEED_ID, journey.getEstimatedVehicleJourneyCode());
  }

  private static boolean isCancelled(EstimatedCall call) {
    return Boolean.TRUE.equals(call.isCancellation());
  }

  /**
   * Maps a SIRI {@link EstimatedVehicleJourney} to a {@link CarpoolTrip}. Calls flagged as
   * cancelled are filtered out before stops are built. Returns {@code null} when fewer than
   * 2 non-cancelled calls remain.
   *
   * @throws IllegalArgumentException if the raw message is malformed (fewer than 2 calls
   *         before filtering, calls out of order, missing flexible areas, etc.)
   */
  @Nullable
  public CarpoolTrip mapSiriToCarpoolTrip(EstimatedVehicleJourney journey) {
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    if (calls.size() < 2) {
      throw new IllegalArgumentException(
        "Carpool trips must have at least 2 stops (origin and destination)."
      );
    }

    var tripId = journey.getEstimatedVehicleJourneyCode();
    var activeCalls = calls
      .stream()
      .filter(c -> !isCancelled(c))
      .toList();
    if (activeCalls.size() < 2) {
      LOG.info(
        "Trip {}: fewer than 2 non-cancelled calls remain ({} of {}), treating as cancellation",
        tripId,
        activeCalls.size(),
        calls.size()
      );
      return null;
    }

    validateEstimatedCallOrder(activeCalls);

    List<CarpoolStop> stops = new ArrayList<>();

    for (int i = 0; i < activeCalls.size(); i++) {
      EstimatedCall call = activeCalls.get(i);
      boolean isFirst = (i == 0);
      boolean isLast = (i == activeCalls.size() - 1);

      var stop = buildCarpoolStopForPosition(call, tripId, i, isFirst, isLast);
      stops.add(stop);
    }

    // Extract start/end times from first/last stops
    var firstStop = stops.getFirst();
    var lastStop = stops.getLast();

    var startTime = firstStop.getExpectedDepartureTime() != null
      ? firstStop.getExpectedDepartureTime()
      : firstStop.getAimedDepartureTime();

    var endTime = lastStop.getExpectedArrivalTime() != null
      ? lastStop.getExpectedArrivalTime()
      : lastStop.getAimedArrivalTime();

    int totalCapacity = extractTotalCapacity(tripId, activeCalls);

    var builder = new CarpoolTripBuilder(new FeedScopedId(FEED_ID, tripId))
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withProvider(journey.getOperatorRef().getValue())
      .withTotalCapacity(totalCapacity)
      .withStops(stops);

    var publicContact = journey.getPublicContact();
    if (publicContact != null) {
      builder.withPublicContactInformation(
        ContactInfo.of()
          .withPhoneNumber(publicContact.getPhoneNumber())
          .withBookingUrl(publicContact.getUrl())
          .build()
      );
    }

    return builder.build();
  }

  /**
   * Build a CarpoolStop from an EstimatedCall with special handling for first/last positions.
   *
   * @param call The SIRI EstimatedCall containing stop information
   * @param tripId The trip ID for generating unique stop IDs
   * @param stopIndex The 0-based index of this stop in the call list
   * @param isFirst true if this is the first stop (origin)
   * @param isLast true if this is the last stop (destination)
   * @return A CarpoolStop representing the stop
   */
  private CarpoolStop buildCarpoolStopForPosition(
    EstimatedCall call,
    String tripId,
    int stopIndex,
    boolean isFirst,
    boolean isLast
  ) {
    var stopId = isFirst
      ? tripId + "_trip_origin"
      : isLast
        ? tripId + "_trip_destination"
        : tripId + "_stop_" + stopIndex;

    return toCarpoolStop(call, stopId, tripId, isFirst, isLast);
  }

  /**
   * Extracts the total capacity from the EstimatedCalls' ExpectedDepartureCapacities.
   * Expects the cancelled calls to have already been filtered out. Only the first element
   * of each call's capacities list is inspected; additional entries are ignored. Uses the
   * value from the first call that has it. Logs a warning if calls report different capacity
   * values. Returns {@link CarpoolTrip#DEFAULT_TOTAL_CAPACITY} if no call has capacity data
   * or if the value is invalid.
   */
  private int extractTotalCapacity(String tripId, List<EstimatedCall> calls) {
    Integer firstCapacity = null;
    int firstCapacityIndex = -1;

    for (int i = 0; i < calls.size(); i++) {
      var capacities = calls.get(i).getExpectedDepartureCapacities();
      if (capacities == null || capacities.isEmpty()) {
        continue;
      }
      BigInteger value = capacities.getFirst().getTotalCapacity();
      if (value == null) {
        continue;
      }
      int intValue = value.intValue();
      if (firstCapacity == null) {
        firstCapacity = intValue;
        firstCapacityIndex = i;
      } else if (intValue != firstCapacity) {
        LOG.warn(
          "Trip {}: totalCapacity differs between calls (call {} has {}, call {} has {})",
          tripId,
          firstCapacityIndex,
          firstCapacity,
          i,
          intValue
        );
      }
    }

    if (firstCapacity == null) {
      return DEFAULT_TOTAL_CAPACITY;
    }
    if (firstCapacity <= 0) {
      LOG.warn(
        "Trip {}: invalid totalCapacity {} at call {}, using default {}",
        tripId,
        firstCapacity,
        firstCapacityIndex,
        DEFAULT_TOTAL_CAPACITY
      );
      return DEFAULT_TOTAL_CAPACITY;
    }
    return firstCapacity;
  }

  /**
   * Extracts the onboard count from the EstimatedCall's ExpectedDepartureOccupancies.
   * Only the first element of the occupancies list is inspected; additional entries are
   * ignored. Returns {@link CarpoolStop#DEFAULT_ONBOARD_COUNT} if not present or if the value is invalid.
   */
  private int extractOnboardCount(String tripId, EstimatedCall call) {
    var occupancies = call.getExpectedDepartureOccupancies();
    if (occupancies != null && !occupancies.isEmpty()) {
      BigInteger onboardCount = occupancies.getFirst().getOnboardCount();
      if (onboardCount != null) {
        int value = onboardCount.intValue();
        if (value <= 0) {
          LOG.warn(
            "Trip {}: invalid onboardCount {}, using default {}",
            tripId,
            value,
            DEFAULT_ONBOARD_COUNT
          );
          return DEFAULT_ONBOARD_COUNT;
        }
        return value;
      }
    }
    return DEFAULT_ONBOARD_COUNT;
  }

  /**
   * Extracts the deviation budget from the EstimatedCall by computing the difference between
   * {@code latestExpectedArrivalTime} and the arrival time ({@code expectedArrivalTime} if
   * present, otherwise {@code aimedArrivalTime}).
   * <p>
   * The result is the <em>remaining</em> slack at this stop, not an initial contract:
   * {@code expectedArrivalTime} already reflects detours committed by prior passenger
   * insertions, and {@code latestExpectedArrivalTime} is the unchanged commitment to the
   * passenger at this stop. Each time this mapper runs against a fresh SIRI snapshot, the
   * extracted value therefore shrinks in step with the consumed slack.
   * <p>
   * Fallbacks:
   * <ul>
   *   <li>Returns {@link CarpoolStop#DEFAULT_DEVIATION_BUDGET} if either timestamp is missing.
   *       This is intentionally permissive — the absence of a commitment should not block
   *       insertions.</li>
   *   <li>Returns {@link Duration#ZERO} (and logs a warning) if {@code latestExpectedArrivalTime}
   *       is before the arrival time — the schedule has slipped past the commitment, so no
   *       further deviation is acceptable.</li>
   * </ul>
   */
  private Duration extractDeviationBudget(EstimatedCall call) {
    var latestExpected = call.getLatestExpectedArrivalTime();
    var arrivalTime = call.getExpectedArrivalTime() != null
      ? call.getExpectedArrivalTime()
      : call.getAimedArrivalTime();

    if (latestExpected == null || arrivalTime == null) {
      return CarpoolStop.DEFAULT_DEVIATION_BUDGET;
    }

    Duration budget = Duration.between(arrivalTime, latestExpected);
    if (budget.isNegative()) {
      LOG.warn(
        "latestExpectedArrivalTime ({}) is before arrivalTime ({}), using zero deviation budget",
        latestExpected,
        arrivalTime
      );
      return Duration.ZERO;
    }
    return budget;
  }

  /**
   * Validates that the EstimatedCalls are properly ordered in time.
   * Ensures intermediate stops occur between the first (boarding) and last (alighting) calls.
   */
  private void validateEstimatedCallOrder(List<EstimatedCall> calls) {
    if (calls.size() < 2) {
      return;
    }

    ZonedDateTime firstTime = calls.getFirst().getAimedDepartureTime();
    ZonedDateTime lastTime = calls.getLast().getAimedArrivalTime();

    if (firstTime == null || lastTime == null) {
      LOG.warn("Cannot validate call order - missing timing information in first or last call");
      return;
    }

    if (firstTime.isAfter(lastTime)) {
      throw new IllegalArgumentException(
        String.format(
          "Invalid call order: first call time (%s) is after last call time (%s)",
          firstTime,
          lastTime
        )
      );
    }

    // Validate intermediate calls are between first and last
    for (int i = 1; i < calls.size() - 1; i++) {
      EstimatedCall intermediateCall = calls.get(i);
      ZonedDateTime intermediateTime = intermediateCall.getAimedDepartureTime() != null
        ? intermediateCall.getAimedDepartureTime()
        : intermediateCall.getAimedArrivalTime();

      if (intermediateTime == null) {
        LOG.warn("Intermediate call at index {} has no timing information", i);
        continue;
      }

      if (intermediateTime.isBefore(firstTime) || intermediateTime.isAfter(lastTime)) {
        throw new IllegalArgumentException(
          String.format(
            "Invalid call order: intermediate call at index %d (time: %s) is not between first (%s) and last (%s) calls",
            i,
            intermediateTime,
            firstTime,
            lastTime
          )
        );
      }
    }
  }

  /**
   * Builds a {@link CarpoolStop} from a SIRI call. The origin (when {@code isFirst} is true)
   * always gets {@link Duration#ZERO} as its deviation budget — the trip cannot start later
   * than scheduled — regardless of any value extracted from the call.
   */
  private CarpoolStop toCarpoolStop(
    EstimatedCall call,
    String id,
    String tripId,
    boolean isFirst,
    boolean isLast
  ) {
    var flexibleArea = toFlexibleArea(call);
    var circleLocation = flexibleArea.getCircularArea();
    var legacyGeometry = flexibleArea.getPolygon();
    var centroid = circleLocation == null
      ? toWgsCoordinate(toPolygon(legacyGeometry))
      : toWgsCoordinate(circleLocation);

    return CarpoolStop.of(new FeedScopedId(FEED_ID, id))
      .withCoordinate(centroid)
      .withAimedDepartureTime(isLast ? null : call.getAimedDepartureTime())
      .withExpectedDepartureTime(isLast ? null : call.getExpectedDepartureTime())
      .withAimedArrivalTime(isFirst ? null : call.getAimedArrivalTime())
      .withExpectedArrivalTime(isFirst ? null : call.getExpectedArrivalTime())
      .withLatestExpectedArrivalTime(isFirst ? null : call.getLatestExpectedArrivalTime())
      .withOnboardCount(extractOnboardCount(tripId, call))
      .withDeviationBudget(isFirst ? Duration.ZERO : extractDeviationBudget(call))
      .build();
  }

  private AimedFlexibleArea toFlexibleArea(EstimatedCall et) {
    var stopAssignments = et.getDepartureStopAssignments();
    if (stopAssignments == null || stopAssignments.isEmpty()) {
      stopAssignments = et.getArrivalStopAssignments();
    }

    if (stopAssignments == null || stopAssignments.size() != 1) {
      throw new IllegalArgumentException("Expected exactly one stop assignment for call: " + et);
    }
    var flexibleArea = stopAssignments.getFirst().getExpectedFlexibleArea();

    if (
      flexibleArea == null ||
      (flexibleArea.getPolygon() == null && flexibleArea.getCircularArea() == null)
    ) {
      throw new IllegalArgumentException("Missing flexible area for stop");
    }

    return flexibleArea;
  }

  private Polygon toPolygon(PolygonType gmlPolygon) {
    var abstractRing = gmlPolygon.getExterior().getAbstractRing().getValue();

    if (!(abstractRing instanceof LinearRingType linearRing)) {
      throw new IllegalArgumentException("Expected LinearRingType for polygon exterior");
    }

    List<Double> values = linearRing.getPosList().getValues();

    // Convert to JTS coordinates (lon lat pairs)
    Coordinate[] coords = new Coordinate[values.size() / 2];
    for (int i = 0; i < values.size(); i += 2) {
      coords[i / 2] = new Coordinate(values.get(i), values.get(i + 1));
    }

    LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coords);
    return GEOMETRY_FACTORY.createPolygon(shell);
  }

  private WgsCoordinate toWgsCoordinate(CircularAreaStructure circle) {
    double lat = circle.getLatitude().doubleValue();
    double lon = circle.getLongitude().doubleValue();

    return new WgsCoordinate(lat, lon);
  }

  private WgsCoordinate toWgsCoordinate(Polygon geometry) {
    var centroid = geometry.getCentroid();

    double lon = centroid.getX();
    double lat = centroid.getY();

    return new WgsCoordinate(lat, lon);
  }
}
