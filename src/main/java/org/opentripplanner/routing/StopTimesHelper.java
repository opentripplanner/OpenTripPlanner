package org.opentripplanner.routing;

import com.google.common.collect.MinMaxPriorityQueue;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;

public class StopTimesHelper {
  /**
   * Fetch upcoming vehicle departures from a stop.
   * It goes though all patterns passing the stop for the previous, current and next service date.
   * It uses a priority queue to keep track of the next departures. The queue is shared between all
   * dates, as services from the previous service date can visit the stop later than the current
   * service date's services. This happens eg. with sleeper trains.
   *
   * TODO: Add frequency based trips
   * @param stop Stop object to perform the search for
   * @param startTime Start time for the search. Seconds from UNIX epoch
   * @param timeRange Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param omitNonPickups If true, do not include vehicles that will not pick up passengers.
   * @param includeCancellations If true, cancelled trips will also be included in result
   */
  public static List<StopTimesInPattern> stopTimesForStop(
      RoutingService routingService,
      TimetableSnapshot timetableSnapshot,
      Stop stop,
      long startTime,
      int timeRange,
      int numberOfDepartures,
      boolean omitNonPickups,
      boolean includeCancellations
  ) {
    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    List<StopTimesInPattern> result = new ArrayList<>();
    Date date = new Date(startTime * 1000);
    ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};

    Collection<TripPattern> plannedPatterns = routingService.getPatternsForStop(stop, false);
    Collection<TripPattern> realTimePatterns = routingService.getPatternsForStop(stop, true);

    // Ensure that realtimePatterns only include realtime-departures
    realTimePatterns.removeAll(plannedPatterns);

    /*
     First, check all TripPatterns without realtime-patterns to get all planned stops. Flag to
     include realtime-cancelled trips is always set to false in this step to avoid replaced trips
     from being included since a planned trip will be cancelled when it is replaced with a modified
     stopPattern. Planned cancellations will be included based on provided parameter.
     */
    for (TripPattern pattern : plannedPatterns) {
      Queue<TripTimeShort> pq = listTripTimeShortsForPatternAtStop(
          routingService,
          timetableSnapshot,
          stop,
          pattern,
          startTime,
          timeRange,
          numberOfDepartures,
          omitNonPickups,
          serviceDates,
          false,
          includeCancellations
      );

      result.addAll(getStopTimesInPattern(pattern, pq));
    }

    /*
    Second, check realtime-TripPatterns, with the provided value for includeCancelledTrips. Planned
    cancellations is always set to false in this step.
     */
    for (TripPattern pattern : realTimePatterns) {
      Queue<TripTimeShort> pq = listTripTimeShortsForPatternAtStop(
          routingService,
          timetableSnapshot,
          stop,
          pattern,
          startTime,
          timeRange,
          numberOfDepartures,
          omitNonPickups,
          serviceDates,
          includeCancellations,
          false
      );

      result.addAll(getStopTimesInPattern(pattern, pq));
    }

