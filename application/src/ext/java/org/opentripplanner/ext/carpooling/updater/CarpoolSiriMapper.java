package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolStopType;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.AimedFlexibleArea;
import uk.org.siri.siri21.CircularAreaStructure;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;

public class CarpoolSiriMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolSiriMapper.class);
  private static final String FEED_ID = "ENT";
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final int DEFAULT_AVAILABLE_SEATS = 2;
  private static final Duration DEFAULT_DEVIATION_BUDGET = Duration.ofMinutes(15);
  // INDEX is not relevant for our stop type. Also set index to a hard coded value to avoid
  // run-away memory use if it by error ends up in global repositories.
  public static final int CARPOOLING_DUMMY_INDEX = -9_999;

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
    var stopId = isFirst
      ? tripId + "_trip_origin"
      : isLast
        ? tripId + "_trip_destination"
        : tripId + "_stop_" + sequenceNumber;

    return toCarpoolStop(call, stopId, isFirst, isLast);
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

  private CarpoolStop toCarpoolStop(
    EstimatedCall call,
    String id,
    boolean isFirst,
    boolean isLast
  ) {
    var flexibleArea = toFlexibleArea(call);
    var circleLocation = flexibleArea.getCircularArea();
    var legacyGeometry = flexibleArea.getPolygon();
    var centroid = circleLocation == null
      ? toWgsCoordinate(toPolygon(legacyGeometry))
      : toWgsCoordinate(circleLocation);

    CarpoolStopType stopType;
    if (isFirst) {
      stopType = CarpoolStopType.PICKUP_ONLY;
    } else if (isLast) {
      stopType = CarpoolStopType.DROP_OFF_ONLY;
    } else {
      stopType = determineCarpoolStopType(call);
    }

    return CarpoolStop.of(new FeedScopedId(FEED_ID, id), () -> CARPOOLING_DUMMY_INDEX)
      .withName(I18NString.of(call.getStopPointNames().getFirst().getValue()))
      .withCentroid(centroid)
      .withCarpoolStopType(stopType)
      .withAimedDepartureTime(isLast ? null : call.getAimedDepartureTime())
      .withExpectedDepartureTime(isLast ? null : call.getExpectedDepartureTime())
      .withAimedArrivalTime(isFirst ? null : call.getAimedArrivalTime())
      .withExpectedArrivalTime(isFirst ? null : call.getExpectedArrivalTime())
      .withPassengerDelta(isLast ? 0 : calculatePassengerDelta(call, stopType))
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

    List<Double> values = linearRing.getPosList().getValue();

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
