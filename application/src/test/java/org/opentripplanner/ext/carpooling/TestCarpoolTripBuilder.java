package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolStopType;
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
   * Creates a simple trip with origin and destination stops, default capacity of 4.
   */
  public static CarpoolTrip createSimpleTrip(WgsCoordinate boarding, WgsCoordinate alighting) {
    var origin = createOriginStop(boarding);
    var destination = createDestinationStop(alighting, 1);
    return createTripWithCapacity(4, List.of(origin, destination));
  }

  /**
   * Creates a simple trip with specific departure time.
   */
  public static CarpoolTrip createSimpleTripWithTime(
    WgsCoordinate boarding,
    WgsCoordinate alighting,
    ZonedDateTime startTime
  ) {
    var origin = createOriginStopWithTime(boarding, startTime, startTime);
    var destination = createDestinationStopWithTime(
      alighting,
      1,
      startTime.plusHours(1),
      startTime.plusHours(1)
    );
    return createTripWithTime(startTime, 4, List.of(origin, destination));
  }

  /**
   * Creates a trip with origin, intermediate stops, and destination.
   */
  public static CarpoolTrip createTripWithStops(
    WgsCoordinate boarding,
    List<CarpoolStop> intermediateStops,
    WgsCoordinate alighting
  ) {
    List<CarpoolStop> allStops = new ArrayList<>();
    allStops.add(createOriginStop(boarding));

    // Renumber intermediate stops to account for origin at position 0
    for (int i = 0; i < intermediateStops.size(); i++) {
      CarpoolStop intermediate = intermediateStops.get(i);
      allStops.add(
        new CarpoolStop(
          intermediate.getAreaStop(),
          intermediate.getCarpoolStopType(),
          intermediate.getPassengerDelta(),
          i + 1,
          intermediate.getExpectedArrivalTime(),
          intermediate.getAimedArrivalTime(),
          intermediate.getExpectedDepartureTime(),
          intermediate.getAimedDepartureTime()
        )
      );
    }

    allStops.add(createDestinationStop(alighting, allStops.size()));
    return createTripWithCapacity(4, allStops);
  }

  /**
   * Creates a trip with specified capacity and all stops (including origin/destination).
   */
  public static CarpoolTrip createTripWithCapacity(int seats, List<CarpoolStop> stops) {
    return createTripWithDeviationBudget(Duration.ofMinutes(10), seats, stops);
  }

  /**
   * Creates a trip with specified deviation budget.
   */
  public static CarpoolTrip createTripWithDeviationBudget(
    Duration deviationBudget,
    WgsCoordinate boarding,
    WgsCoordinate alighting
  ) {
    var origin = createOriginStop(boarding);
    var destination = createDestinationStop(alighting, 1);
    return createTripWithDeviationBudget(deviationBudget, 4, List.of(origin, destination));
  }

  /**
   * Creates a trip with all parameters specified.
   */
  public static CarpoolTrip createTripWithDeviationBudget(
    Duration deviationBudget,
    int seats,
    List<CarpoolStop> stops
  ) {
    return new org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder(
      org.opentripplanner.transit.model.framework.FeedScopedId.ofNullable(
        "TEST",
        "trip-" + idCounter.incrementAndGet()
      )
    )
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
    List<CarpoolStop> stops
  ) {
    return new org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder(
      org.opentripplanner.transit.model.framework.FeedScopedId.ofNullable(
        "TEST",
        "trip-" + idCounter.incrementAndGet()
      )
    )
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
      CarpoolStopType.PICKUP_AND_DROP_OFF,
      passengerDelta,
      sequence,
      null,
      null,
      null,
      null
    );
  }

  /**
   * Creates an origin stop (first stop, PICKUP_ONLY, passengerDelta=0, departure times only).
   */
  public static CarpoolStop createOriginStop(WgsCoordinate location) {
    return createOriginStopWithTime(location, null, null);
  }

  /**
   * Creates an origin stop with specific departure times.
   */
  public static CarpoolStop createOriginStopWithTime(
    WgsCoordinate location,
    ZonedDateTime expectedDepartureTime,
    ZonedDateTime aimedDepartureTime
  ) {
    return new CarpoolStop(
      createAreaStop(location),
      CarpoolStopType.PICKUP_ONLY,
      0,
      0,
      null,
      null,
      expectedDepartureTime,
      aimedDepartureTime
    );
  }

  /**
   * Creates a destination stop (last stop, DROP_OFF_ONLY, passengerDelta=0, arrival times only).
   */
  public static CarpoolStop createDestinationStop(WgsCoordinate location, int sequenceNumber) {
    return createDestinationStopWithTime(location, sequenceNumber, null, null);
  }

  /**
   * Creates a destination stop with specific arrival times.
   */
  public static CarpoolStop createDestinationStopWithTime(
    WgsCoordinate location,
    int sequenceNumber,
    ZonedDateTime expectedArrivalTime,
    ZonedDateTime aimedArrivalTime
  ) {
    return new CarpoolStop(
      createAreaStop(location),
      CarpoolStopType.DROP_OFF_ONLY,
      0,
      sequenceNumber,
      expectedArrivalTime,
      aimedArrivalTime,
      null,
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
