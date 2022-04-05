package org.opentripplanner.routing.stoptimes;

import static org.opentripplanner.routing.stoptimes.StopTimesHelper.skipByTripCancellation;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * A helper class to fetch previous/next alternative legs for a scheduled transit leg.
 * The replacement legs arrive/depart from/to the same station as the original leg, but uses other
 * trips for the leg.
 *
 * Generalized cost and constrained transfers are not included in the alternative legs.
 */
public class AlternativeLegs {

  public static final int ZERO_COST = 0;

  public static List<ScheduledTransitLeg> getAlternativeLegs(
    Leg leg,
    Integer numberLegs,
    RoutingService routingService,
    boolean searchBackward
  ) {
    StopLocation fromStop = leg.getFrom().stop;
    StopLocation toStop = leg.getTo().stop;

    Station fromStation = fromStop.getParentStation();
    Station toStation = toStop.getParentStation();

    Collection<StopLocation> origins = fromStation == null
      ? List.of(fromStop)
      : fromStation.getChildStops();

    Collection<StopLocation> destinations = toStation == null
      ? List.of(toStop)
      : toStation.getChildStops();

    TimetableSnapshot timetableSnapshot = routingService.getTimetableSnapshot();

    Comparator<ScheduledTransitLeg> legComparator = Comparator.comparing(
      ScheduledTransitLeg::getStartTime
    );

    if (searchBackward) {
      legComparator = legComparator.reversed();
    }

    return origins
      .stream()
      .flatMap(stop -> routingService.getPatternsForStop(stop, timetableSnapshot).stream())
      .filter(tripPattern -> tripPattern.getStops().stream().anyMatch(destinations::contains))
      .flatMap(tripPattern -> withBoardingAlightingPositions(origins, destinations, tripPattern))
      .flatMap(t ->
        generateLegs(
          routingService,
          timetableSnapshot,
          t,
          leg.getStartTime(),
          leg.getServiceDate(),
          searchBackward
        )
      )
      .filter(Predicate.not(leg::isPartiallySameTransitLeg))
      .sorted(legComparator)
      .limit(numberLegs)
      .collect(Collectors.toList());
  }

  /**
   * This has been copied and slightly modified from StopTimesHelper.
   * TODO: Adapt after new transit model is in place
   */
  @Nonnull
  private static Stream<ScheduledTransitLeg> generateLegs(
    RoutingService routingService,
    TimetableSnapshot timetableSnapshot,
    T2<TripPattern, P2<Integer>> patternWithBoardAlightPositions,
    Calendar departureTime,
    ServiceDate originalDate,
    boolean searchBackward
  ) {
    TripPattern pattern = patternWithBoardAlightPositions.first;
    int boardingPosition = patternWithBoardAlightPositions.second.first;
    int alightingPosition = patternWithBoardAlightPositions.second.second;

    // TODO: What should we have here
    ZoneId timeZone = routingService.getTimeZone().toZoneId();

    Comparator<TripTimeOnDate> comparator = Comparator.comparing((TripTimeOnDate tts) ->
      tts.getServiceDayMidnight() + tts.getRealtimeDeparture()
    );

    if (searchBackward) {
      comparator = comparator.reversed();
    }

    Queue<TripTimeOnDate> pq = new PriorityQueue<>(comparator);

    long startTime = departureTime.getTimeInMillis() / 1000;

    // Loop through all possible days
    ServiceDate[] serviceDates = { originalDate.previous(), originalDate, originalDate.next() };

    for (ServiceDate serviceDate : serviceDates) {
      ServiceDay sd = new ServiceDay(
        routingService.getServiceCodes(),
        serviceDate,
        routingService.getCalendarService(),
        pattern.getRoute().getAgency().getId()
      );

      Timetable timetable;
      if (timetableSnapshot != null) {
        timetable = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        timetable = pattern.getScheduledTimetable();
      }

      int secondsSinceMidnight = sd.secondsSinceMidnight(startTime);
      for (TripTimes tripTimes : timetable.getTripTimes()) {
        if (!sd.serviceRunning(tripTimes.getServiceCode())) {
          continue;
        }
        if (skipByTripCancellation(tripTimes, false)) {
          continue;
        }

        boolean departureTimeInRange = searchBackward
          ? tripTimes.getDepartureTime(boardingPosition) <= secondsSinceMidnight
          : tripTimes.getDepartureTime(boardingPosition) >= secondsSinceMidnight;

        if (departureTimeInRange) {
          pq.add(new TripTimeOnDate(tripTimes, boardingPosition, pattern, sd));
        }
      }
    }

    List<ScheduledTransitLeg> res = new ArrayList<>();

    while (!pq.isEmpty()) {
      TripTimeOnDate tripTimeOnDate = pq.poll();
      res.add(mapToLeg(timeZone, pattern, boardingPosition, alightingPosition, tripTimeOnDate));
    }

    return res.stream();
  }

  @Nonnull
  private static ScheduledTransitLeg mapToLeg(
    ZoneId timeZone,
    TripPattern pattern,
    int boardingPosition,
    int alightingPosition,
    TripTimeOnDate tripTimeOnDate
  ) {
    ServiceDate serviceDay = tripTimeOnDate.getServiceDay();

    TripTimes tripTimes = tripTimeOnDate.getTripTimes();
    ZonedDateTime boardingTime = serviceDay.toZonedDateTime(
      timeZone,
      tripTimeOnDate.getRealtimeDeparture()
    );
    ZonedDateTime alightingTime = serviceDay.toZonedDateTime(
      timeZone,
      tripTimes.getArrivalTime(alightingPosition)
    );

    return new ScheduledTransitLeg(
      tripTimes,
      pattern,
      boardingPosition,
      alightingPosition,
      GregorianCalendar.from(boardingTime),
      GregorianCalendar.from(alightingTime),
      serviceDay.toLocalDate(),
      timeZone,
      null,
      null,
      ZERO_COST
    );
  }

  @Nonnull
  private static Stream<T2<TripPattern, P2<Integer>>> withBoardingAlightingPositions(
    Collection<StopLocation> origins,
    Collection<StopLocation> destinations,
    TripPattern tripPattern
  ) {
    List<StopLocation> stops = tripPattern.getStops();

    // Find out all alighting positions
    var alightingPositions = IntStream
      .iterate(stops.size() - 1, i -> i - 1)
      .limit(stops.size())
      .filter(i -> destinations.contains(stops.get(i)) && tripPattern.canAlight(i))
      .toArray();

    // Find out all boarding positions
    return IntStream
      .range(0, stops.size())
      .filter(i -> origins.contains(stops.get(i)) && tripPattern.canBoard(i))
      .boxed()
      // Create a cartesian product of the pairs
      .flatMap(boardingPosition ->
        Arrays
          .stream(alightingPositions)
          // Filter out the impossible combinations
          .filter(alightingPosition -> boardingPosition < alightingPosition)
          .mapToObj(alightingPosition -> new P2<>(boardingPosition, alightingPosition))
      )
      .map(pair -> new T2<>(tripPattern, pair));
  }
}
