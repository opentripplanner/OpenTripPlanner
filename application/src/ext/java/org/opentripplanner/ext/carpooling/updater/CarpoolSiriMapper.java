package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolStopType;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;

public class CarpoolSiriMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolSiriMapper.class);
  private static final String FEED_ID = "ENT";
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final AtomicInteger COUNTER = new AtomicInteger(0);

  private static final int DEFAULT_AVAILABLE_SEATS = 2;
  private static final Duration DEFAULT_DEVIATION_BUDGET = Duration.ofMinutes(15);

  public CarpoolTrip mapSiriToCarpoolTrip(EstimatedVehicleJourney journey) {
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    if (calls.size() < 2) {
      throw new IllegalArgumentException(
        "Carpool trips must have at least 2 stops (origin and destination)."
      );
    }

    var tripId = journey.getEstimatedVehicleJourneyCode();

    validateEstimatedCallOrder(calls);

    List<CarpoolStop> stops = new ArrayList<>();

    for (int i = 0; i < calls.size(); i++) {
      EstimatedCall call = calls.get(i);
      boolean isFirst = (i == 0);
      boolean isLast = (i == calls.size() - 1);

      CarpoolStop stop = buildCarpoolStopForPosition(call, tripId, i, isFirst, isLast);
      stops.add(stop);
    }

    // Extract start/end times from first/last stops
    CarpoolStop firstStop = stops.getFirst();
    CarpoolStop lastStop = stops.getLast();

    ZonedDateTime startTime = firstStop.getExpectedDepartureTime() != null
      ? firstStop.getExpectedDepartureTime()
      : firstStop.getAimedDepartureTime();

    ZonedDateTime endTime = lastStop.getExpectedArrivalTime() != null
      ? lastStop.getExpectedArrivalTime()
      : lastStop.getAimedArrivalTime();

    return new CarpoolTripBuilder(new FeedScopedId(FEED_ID, tripId))
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withProvider(journey.getOperatorRef().getValue())
      // TODO: Find a better way to exchange deviation budget with providers.
      .withDeviationBudget(DEFAULT_DEVIATION_BUDGET)
      // TODO: Make available seats dynamic based on EstimatedVehicleJourney data
      .withAvailableSeats(DEFAULT_AVAILABLE_SEATS)
      .withStops(stops)
      .build();
  }

  /**
   * Build a CarpoolStop from an EstimatedCall with special handling for first/last positions.
   *
   * @param call The SIRI EstimatedCall containing stop information
   * @param tripId The trip ID for generating unique stop IDs
   * @param sequenceNumber The 0-based sequence number of this stop
   * @param isFirst true if this is the first stop (origin)
   * @param isLast true if this is the last stop (destination)
   * @return A CarpoolStop representing the stop
   */
  private CarpoolStop buildCarpoolStopForPosition(
    EstimatedCall call,
    String tripId,
    int sequenceNumber,
    boolean isFirst,
    boolean isLast
  ) {
    String stopId = isFirst
      ? tripId + "_trip_origin"
      : isLast ? tripId + "_trip_destination" : tripId + "_stop_" + sequenceNumber;

    var areaStop = buildAreaStop(call, stopId);

    // Extract all four timing fields
    ZonedDateTime expectedArrivalTime = call.getExpectedArrivalTime();
    ZonedDateTime aimedArrivalTime = call.getAimedArrivalTime();
    ZonedDateTime expectedDepartureTime = call.getExpectedDepartureTime();
    ZonedDateTime aimedDepartureTime = call.getAimedDepartureTime();

    // Special handling for first and last stops
    CarpoolStopType stopType;
    int passengerDelta;

    if (isFirst) {
      // Origin: PICKUP_ONLY, no passengers initially, only departure times
      stopType = CarpoolStopType.PICKUP_ONLY;
      passengerDelta = 0;
      expectedArrivalTime = null;
      aimedArrivalTime = null;
    } else if (isLast) {
      // Destination: DROP_OFF_ONLY, no passengers remain, only arrival times
      stopType = CarpoolStopType.DROP_OFF_ONLY;
      passengerDelta = 0;
      expectedDepartureTime = null;
      aimedDepartureTime = null;
    } else {
      // Intermediate stop: determine from call data
      stopType = determineCarpoolStopType(call);
      passengerDelta = calculatePassengerDelta(call, stopType);
    }

    return new CarpoolStop(
      areaStop,
      stopType,
      passengerDelta,
      sequenceNumber,
      expectedArrivalTime,
      aimedArrivalTime,
      expectedDepartureTime,
      aimedDepartureTime
    );
  }

  /**
   * Determine the carpool stop type from the EstimatedCall data.
   */
  private CarpoolStopType determineCarpoolStopType(EstimatedCall call) {
    boolean hasArrival =
      call.getExpectedArrivalTime() != null || call.getAimedArrivalTime() != null;
    boolean hasDeparture =
      call.getExpectedDepartureTime() != null || call.getAimedDepartureTime() != null;

    if (hasArrival && hasDeparture) {
      return CarpoolStopType.PICKUP_AND_DROP_OFF;
    } else if (hasDeparture) {
      return CarpoolStopType.PICKUP_ONLY;
    } else if (hasArrival) {
      return CarpoolStopType.DROP_OFF_ONLY;
    } else {
      return CarpoolStopType.PICKUP_AND_DROP_OFF;
    }
  }

  /**
   * Calculate the passenger delta (change in passenger count) from the EstimatedCall.
   */
  private int calculatePassengerDelta(EstimatedCall call, CarpoolStopType stopType) {
    // This is a placeholder implementation - adapt based on SIRI ET data structure
    // SIRI ET may have passenger count changes, boarding/alighting numbers, etc.

    // For now, return a default value of 1 passenger pickup/dropoff
    if (stopType == CarpoolStopType.DROP_OFF_ONLY) {
      // Assume 1 passenger drop-off
      return -1;
    } else if (stopType == CarpoolStopType.PICKUP_ONLY) {
      // Assume 1 passenger pickup
      return 1;
    } else {
      // No net change for both pickup and drop-off
      return 0;
    }
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

  private AreaStop buildAreaStop(EstimatedCall call, String id) {
    var stopAssignments = call.getDepartureStopAssignments();
    if (stopAssignments == null || stopAssignments.isEmpty()) {
      stopAssignments = call.getArrivalStopAssignments();
    }

    if (stopAssignments == null || stopAssignments.size() != 1) {
      throw new IllegalArgumentException("Expected exactly one stop assignment for call: " + call);
    }
    var flexibleArea = stopAssignments.getFirst().getExpectedFlexibleArea();

    if (flexibleArea == null || flexibleArea.getPolygon() == null) {
      throw new IllegalArgumentException("Missing flexible area for stop");
    }

    var polygon = createPolygonFromGml(flexibleArea.getPolygon());

    return AreaStop.of(new FeedScopedId(FEED_ID, id), COUNTER::getAndIncrement)
      .withName(I18NString.of(call.getStopPointNames().getFirst().getValue()))
      .withGeometry(polygon)
      .build();
  }

  private Polygon createPolygonFromGml(PolygonType gmlPolygon) {
    var abstractRing = gmlPolygon.getExterior().getAbstractRing().getValue();

    if (!(abstractRing instanceof LinearRingType linearRing)) {
      throw new IllegalArgumentException("Expected LinearRingType for polygon exterior");
    }

    List<Double> values = linearRing.getPosList().getValue();

    // Convert to JTS coordinates (lon lat pairs)
    Coordinate[] coords = new Coordinate[values.size() / 2];
    for (int i = 0; i < values.size(); i += 2) {
      coords[i / 2] = new Coordinate(values.get(i), values.get(i + 1));
    }

    LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coords);
    return GEOMETRY_FACTORY.createPolygon(shell);
  }
}
