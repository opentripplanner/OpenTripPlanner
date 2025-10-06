package org.opentripplanner.routing.algorithm.mapping;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

public class ArriveByMapperSupport {

  public static final LocalDate SUNDAY = LocalDate.of(2025, 10, 5);
  public static final LocalDate MONDAY = LocalDate.of(2025, 10, 6);

  // Midnight crossing times from Sunday service day start 00:00 CEST Sunday, = 22:00 UTC Saturday
  public static final int AFTER_MIDNIGHT_DEPART = 95700; // 26.58h = Monday 00:35 UTC
  public static final int AFTER_CUTOFF_ARRIVE = 99540; // 27.65h = Monday 01:39 UTC (filtered out)

  // Normal daytime times
  public static final int BEFORE_CUTOFF_DEPART = 54000; // 13:00 UTC
  public static final int BEFORE_CUTOFF_ARRIVE = 55800; // 13:30 UTC (not filtered)

  // Edge case: arrival exactly at cutoff time
  public static final int AT_CUTOFF_ARRIVE = 57600; // 14:00 UTC (not filtered)

  public static final GenericLocation FROM = GenericLocation.fromCoordinate(60.0, 10.0);
  public static final GenericLocation TO = GenericLocation.fromCoordinate(59.0, 12.0);

  public static <T extends TripSchedule> RaptorPathToItineraryMapper<
    T
  > createMidnightDepartByMapper(Instant departTime, RaptorTransitData transitData) {
    var serviceDate = LocalDateTime.of(2025, Month.OCTOBER, 5, 0, 0);
    var transitSearchTimeZero = serviceDate.plusDays(1).atZone(ZoneIds.STOCKHOLM);
    return createMapper(transitSearchTimeZero, departTime, false, transitData);
  }

  public static <T extends TripSchedule> RaptorPathToItineraryMapper<
    T
  > createMidnightArriveByMapper(Instant cutoffTime, RaptorTransitData transitData) {
    var serviceDate = LocalDateTime.of(2025, Month.OCTOBER, 5, 0, 0);
    var transitSearchTimeZero = serviceDate.plusDays(1).atZone(ZoneIds.STOCKHOLM);
    return createMapper(transitSearchTimeZero, cutoffTime, true, transitData);
  }

  /**
   * Creates a mapper configured for the given search direction (arriveBy or departBy) with the specified scenario.
   */
  public static <T extends TripSchedule> RaptorPathToItineraryMapper<T> createMapper(
    ZonedDateTime transitSearchTimeZero,
    Instant time,
    boolean arriveBy,
    RaptorTransitData transitData
  ) {
    var request = RouteRequest.of()
      .withFrom(FROM)
      .withTo(TO)
      .withDateTime(time)
      .withArriveBy(arriveBy)
      .buildRequest();

    var timeTableRepository = new TimetableRepository();
    timeTableRepository.initTimeZone(ZoneIds.STOCKHOLM);
    timeTableRepository.index();

    return new RaptorPathToItineraryMapper<>(
      new Graph(),
      new DefaultTransitService(timeTableRepository),
      transitData,
      transitSearchTimeZero,
      request
    );
  }

  public static String formatTimeString(int... seconds) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < seconds.length; i++) {
      if (i > 0) result.append(" ");
      result.append(
        String.format(
          "%02d:%02d:%02d",
          seconds[i] / 3600,
          (seconds[i] % 3600) / 60,
          seconds[i] % 60
        )
      );
    }
    return result.toString();
  }

  /**
   * Creates a mapper configured for arriveBy search with normal daytime scenario.
   */
  public static <T extends TripSchedule> RaptorPathToItineraryMapper<T> createDaytimeArriveByMapper(
    Instant cutoffTime,
    RaptorTransitData transitData
  ) {
    var serviceDate = LocalDateTime.of(2025, Month.OCTOBER, 6, 0, 0);
    var transitSearchTimeZero = serviceDate.atZone(ZoneIds.STOCKHOLM);
    return createMapper(transitSearchTimeZero, cutoffTime, true, transitData);
  }
}
