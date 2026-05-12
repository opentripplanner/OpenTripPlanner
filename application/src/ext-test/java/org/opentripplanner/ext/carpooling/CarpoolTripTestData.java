package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Builder utility for creating test CarpoolTrip instances without requiring full Graph
 * infrastructure.
 */
public class CarpoolTripTestData {

  private static int idCounter = 0;

  private static final int DEFAULT_TOTAL_CAPACITY = CarpoolTrip.DEFAULT_TOTAL_CAPACITY;
  private static final Duration DEFAULT_DEVIATION_BUDGET = Duration.ofMinutes(10);

  /**
   * Creates a simple trip with origin and destination stops.
   */
  public static CarpoolTrip createSimpleTrip(WgsCoordinate boarding, WgsCoordinate alighting) {
    var stops = List.of(createOriginStop(boarding), createDestinationStop(alighting));
    return buildTrip(DEFAULT_TOTAL_CAPACITY, null, stops);
  }

  /**
   * Creates a simple trip with specific departure time.
   */
  public static CarpoolTrip createSimpleTripWithTime(
    WgsCoordinate boarding,
    WgsCoordinate alighting,
    ZonedDateTime startTime
  ) {
    var stops = List.of(
      createOriginStopWithTime(boarding, startTime, startTime),
      createDestinationStopWithTime(alighting, startTime.plusHours(1), startTime.plusHours(1))
    );
    return buildTrip(DEFAULT_TOTAL_CAPACITY, startTime, stops);
  }

  /**
   * Creates a simple trip with specific start and end times.
   */
  public static CarpoolTrip createSimpleTripWithTimes(
    WgsCoordinate boarding,
    WgsCoordinate alighting,
    ZonedDateTime startTime,
    ZonedDateTime endTime
  ) {
    var origin = createOriginStopWithTime(boarding, startTime, startTime);
    var destination = createDestinationStopWithTime(alighting, endTime, endTime);
    return new CarpoolTripBuilder(FeedScopedId.ofNullable("TEST", "trip-" + ++idCounter))
      .withStops(List.of(origin, destination))
      .withTotalCapacity(DEFAULT_TOTAL_CAPACITY)
      .withStartTime(origin.getAimedDepartureTime())
      .withEndTime(destination.getAimedArrivalTime())
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

    for (int i = 0; i < intermediateStops.size(); i++) {
      CarpoolStop intermediate = intermediateStops.get(i);
      allStops.add(
        CarpoolStop.of(intermediate.getId())
          .withCoordinate(intermediate.getCoordinate())
          .withExpectedDepartureTime(intermediate.getExpectedDepartureTime())
          .withAimedDepartureTime(intermediate.getAimedDepartureTime())
          .withExpectedArrivalTime(intermediate.getExpectedArrivalTime())
          .withAimedArrivalTime(intermediate.getAimedArrivalTime())
          .withOnboardCount(intermediate.getOnboardCount())
          .withDeviationBudget(intermediate.getDeviationBudget())
          .build()
      );
    }

    allStops.add(createDestinationStop(alighting));
    return buildTrip(DEFAULT_TOTAL_CAPACITY, null, allStops);
  }

  /**
   * Creates a trip with origin, intermediate stops, and destination. The deviation budget is applied
   * to the origin and destination stops, while intermediate stops retain their own deviation budget.
   */
  public static CarpoolTrip createTripWithStops(
    WgsCoordinate boarding,
    List<CarpoolStop> intermediateStops,
    WgsCoordinate alighting,
    Duration deviationBudget
  ) {
    List<CarpoolStop> allStops = new ArrayList<>();
    allStops.add(createOriginStopWithDeviationBudget(boarding, deviationBudget));

    for (int i = 0; i < intermediateStops.size(); i++) {
      CarpoolStop intermediate = intermediateStops.get(i);
      allStops.add(
        CarpoolStop.of(intermediate.getId())
          .withCoordinate(intermediate.getCoordinate())
          .withExpectedDepartureTime(intermediate.getExpectedDepartureTime())
          .withAimedDepartureTime(intermediate.getAimedDepartureTime())
          .withExpectedArrivalTime(intermediate.getExpectedArrivalTime())
          .withAimedArrivalTime(intermediate.getAimedArrivalTime())
          .withOnboardCount(intermediate.getOnboardCount())
          .withDeviationBudget(intermediate.getDeviationBudget())
          .build()
      );
    }

    allStops.add(createDestinationStopWithDeviationBudget(alighting, deviationBudget));
    return buildTrip(DEFAULT_TOTAL_CAPACITY, null, allStops);
  }

