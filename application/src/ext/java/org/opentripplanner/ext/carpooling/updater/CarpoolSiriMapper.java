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

  public CarpoolTrip mapSiriToCarpoolTrip(EstimatedVehicleJourney journey) {
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    if (calls.size() < 2) {
      throw new IllegalArgumentException(
        "Carpool trips must have at least 2 stops (origin and destination)."
      );
    }

    var origin = calls.getFirst();
    var destination = calls.getLast();

    var tripId = journey.getEstimatedVehicleJourneyCode();

    var originArea = buildAreaStop(origin, tripId + "_trip_origin");
    var destinationArea = buildAreaStop(destination, tripId + "_trip_destination");

    var startTime = origin.getExpectedDepartureTime() != null
      ? origin.getExpectedDepartureTime()
      : origin.getAimedDepartureTime();

    var endTime = destination.getExpectedArrivalTime() != null
      ? destination.getExpectedArrivalTime()
      : destination.getAimedArrivalTime();

    // TODO: Find a better way to exchange deviation budget with providers.
    // Using 15 minutes as a default for now.
    var deviationBudget = Duration.ofMinutes(15);

    var provider = journey.getOperatorRef().getValue();

    validateEstimatedCallOrder(calls);

    List<CarpoolStop> stops = new ArrayList<>();
    for (int i = 1; i < calls.size() - 1; i++) {
      var intermediateCall = calls.get(i);
      var stop = buildCarpoolStop(intermediateCall, tripId, i - 1);
      stops.add(stop);
    }

    return new CarpoolTripBuilder(new FeedScopedId(FEED_ID, tripId))
      .withOriginArea(originArea)
      .withDestinationArea(destinationArea)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withProvider(provider)
      .withDeviationBudget(deviationBudget)
      // TODO: Make available seats dynamic based on EstimatedVehicleJourney data
      .withAvailableSeats(2)
      .withStops(stops)
      .build();
  }

  /**
   * Build a CarpoolStop from an EstimatedCall, using point geometry instead of area geometry.
   * Determines the stop type and passenger delta from the call data.
   *
   * @param call The SIRI EstimatedCall containing stop information
   * @param tripId The trip ID for generating unique stop IDs
   * @param sequenceNumber The 0-based sequence number of this stop
   * @return A CarpoolStop representing the intermediate pickup/drop-off point
   */
  private CarpoolStop buildCarpoolStop(EstimatedCall call, String tripId, int sequenceNumber) {
    var areaStop = buildAreaStop(call, tripId + "_stop_" + sequenceNumber);

    // Extract timing information
    ZonedDateTime estimatedTime = call.getExpectedArrivalTime() != null
      ? call.getExpectedArrivalTime()
      : call.getAimedArrivalTime();

    // Determine stop type and passenger delta from call attributes
    CarpoolStop.CarpoolStopType stopType = determineCarpoolStopType(call);
    int passengerDelta = calculatePassengerDelta(call, stopType);

    return new CarpoolStop(areaStop, stopType, passengerDelta, sequenceNumber, estimatedTime);
  }

  /**
   * Determine the carpool stop type from the EstimatedCall data.
   */
  private CarpoolStop.CarpoolStopType determineCarpoolStopType(EstimatedCall call) {
    boolean hasArrival =
      call.getExpectedArrivalTime() != null || call.getAimedArrivalTime() != null;
    boolean hasDeparture =
      call.getExpectedDepartureTime() != null || call.getAimedDepartureTime() != null;

    if (hasArrival && hasDeparture) {
      return CarpoolStop.CarpoolStopType.PICKUP_AND_DROP_OFF;
    } else if (hasDeparture) {
      return CarpoolStop.CarpoolStopType.PICKUP_ONLY;
    } else if (hasArrival) {
      return CarpoolStop.CarpoolStopType.DROP_OFF_ONLY;
    } else {
      return CarpoolStop.CarpoolStopType.PICKUP_AND_DROP_OFF;
    }
  }

  /**
   * Calculate the passenger delta (change in passenger count) from the EstimatedCall.
   */
  private int calculatePassengerDelta(EstimatedCall call, CarpoolStop.CarpoolStopType stopType) {
    // This is a placeholder implementation - adapt based on SIRI ET data structure
    // SIRI ET may have passenger count changes, boarding/alighting numbers, etc.

    // For now, return a default value of 1 passenger pickup/dropoff
    if (stopType == CarpoolStop.CarpoolStopType.DROP_OFF_ONLY) {
      // Assume 1 passenger drop-off
      return -1;
    } else if (stopType == CarpoolStop.CarpoolStopType.PICKUP_ONLY) {
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
