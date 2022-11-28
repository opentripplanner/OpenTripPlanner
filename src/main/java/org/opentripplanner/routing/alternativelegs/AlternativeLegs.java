package org.opentripplanner.routing.alternativelegs;

import static org.opentripplanner.routing.stoptimes.StopTimesHelper.skipByTripCancellation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

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
    TransitService transitService,
    boolean searchBackward,
    AlternativeLegsFilter filter
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

    Comparator<ScheduledTransitLeg> legComparator = Comparator.comparing(
      ScheduledTransitLeg::getStartTime
    );

    if (searchBackward) {
      legComparator = legComparator.reversed();
    }

    Predicate<TripPattern> tripPatternPredicate = filter.getFilter(leg);

    return origins
      .stream()
      .flatMap(stop -> transitService.getPatternsForStop(stop, true).stream())
      .filter(tripPattern -> tripPattern.getStops().stream().anyMatch(destinations::contains))
      .filter(tripPatternPredicate)
      .flatMap(tripPattern -> withBoardingAlightingPositions(origins, destinations, tripPattern))
      .flatMap(t ->
        generateLegs(transitService, t, leg.getStartTime(), leg.getServiceDate(), searchBackward)
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
    TransitService transitService,
    TripPatternBetweenStops tripPatternBetweenStops,
    ZonedDateTime departureTime,
    LocalDate originalDate,
    boolean searchBackward
  ) {
    TripPattern pattern = tripPatternBetweenStops.tripPattern;
    int boardingPosition = tripPatternBetweenStops.positions.boardingPosition;
    int alightingPosition = tripPatternBetweenStops.positions.alightingPosition;

    // TODO: What should we have here
    ZoneId timeZone = transitService.getTimeZone();

    Comparator<TripTimeOnDate> comparator = Comparator.comparing((TripTimeOnDate tts) ->
      tts.getServiceDayMidnight() + tts.getRealtimeDeparture()
    );

    if (searchBackward) {
      comparator = comparator.reversed();
    }

    Queue<TripTimeOnDate> pq = new PriorityQueue<>(comparator);

    // Loop through all possible days
    var serviceDates = List.of(originalDate.minusDays(1), originalDate, originalDate.plusDays(1));

    for (LocalDate serviceDate : serviceDates) {
      Timetable timetable = transitService.getTimetableForTripPattern(pattern, serviceDate);
      ZonedDateTime midnight = ServiceDateUtils.asStartOfService(
        serviceDate,
        transitService.getTimeZone()
      );
      int secondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
        midnight,
        departureTime
      );

      var servicesRunning = transitService.getServiceCodesRunningForDate(serviceDate);

      for (TripTimes tripTimes : timetable.getTripTimes()) {
        if (!servicesRunning.contains(tripTimes.getServiceCode())) {
          continue;
        }
        if (skipByTripCancellation(tripTimes, false)) {
          continue;
        }

        boolean departureTimeInRange = searchBackward
          ? tripTimes.getDepartureTime(boardingPosition) <= secondsSinceMidnight
          : tripTimes.getDepartureTime(boardingPosition) >= secondsSinceMidnight;

        if (departureTimeInRange) {
          pq.add(
            new TripTimeOnDate(
              tripTimes,
              boardingPosition,
              pattern,
              serviceDate,
              midnight.toInstant()
            )
          );
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
    LocalDate serviceDay = tripTimeOnDate.getServiceDay();
    TripTimes tripTimes = tripTimeOnDate.getTripTimes();

    ZonedDateTime boardingTime = ServiceDateUtils.toZonedDateTime(
      serviceDay,
      timeZone,
      tripTimeOnDate.getRealtimeDeparture()
    );
    ZonedDateTime alightingTime = ServiceDateUtils.toZonedDateTime(
      serviceDay,
      timeZone,
      tripTimes.getArrivalTime(alightingPosition)
    );

    return new ScheduledTransitLeg(
      tripTimes,
      pattern,
      boardingPosition,
      alightingPosition,
      boardingTime,
      alightingTime,
      serviceDay,
      timeZone,
      null,
      null,
      ZERO_COST,
      null
    );
  }

  @Nonnull
  private static Stream<TripPatternBetweenStops> withBoardingAlightingPositions(
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
          .mapToObj(alightingPosition ->
            new BoardingAlightingPositions(boardingPosition, alightingPosition)
          )
      )
      .map(pair -> new TripPatternBetweenStops(tripPattern, pair));
  }

  private record BoardingAlightingPositions(int boardingPosition, int alightingPosition) {}

  private record TripPatternBetweenStops(
    TripPattern tripPattern,
    BoardingAlightingPositions positions
  ) {}
}
