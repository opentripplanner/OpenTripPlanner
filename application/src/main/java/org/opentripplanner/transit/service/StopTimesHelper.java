package org.opentripplanner.transit.service;

import static org.opentripplanner.transit.service.ArrivalDeparture.ARRIVALS;
import static org.opentripplanner.transit.service.ArrivalDeparture.DEPARTURES;
import static org.opentripplanner.utils.time.ServiceDateUtils.calculateRunningDates;

import com.google.common.collect.MinMaxPriorityQueue;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateMatcherFactory;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class StopTimesHelper {

  private final TransitService transitService;

  StopTimesHelper(TransitService transitService) {
    this.transitService = transitService;
  }

  /**
   * Fetch upcoming vehicle departures from a stop. It goes through all patterns passing the stop
   * for the given time-window `[startTime, startTime+timeRange]`. It uses a priority queue to keep
   * track of the next departures. The queue is shared between all dates, as services from earlier
   * service dates can visit the stop later than the current service date's services. This happens
   * with sleeper trains and multi-day services.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop                  Stop object to perform the search for
   * @param startTime             Start time for the search.
   * @param timeRange             Searches forward for timeRange from startTime
   * @param numberOfDepartures    Number of departures to fetch per pattern
   * @param arrivalDeparture      Filter by arrivals, departures, or both
   * @param includeCancelledTrips If true, cancelled trips will also be included in result
   */
  List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips,
    Comparator<TripTimeOnDate> sortOrder
  ) {
    if (numberOfDepartures <= 0) {
      return List.of();
    }

    List<StopTimesInPattern> result = new ArrayList<>();

    // Fetch all patterns, including those from realtime sources
    Collection<TripPattern> patterns = transitService.findPatterns(stop, true);

    for (TripPattern pattern : patterns) {
      Queue<TripTimeOnDate> pq = listTripTimeOnDatesForPatternAtStop(
        stop,
        pattern,
        startTime,
        timeRange,
        numberOfDepartures,
        arrivalDeparture,
        includeCancelledTrips,
        sortOrder
      );

      result.addAll(getStopTimesInPattern(pattern, pq));
    }

    return result;
  }

  List<TripTimeOnDate> findTripTimesOnDate(TripTimeOnDateRequest request) {
    Matcher<TripTimeOnDate> matcher = TripTimeOnDateMatcherFactory.of(request);
    return request
      .stopLocations()
      .stream()
      .flatMap(stopLocation ->
        stopTimesForStop(
          stopLocation,
          request.time(),
          request.timeWindow(),
          request.numberOfDepartures(),
          request.arrivalDeparture(),
          true,
          request.sortOrder()
        )
          .stream()
          .flatMap(st -> st.times.stream())
          .filter(matcher::match)
      )
      .sorted(request.sortOrder())
      .toList();
  }

  /**
   * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when
   * creating complete stop timetables for a single day.
   *
   * @param stop        Stop object to perform the search for
   * @param serviceDate Return all departures for the specified date
   */
  List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  ) {
    List<StopTimesInPattern> ret = new ArrayList<>();

    var servicesRunning = transitService.getServiceCodesRunningForDate(serviceDate);
    Instant midnight = ServiceDateUtils.asStartOfService(
      serviceDate,
      transitService.getTimeZone()
    ).toInstant();

    for (TripPattern pattern : transitService.findPatterns(stop, true)) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      Timetable tt = transitService.findTimetable(pattern, serviceDate);
      List<StopLocation> stops = pattern.getStops();
      for (int i = 0; i < stops.size(); i++) {
        StopLocation currStop = stops.get(i);
        if (currStop == stop) {
          if (skipByPickUpDropOff(pattern, arrivalDeparture, i)) {
            continue;
          }
          if (skipByStopCancellation(pattern, includeCancellations, i)) {
            continue;
          }
          for (TripTimes t : tt.getTripTimes()) {
            if (TripTimesHelper.skipByTripCancellationOrDeletion(t, includeCancellations)) {
              continue;
            }
            if (servicesRunning.contains(t.getServiceCode())) {
              stopTimes.times.add(new TripTimeOnDate(t, i, pattern, serviceDate, midnight));
            }
          }
        }
      }
      ret.add(stopTimes);
    }
    return ret;
  }

  /**
   * Fetch upcoming vehicle departures from a stop for a single pattern, passing the stop for the
   * previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop                 Stop object to perform the search for
   * @param pattern              Pattern object to perform the search for
   * @param startTime            Start time for the search.
   * @param timeRange            Searches forward for timeRange from startTime
   * @param numberOfDepartures   Number of departures to fetch per pattern
   * @param arrivalDeparture     Filter by arrivals, departures, or both.
   * @param includeCancellations If the result should include those trip times where either the entire
   *                             trip or the stop at the given stop location has been cancelled.
   *                             Deleted trips are never returned no matter the value of this parameter.
   */
  List<TripTimeOnDate> stopTimesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  ) {
    Queue<TripTimeOnDate> pq = listTripTimeOnDatesForPatternAtStop(
      stop,
      pattern,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      includeCancellations,
      TripTimeOnDate.compareByDeparture()
    );

    return new ArrayList<>(pq);
  }

  private static List<StopTimesInPattern> getStopTimesInPattern(
    TripPattern pattern,
    Queue<TripTimeOnDate> pq
  ) {
    List<StopTimesInPattern> result = new ArrayList<>();
    if (!pq.isEmpty()) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      while (!pq.isEmpty()) {
        stopTimes.times.add(0, pq.poll());
      }
      result.add(stopTimes);
    }
    return result;
  }

  private Queue<TripTimeOnDate> listTripTimeOnDatesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations,
    Comparator<TripTimeOnDate> sortOrder
  ) {
    ZoneId zoneId = transitService.getTimeZone();

    // The bounded priority Q is used to keep a sorted short list of trip times. We can not
    // rely on the trip times to be in order because of real-time updates. This code can
    // probably be optimized, and the trip search in the Raptor search does almost the same
    // thing. This is not part of a routing request, but is a used frequently in some
    // operation like Entur for "departure boards" (apps, widgets, screens on platforms, and
    // hotel lobbies). Setting the numberOfDepartures and timeRange to a big number for a
    // transit hub could result in a DOS attack, but there are probably other more effective
    // ways to do it.
    //
    MinMaxPriorityQueue<TripTimeOnDate> pq = MinMaxPriorityQueue.orderedBy(sortOrder)
      .maximumSize(numberOfDepartures)
      .create();

    int timeRangeSeconds = (int) timeRange.toSeconds();
    int maxTripSpanDays = pattern.getScheduledTimetable().getMaxTripSpanDays();

    // The `maxTripSpanDays + 1` is used to "overselect" the running-dates to account for up to
    // 24h delays. This had a performance overhead of ~25% (Bergen, Norway), so we check if there are delays
    // in the loop below and remove the extra day if not.
    var runningDates = calculateRunningDates(startTime, timeRange, zoneId, maxTripSpanDays + 1);
    var firstDay = runningDates.getFirst();

    for (LocalDate serviceDate : runningDates) {
      Timetable timetable = transitService.findTimetable(pattern, serviceDate);

      // Skip the first running date if the maxTripSpanDays is the same for the scheduled
      // and the realtime timetable. We overselected the runing dates in case the realtime
      // timetable was delayed into the next service-day. Note! If no realtime data exist this
      // check is true, and the first running date is skiped.
      if (firstDay == serviceDate && maxTripSpanDays == timetable.getMaxTripSpanDays()) {
        continue;
      }

      var serviceDateMidnight = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
      int secondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
        serviceDateMidnight,
        ZonedDateTime.ofInstant(startTime, zoneId)
      );
      var servicesRunning = transitService.getServiceCodesRunningForDate(serviceDate);

      List<StopLocation> stops = pattern.getStops();
      for (int stopPos = 0; stopPos < stops.size(); stopPos++) {
        StopLocation currStop = stops.get(stopPos);
        if (currStop == stop) {
          if (skipByPickUpDropOff(pattern, arrivalDeparture, stopPos)) {
            continue;
          }
          if (skipByStopCancellation(pattern, includeCancellations, stopPos)) {
            continue;
          }

          for (TripTimes tripTimes : timetable.getTripTimes()) {
            if (!servicesRunning.contains(tripTimes.getServiceCode())) {
              continue;
            }
            if (TripTimesHelper.skipByTripCancellationOrDeletion(tripTimes, includeCancellations)) {
              continue;
            }

            boolean departureTimeInRange =
              tripTimes.getDepartureTime(stopPos) >= secondsSinceMidnight &&
              tripTimes.getDepartureTime(stopPos) <= secondsSinceMidnight + timeRangeSeconds;

            boolean arrivalTimeInRange =
              tripTimes.getArrivalTime(stopPos) >= secondsSinceMidnight &&
              tripTimes.getArrivalTime(stopPos) <= secondsSinceMidnight + timeRangeSeconds;

            // ARRIVAL: Arrival time has to be within range
            // DEPARTURES: Departure time has to be within range
            // BOTH: Either arrival time or departure time has to be within range
            if (
              (arrivalDeparture != ARRIVALS && departureTimeInRange) ||
              (arrivalDeparture != DEPARTURES && arrivalTimeInRange)
            ) {
              pq.add(
                new TripTimeOnDate(
                  tripTimes,
                  stopPos,
                  pattern,
                  serviceDate,
                  serviceDateMidnight.toInstant()
                )
              );
            }
          }
          // TODO Add back support for frequency entries
        }
      }
    }
    return pq;
  }

  private static boolean skipByPickUpDropOff(
    TripPattern pattern,
    ArrivalDeparture arrivalDeparture,
    int stopPos
  ) {
    boolean noPickup = pattern.getBoardType(stopPos).is(PickDrop.NONE);
    boolean noDropoff = pattern.getAlightType(stopPos).is(PickDrop.NONE);

    if (noPickup && noDropoff) {
      return true;
    }
    if (noPickup && arrivalDeparture == DEPARTURES) {
      return true;
    }
    if (noDropoff && arrivalDeparture == ARRIVALS) {
      return true;
    }
    return false;
  }

  private static boolean skipByStopCancellation(
    TripPattern pattern,
    boolean includeCancelled,
    int stopPos
  ) {
    boolean pickupCancelled = pattern.getBoardType(stopPos).is(PickDrop.CANCELLED);
    boolean dropOffCancelled = pattern.getAlightType(stopPos).is(PickDrop.CANCELLED);

    return (pickupCancelled || dropOffCancelled) && !includeCancelled;
  }
}