    return result;
  }

  private static List<StopTimesInPattern> getStopTimesInPattern(
      TripPattern pattern, Queue<TripTimeShort> pq
  ) {
    List<StopTimesInPattern> result = new ArrayList<>();
    if (pq.size() != 0) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      while (pq.size() != 0) {
        stopTimes.times.add(0, pq.poll());
      }
      result.add(stopTimes);
    }
    return result;
  }

  /**
   * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when creating complete stop
   * timetables for a single day.
   *
   * @param stop Stop object to perform the search for
   * @param serviceDate Return all departures for the specified date
   */
  public static List<StopTimesInPattern> stopTimesForStop(
      RoutingService routingService,
      Stop stop,
      ServiceDate serviceDate,
      boolean omitNonPickups
  ) {
    List<StopTimesInPattern> ret = new ArrayList<>();

    Collection<TripPattern> patternsForStop = routingService.getPatternsForStop(stop, true);
    for (TripPattern pattern : patternsForStop) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      Timetable tt;
      TimetableSnapshot timetableSnapshot = routingService.getTimetableSnapshot();
      if (timetableSnapshot != null){
        tt = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        tt = pattern.scheduledTimetable;
      }
      ServiceDay sd = new ServiceDay(routingService.getServiceCodes(), serviceDate, routingService.getCalendarService(), pattern.route.getAgency().getId());
      int sidx = 0;
      for (Stop currStop : pattern.stopPattern.stops) {
        if (currStop == stop) {
          if(omitNonPickups && pattern.stopPattern.pickups[sidx] == StopPattern.PICKDROP_NONE) continue;
          for (TripTimes t : tt.tripTimes) {
            if (!sd.serviceRunning(t.serviceCode)) { continue; }
            stopTimes.times.add(new TripTimeShort(t, sidx, stop, sd));
          }
        }
        sidx++;
      }
      ret.add(stopTimes);
    }
    return ret;
  }

  /**
   * Fetch upcoming vehicle departures from a stop for a single pattern, passing the stop for the previous, current and
   * next service date. It uses a priority queue to keep track of the next departures. The queue is shared between all
   * dates, as services from the previous service date can visit the stop later than the current service date's
   * services.
   *
   * TODO: Add frequency based trips
   * @param stop Stop object to perform the search for
   * @param pattern Pattern object to perform the search for
   * @param startTime Start time for the search. Seconds from UNIX epoch
   * @param timeRange Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param omitNonPickups If true, do not include vehicles that will not pick up passengers.
   */
  public static List<TripTimeShort> stopTimesForPatternAtStop(
          RoutingService routingService,
          TimetableSnapshot timetableSnapshot,
          Stop stop,
          TripPattern pattern,
          long startTime,
          int timeRange,
          int numberOfDepartures,
          boolean omitNonPickups) {

    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    Date date = new Date(startTime * 1000);
    ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};
    Queue<TripTimeShort> pq = listTripTimeShortsForPatternAtStop(
        routingService,
        timetableSnapshot,
        stop,
        pattern,
        startTime,
        timeRange,
        numberOfDepartures,
        omitNonPickups,
        serviceDates,
        false,
        false
    );

    return new ArrayList<>(pq);
  }

  private static Queue<TripTimeShort> listTripTimeShortsForPatternAtStop(
      RoutingService routingService,
      TimetableSnapshot timetableSnapshot,
      Stop requestedStop,
      TripPattern pattern,
      long startTime,
      int timeRange,
      int numberOfDepartures,
      boolean omitNonPickups,
      ServiceDate[] serviceDates,
      boolean includeRealTimeCancellations,
      boolean includePlannedCancellations
  ) {

    // The bounded priority Q is used to keep a sorted short list of trip times. We can not
    // relay on the trip times to be in order because of real-time updates. This code can
    // probably be optimized, and the trip search in the Raptor search does almost the same
    // thing. This is no part of a routing request, but is a used frequently in some
    // operation like Entur for "departure boards" (apps, widgets, screens on platforms, and
    // hotel lobbies). Setting the numberOfDepartures and timeRange to a big number for a
    // transit hub could result in a DOS attack, but there are probably other more effective
    // ways to do it.
    //
    // The {@link MinMaxPriorityQueue} is marked beta, but we do not have a god alternative.
    MinMaxPriorityQueue<TripTimeShort> result = MinMaxPriorityQueue
        .orderedBy(Comparator.comparing((TripTimeShort tts) -> tts.getServiceDay()
            + tts.getRealtimeDeparture()))
        .maximumSize(numberOfDepartures)
        .create();

    // Loop through all possible days
    for (ServiceDate serviceDate : serviceDates) {
      ServiceDay serviceDay = new ServiceDay(
          routingService.getServiceCodes(),
          serviceDate,
          routingService.getCalendarService(),
          pattern.route.getAgency().getId()
      );
      Timetable timetable;
      if (timetableSnapshot != null) {
        timetable = timetableSnapshot.resolve(pattern, serviceDate);
      }
      else {
        timetable = pattern.scheduledTimetable;
      }

      // Short-circuiting must be delayed in the case of includeRealTimeCancellations
      if (!includeRealTimeCancellations
          && !timetable.temporallyViable(serviceDay, startTime, timeRange,true)) {
        continue;
      }

      int secondsSinceMidnight = serviceDay.secondsSinceMidnight(startTime);
      int stopIndex = 0;
      for (Stop currentStop : pattern.stopPattern.stops) {
        if (currentStop == requestedStop) {
          // Short-circuiting must be delayed in the case of includeRealTimeCancellations
          if (!includeRealTimeCancellations) {
            if (omitNonPickups
                && pattern.stopPattern.pickups[stopIndex] == StopPattern.PICKDROP_NONE) {
              continue;
            }
          }
          for (TripTimes tripTimes : timetable.tripTimes) {
            // Short-circuiting must be delayed in the case of includeRealTimeCancellations
            if (!includeRealTimeCancellations
                && !serviceDay.serviceRunning(tripTimes.serviceCode)) { continue; }

            boolean stopOrTripIsCancelled = tripTimes.stopOrTripIsCancelled(stopIndex);

            boolean includeByCancellation = !stopOrTripIsCancelled
                || includePlannedCancellations
                || includeRealTimeCancellations;

            boolean includeByDepartureTime =
                tripTimes.getDepartureTime(stopIndex) >= secondsSinceMidnight
                    && tripTimes.getDepartureTime(stopIndex) <= secondsSinceMidnight + timeRange;

            if (includeByDepartureTime && includeByCancellation) {
              result.add(new TripTimeShort(tripTimes, stopIndex, requestedStop, serviceDay));
            }
          }

          // TODO: This needs to be adapted after #1647 is merged
          for (FrequencyEntry frequencyEntry : timetable.frequencyEntries) {
            if (!serviceDay.serviceRunning(frequencyEntry.tripTimes.serviceCode)) { continue; }
            int departureTime = frequencyEntry.nextDepartureTime(stopIndex, secondsSinceMidnight);
            if (departureTime == -1) { continue; }
            int lastDeparture =
                frequencyEntry.endTime + frequencyEntry.tripTimes.getArrivalTime(stopIndex)
                    - frequencyEntry.tripTimes.getDepartureTime(0);
            int i = 0;
            while (departureTime <= lastDeparture && i < numberOfDepartures) {
              result.add(new TripTimeShort(frequencyEntry.materialize(
                  stopIndex,
                  departureTime,
                  true
              ),
                  stopIndex,
                  requestedStop,
                  serviceDay
              ));
              departureTime += frequencyEntry.headway;
              i++;
            }
          }
        }
        stopIndex++;
      }
    }
    return result;
  }
}
