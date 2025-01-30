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
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegConstructionSupport;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

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
    NavigationDirection direction,
    AlternativeLegsFilter filter
  ) {
    return getAlternativeLegs(leg, numberLegs, transitService, direction, filter, false, false);
  }

  /**
   * Searches for alternatives to a given leg prior to or later than the departure of the given leg
   *
   * @param leg                  The original leg to which alternatives are searched for
   * @param numberLegs           The number of alternative legs requested. If fewer legs are found,
   *                             only the found legs are returned.
   * @param transitService       The transit service used for the search
   * @param direction            Indicating whether the alternative legs should depart before or
   *                             after than the original.
   * @param filter               AlternativeLegsFilter indicating which properties of the original
   *                             leg should not change in the alternative legs
   * @param exactOriginStop      Boolean indicating whether the exact departure stop of the original
   *                             leg is used as departure stop for the alternatives. If false, all
   *                             stops belonging to the same parent station are used.
   * @param exactDestinationStop Boolean indicating whether the exact destination stop of the
   *                             original leg is used as destination stop for the alternatives. If
   *                             false, all stops belonging to the same parent station are used.
   */
  public static List<ScheduledTransitLeg> getAlternativeLegs(
    Leg leg,
    Integer numberLegs,
    TransitService transitService,
    NavigationDirection direction,
    AlternativeLegsFilter filter,
    boolean exactOriginStop,
    boolean exactDestinationStop
  ) {
    StopLocation fromStop = leg.getFrom().stop;
    StopLocation toStop = leg.getTo().stop;

    Station fromStation = fromStop.getParentStation();
    Station toStation = toStop.getParentStation();

    Collection<StopLocation> origins = fromStation == null || exactOriginStop
      ? List.of(fromStop)
      : fromStation.getChildStops();

    Collection<StopLocation> destinations = toStation == null || exactDestinationStop
      ? List.of(toStop)
      : toStation.getChildStops();

    Comparator<ScheduledTransitLeg> legComparator = Comparator.comparing(
      ScheduledTransitLeg::getStartTime
    );

    if (direction == NavigationDirection.PREVIOUS) {
      legComparator = legComparator.reversed();
    }

    Predicate<TripPattern> tripPatternPredicate = filter.getFilter(leg);

    return origins
      .stream()
      .flatMap(stop -> transitService.findPatterns(stop, true).stream())
      .filter(tripPattern -> tripPattern.getStops().stream().anyMatch(destinations::contains))
      .filter(tripPatternPredicate)
      .distinct()
      .flatMap(tripPattern -> withBoardingAlightingPositions(origins, destinations, tripPattern))
      .flatMap(t ->
        generateLegs(transitService, t, leg.getStartTime(), leg.getServiceDate(), direction)
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
  private static Stream<ScheduledTransitLeg> generateLegs(
    TransitService transitService,
    TripPatternBetweenStops tripPatternBetweenStops,
    ZonedDateTime departureTime,
    LocalDate originalDate,
    NavigationDirection direction
  ) {
    TripPattern pattern = tripPatternBetweenStops.tripPattern;
    int boardingPosition = tripPatternBetweenStops.positions.boardingPosition;
    int alightingPosition = tripPatternBetweenStops.positions.alightingPosition;

    // TODO: What should we have here
    ZoneId timeZone = transitService.getTimeZone();

    Comparator<TripTimeOnDate> comparator = Comparator.comparing((TripTimeOnDate tts) ->
      tts.getServiceDayMidnight() + tts.getRealtimeDeparture()
    );

    if (direction == NavigationDirection.PREVIOUS) {
      comparator = comparator.reversed();
    }

    Queue<TripTimeOnDate> pq = new PriorityQueue<>(comparator);

    // Loop through all possible days
    var serviceDates = List.of(originalDate.minusDays(1), originalDate, originalDate.plusDays(1));

    for (LocalDate serviceDate : serviceDates) {
      Timetable timetable = transitService.findTimetable(pattern, serviceDate);
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

        boolean departureTimeInRange = direction == NavigationDirection.PREVIOUS
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
      res.add(
        mapToLeg(
          timeZone,
          pattern,
          boardingPosition,
          alightingPosition,
          tripTimeOnDate,
          transitService
        )
      );
    }

    return res.stream();
  }

  private static ScheduledTransitLeg mapToLeg(
    ZoneId timeZone,
    TripPattern pattern,
    int boardingPosition,
    int alightingPosition,
    TripTimeOnDate tripTimeOnDate,
    TransitService transitService
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

    TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDate(
      new TripIdAndServiceDate(tripTimeOnDate.getTrip().getId(), tripTimeOnDate.getServiceDay())
    );

    return new ScheduledTransitLegBuilder<>()
      .withTripTimes(tripTimes)
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(boardingPosition)
      .withAlightStopIndexInPattern(alightingPosition)
      .withStartTime(boardingTime)
      .withEndTime(alightingTime)
      .withServiceDate(serviceDay)
      .withZoneId(timeZone)
      .withTripOnServiceDate(tripOnServiceDate)
      .withDistanceMeters(
        LegConstructionSupport.computeDistanceMeters(pattern, boardingPosition, alightingPosition)
      )
      .build();
  }

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
      .flatMap(boardingPosition ->
        Arrays
          .stream(alightingPositions)
          // Filter out the impossible combinations
          .filter(alightingPosition -> boardingPosition < alightingPosition)
          .min()
          .stream()
          .mapToObj(alightingPosition ->
            new BoardingAlightingPositions(boardingPosition, alightingPosition)
          )
      )
      // Group by alighting position
      .collect(Collectors.groupingBy(pair -> pair.alightingPosition))
      .values()
      .stream()
      // Find the shortest leg in each group
      .flatMap(legGroup ->
        legGroup
          .stream()
          .min(Comparator.comparing(ba -> ba.alightingPosition - ba.boardingPosition))
          .stream()
      )
      .map(pair -> new TripPatternBetweenStops(tripPattern, pair));
  }

  private record BoardingAlightingPositions(int boardingPosition, int alightingPosition) {}

  private record TripPatternBetweenStops(
    TripPattern tripPattern,
    BoardingAlightingPositions positions
  ) {}
}