  /**
   * Creates a trip with specified capacity and all stops (including origin/destination).
   */
  public static CarpoolTrip createTripWithCapacity(int capacity, List<CarpoolStop> stops) {
    return buildTrip(capacity, null, stops);
  }

  /**
   * Creates a trip with specified deviation budget on all stops.
   */
  public static CarpoolTrip createTripWithDeviationBudget(
    Duration deviationBudget,
    WgsCoordinate boarding,
    WgsCoordinate alighting
  ) {
    var origin = createOriginStopWithDeviationBudget(boarding, deviationBudget);
    var destination = createDestinationStopWithDeviationBudget(alighting, deviationBudget);
    return buildTrip(DEFAULT_TOTAL_CAPACITY, null, List.of(origin, destination));
  }

  /**
   * Creates a trip with specific start time.
   */
  public static CarpoolTrip createTripWithTime(
    ZonedDateTime startTime,
    int capacity,
    List<CarpoolStop> stops
  ) {
    return buildTrip(capacity, startTime, stops);
  }

  /**
   * Creates a CarpoolStop with specified onboard count.
   */
  public static CarpoolStop createStop(int onboardCount) {
    return createStopAt(onboardCount, CarpoolTestCoordinates.OSLO_CENTER);
  }

  /**
   * Creates a CarpoolStop at a specific location with onboardCount=1 (driver only).
   */
  public static CarpoolStop createStopAt(WgsCoordinate location) {
    return createStopAt(1, location);
  }

  /**
   * Creates a CarpoolStop at a specific location with a specific deviation budget.
   */
  public static CarpoolStop createStopAt(WgsCoordinate location, Duration deviationBudget) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-" + ++idCounter))
      .withCoordinate(location)
      .withOnboardCount(1)
      .withDeviationBudget(deviationBudget)
      .build();
  }

  /**
   * Creates a CarpoolStop with all parameters.
   */
  public static CarpoolStop createStopAt(int onboardCount, WgsCoordinate location) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-" + ++idCounter))
      .withCoordinate(location)
      .withOnboardCount(onboardCount)
      .withDeviationBudget(DEFAULT_DEVIATION_BUDGET)
      .build();
  }

  public static CarpoolStop createOriginStop(WgsCoordinate location) {
    return createOriginStopWithTime(location, null, null);
  }

  public static CarpoolStop createOriginStopWithTime(
    WgsCoordinate location,
    ZonedDateTime expectedDepartureTime,
    ZonedDateTime aimedDepartureTime
  ) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-" + ++idCounter))
      .withCoordinate(location)
      .withOnboardCount(1)
      .withExpectedDepartureTime(expectedDepartureTime)
      .withAimedDepartureTime(aimedDepartureTime)
      .withDeviationBudget(DEFAULT_DEVIATION_BUDGET)
      .build();
  }

  /**
   * Creates an origin stop with specific deviation budget.
   */
  public static CarpoolStop createOriginStopWithDeviationBudget(
    WgsCoordinate location,
    Duration deviationBudget
  ) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-" + ++idCounter))
      .withCoordinate(location)
      .withOnboardCount(1)
      .withDeviationBudget(deviationBudget)
      .build();
  }

  public static CarpoolStop createDestinationStop(WgsCoordinate location) {
    return createDestinationStopWithTime(location, null, null);
  }

  public static CarpoolStop createDestinationStopWithTime(
    WgsCoordinate location,
    ZonedDateTime expectedArrivalTime,
    ZonedDateTime aimedArrivalTime
  ) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-" + ++idCounter))
      .withCoordinate(location)
      .withOnboardCount(1)
      .withExpectedArrivalTime(expectedArrivalTime)
      .withAimedArrivalTime(aimedArrivalTime)
      .withDeviationBudget(DEFAULT_DEVIATION_BUDGET)
      .build();
  }

  /**
   * Creates a destination stop with specific deviation budget.
   */
  public static CarpoolStop createDestinationStopWithDeviationBudget(
    WgsCoordinate location,
    Duration deviationBudget
  ) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "area-" + ++idCounter))
      .withCoordinate(location)
      .withOnboardCount(1)
      .withDeviationBudget(deviationBudget)
      .build();
  }

  private static CarpoolTrip buildTrip(
    int capacity,
    ZonedDateTime startTime,
    List<CarpoolStop> stops
  ) {
    var actualStartTime = startTime != null ? startTime : ZonedDateTime.now();
    var builder = new CarpoolTripBuilder(FeedScopedId.ofNullable("TEST", "trip-" + ++idCounter))
      .withStops(stops)
      .withTotalCapacity(capacity)
      .withStartTime(actualStartTime);
    if (startTime != null) {
      builder.withEndTime(startTime.plusHours(1));
    }
    return builder.build();
  }
}
