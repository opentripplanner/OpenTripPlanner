package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolStopType;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Builder utility for creating test CarpoolTrip instances without requiring full Graph
 * infrastructure.
 */
public class CarpoolTripTestData {

  private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
  private static final AtomicInteger AREA_STOP_COUNTER = new AtomicInteger(0);

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

  public static CarpoolTrip createSimpleTripWithTimes(
    WgsCoordinate boarding,
    WgsCoordinate alighting,
    ZonedDateTime startTime,
    ZonedDateTime endTime
  ) {
    var origin = createOriginStopWithTime(boarding, startTime, startTime);
    var destination = createDestinationStopWithTime(alighting, 1, endTime, endTime);
    return new org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder(
      FeedScopedId.ofNullable("TEST", "trip-" + ID_COUNTER.incrementAndGet())
    )
      .withStops(List.of(origin, destination))
      .withAvailableSeats(4)
      .withStartTime(origin.getAimedDepartureTime())
      .withEndTime(destination.getAimedArrivalTime())
      .withDeviationBudget(Duration.of(8, ChronoUnit.MINUTES))
      .build();
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
        CarpoolStop.of(intermediate.getId(), () -> intermediate.getIndex() + 1)
          .withCoordinate(intermediate.getCoordinate())
          .withCarpoolStopType(intermediate.getCarpoolStopType())
          .withExpectedDepartureTime(intermediate.getExpectedDepartureTime())
          .withAimedArrivalTime(intermediate.getAimedArrivalTime())
          .withExpectedArrivalTime(intermediate.getExpectedArrivalTime())
          .withAimedArrivalTime(intermediate.getAimedDepartureTime())
          .withSequenceNumber(intermediate.getSequenceNumber() + 1)
          .withPassengerDelta(intermediate.getPassengerDelta())
          .build()
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
      FeedScopedId.ofNullable("TEST", "trip-" + ID_COUNTER.incrementAndGet())
    )
      .withStops(stops)
      .withAvailableSeats(seats)
      .withStartTime(ZonedDateTime.now())
      .withDeviationBudget(deviationBudget)
      .build();
  }

  /**
   * Creates a trip with specific start time and all other parameters. End time is calculated as
   * startTime + 1 hour.
   */
  public static CarpoolTrip createTripWithTime(
    ZonedDateTime startTime,
    int seats,
    List<CarpoolStop> stops
  ) {
    return new org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder(
      FeedScopedId.ofNullable("TEST", "trip-" + ID_COUNTER.incrementAndGet())
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
    return CarpoolStop.of(
        FeedScopedId.ofNullable("TEST", "area-" + AREA_STOP_COUNTER.incrementAndGet()),
        AREA_STOP_COUNTER::getAndIncrement
      )
      .withCoordinate(location)
      .withSequenceNumber(sequence)
      .withPassengerDelta(passengerDelta)
      .build();
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
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-0"), () -> 0)
      .withCoordinate(location)
      .withExpectedDepartureTime(expectedDepartureTime)
      .withAimedDepartureTime(aimedDepartureTime)
      .build();
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
    return CarpoolStop.of(
        FeedScopedId.ofNullable("TEST", "area-" + AREA_STOP_COUNTER.incrementAndGet()),
        AREA_STOP_COUNTER::getAndIncrement
      )
      .withCoordinate(location)
      .withCarpoolStopType(CarpoolStopType.DROP_OFF_ONLY)
      .withSequenceNumber(sequenceNumber)
      .withExpectedArrivalTime(expectedArrivalTime)
      .withAimedArrivalTime(aimedArrivalTime)
      .build();
  }
}
