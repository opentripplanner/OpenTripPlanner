package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * Builder utility for creating test CarpoolTrip instances without requiring full Graph infrastructure.
 */
public class TestCarpoolTripBuilder {

  private static final AtomicInteger idCounter = new AtomicInteger(0);
  private static final AtomicInteger areaStopCounter = new AtomicInteger(0);

  /**
   * Creates a simple trip with no stops and default capacity of 4.
   */
  public static CarpoolTrip createSimpleTrip(WgsCoordinate boarding, WgsCoordinate alighting) {
    return createTripWithCapacity(4, boarding, List.of(), alighting);
  }

  /**
   * Creates a simple trip with specific departure time.
   */
  public static CarpoolTrip createSimpleTripWithTime(
    WgsCoordinate boarding,
    WgsCoordinate alighting,
    ZonedDateTime startTime
  ) {
    return createTripWithTime(startTime, 4, boarding, List.of(), alighting);
  }

  /**
   * Creates a trip with specified stops.
   */
  public static CarpoolTrip createTripWithStops(
    WgsCoordinate boarding,
    List<CarpoolStop> stops,
    WgsCoordinate alighting
  ) {
    return createTripWithCapacity(4, boarding, stops, alighting);
  }

  /**
   * Creates a trip with specified capacity.
   */
  public static CarpoolTrip createTripWithCapacity(
    int seats,
    WgsCoordinate boarding,
    List<CarpoolStop> stops,
    WgsCoordinate alighting
  ) {
    return createTripWithDeviationBudget(Duration.ofMinutes(10), seats, boarding, stops, alighting);
  }

  /**
   * Creates a trip with specified deviation budget.
   */
  public static CarpoolTrip createTripWithDeviationBudget(
    Duration deviationBudget,
    WgsCoordinate boarding,
    WgsCoordinate alighting
  ) {
    return createTripWithDeviationBudget(deviationBudget, 4, boarding, List.of(), alighting);
  }

  /**
   * Creates a trip with all parameters specified.
   */
  public static CarpoolTrip createTripWithDeviationBudget(
    Duration deviationBudget,
    int seats,
    WgsCoordinate origin,
    List<CarpoolStop> stops,
    WgsCoordinate destination
  ) {
    return new org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder(
      org.opentripplanner.transit.model.framework.FeedScopedId.ofNullable(
        "TEST",
        "trip-" + idCounter.incrementAndGet()
      )
    )
      .withOriginArea(createAreaStop(origin))
      .withDestinationArea(createAreaStop(destination))
      .withStops(stops)
      .withAvailableSeats(seats)
      .withStartTime(ZonedDateTime.now())
      .withDeviationBudget(deviationBudget)
      .build();
  }

  /**
   * Creates a trip with specific start time and all other parameters.
   * End time is calculated as startTime + 1 hour.
   */
  public static CarpoolTrip createTripWithTime(
    ZonedDateTime startTime,
    int seats,
    WgsCoordinate origin,
    List<CarpoolStop> stops,
    WgsCoordinate destination
  ) {
    return new org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder(
      org.opentripplanner.transit.model.framework.FeedScopedId.ofNullable(
        "TEST",
        "trip-" + idCounter.incrementAndGet()
      )
    )
      .withOriginArea(createAreaStop(origin))
      .withDestinationArea(createAreaStop(destination))
      .withStops(stops)
      .withAvailableSeats(seats)
      .withStartTime(startTime)
      .withEndTime(startTime.plusHours(1))
      .withDeviationBudget(Duration.ofMinutes(10))
      .build();
  }

  /**
   * Creates a CarpoolStop with specified sequence (0-based) and passenger delta.
   */
  public static CarpoolStop createStop(int zeroBasedSequence, int passengerDelta) {
    return createStopAt(zeroBasedSequence, passengerDelta, CarpoolTestCoordinates.OSLO_CENTER);
  }

  /**
   * Creates a CarpoolStop at a specific location.
   */
  public static CarpoolStop createStopAt(int sequence, WgsCoordinate location) {
    return createStopAt(sequence, 0, location);
  }

  /**
   * Creates a CarpoolStop with all parameters.
   */
  public static CarpoolStop createStopAt(int sequence, int passengerDelta, WgsCoordinate location) {
    return new CarpoolStop(
      createAreaStop(location),
      CarpoolStop.CarpoolStopType.PICKUP_AND_DROP_OFF,
      passengerDelta,
      sequence,
      null
    );
  }

  /**
   * Creates a minimal AreaStop for testing.
   */
  private static AreaStop createAreaStop(WgsCoordinate coordinate) {
    // Create a simple point geometry at the coordinate
    var geometryFactory = new org.locationtech.jts.geom.GeometryFactory();
    var point = geometryFactory.createPoint(
      new org.locationtech.jts.geom.Coordinate(coordinate.longitude(), coordinate.latitude())
    );

    return AreaStop.of(
      org.opentripplanner.transit.model.framework.FeedScopedId.ofNullable(
        "TEST",
        "area-" + areaStopCounter.incrementAndGet()
      ),
      areaStopCounter::getAndIncrement
    )
      .withGeometry(point)
      .build();
  }
}
