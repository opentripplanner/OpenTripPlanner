package org.opentripplanner.routing.stoptimes;

import com.google.common.collect.MinMaxPriorityQueue;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.stoptimes.ArrivalDeparture.ARRIVALS;
import static org.opentripplanner.routing.stoptimes.ArrivalDeparture.DEPARTURES;

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
   * @param arrivalDeparture Filter by arrivals, departures, or both
   * @param includeCancelledTrips If true, cancelled trips will also be included in result
   */
  public static List<StopTimesInPattern> stopTimesForStop(
      RoutingService routingService,
      TimetableSnapshot timetableSnapshot,
      Stop stop,
      long startTime,
      int timeRange,
      int numberOfDepartures,
      ArrivalDeparture arrivalDeparture,
      boolean includeCancelledTrips
  ) {
    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    List<StopTimesInPattern> result = new ArrayList<>();
    Date date = new Date(startTime * 1000);
    ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};

    // TODO The following logic could probably be encapsulated in the TimetableSnapshot
    Collection<TripPattern> plannedPatterns = routingService.getPatternsForStop(stop, false);
    Collection<TripPattern> realTimePatterns = routingService.getPatternsForStop(stop, true);

    // Only include the planned patterns that are not also part of the collection of realtime
    // patterns. These may have been cancelled with no replacement.
    plannedPatterns.removeAll(realTimePatterns);

    // Realtime patterns may have been cancelled and replaced. Do not include the patterns that
    // have been replaced
    Collection<TripPattern> replacedPatterns = realTimePatterns
        .stream()
        .map(TripPattern::getOriginalTripPattern)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    realTimePatterns.removeAll(replacedPatterns);

    /*
     First, check all the TripPatterns that are planned, but not part of the realtime patterns. This
     is so that we catch TripPatterns that have been cancelled by realtime updates.
     */
    for (TripPattern pattern : plannedPatterns) {
      Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
          routingService,
          timetableSnapshot,
          stop,
          pattern,
          startTime,
          timeRange,
          numberOfDepartures,
          arrivalDeparture,
          includeCancelledTrips,
          serviceDates
      );

      result.addAll(getStopTimesInPattern(pattern, pq));
    }

    /*
    Second, check all the realtime-updated TripPatterns.
     */
    for (TripPattern pattern : realTimePatterns) {
      Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
          routingService,
          timetableSnapshot,
          stop,
          pattern,
          startTime,
          timeRange,
          numberOfDepartures,
          arrivalDeparture,
          includeCancelledTrips,
          serviceDates
      );

      result.addAll(getStopTimesInPattern(pattern, pq));
    }

    return result;
  }

  private static List<StopTimesInPattern> getStopTimesInPattern(
      TripPattern pattern, Queue<TripTimeOnDate> pq
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
      ArrivalDeparture arrivalDeparture
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
        tt = pattern.getScheduledTimetable();
      }
      ServiceDay sd = new ServiceDay(routingService.getServiceCodes(), serviceDate, routingService.getCalendarService(), pattern
          .getRoute()
          .getAgency().getId());
      int sidx = 0;
      for (Stop currStop : pattern.getStopPattern().getStops()) {
        if (currStop == stop) {
          if(skipByPickUpDropOff(pattern, arrivalDeparture, sidx)) continue;
          for (TripTimes t : tt.getTripTimes()) {
            if (!sd.serviceRunning(t.getServiceCode())) { continue; }
            stopTimes.times.add(new TripTimeOnDate(t, sidx, pattern, sd));
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
   * @param arrivalDeparture Filter by arrivals, departures, or both.
   */
  public static List<TripTimeOnDate> stopTimesForPatternAtStop(
          RoutingService routingService,
          TimetableSnapshot timetableSnapshot,
          Stop stop,
          TripPattern pattern,
          long startTime,
          int timeRange,
          int numberOfDepartures,
          ArrivalDeparture arrivalDeparture) {

    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    Date date = new Date(startTime * 1000);
    ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};
    Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
        routingService,
        timetableSnapshot,
        stop,
        pattern,
        startTime,
        timeRange,
        numberOfDepartures,
        arrivalDeparture,
        false,
        serviceDates
    );

    return new ArrayList<>(pq);
  }

  private static Queue<TripTimeOnDate> listTripTimeShortsForPatternAtStop(
      RoutingService routingService,
      TimetableSnapshot timetableSnapshot,
      Stop stop,
      TripPattern pattern,
      long startTime,
      int timeRange,
      int numberOfDepartures,
      ArrivalDeparture arrivalDeparture,
      boolean includeCancellations,
      ServiceDate[] serviceDates
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
    MinMaxPriorityQueue<TripTimeOnDate> pq = MinMaxPriorityQueue
            .orderedBy(Comparator.comparing((TripTimeOnDate tts) -> tts.getServiceDay()
                + tts.getRealtimeDeparture()))
            .maximumSize(numberOfDepartures)
            .create();

    // Loop through all possible days
    for (ServiceDate serviceDate : serviceDates) {
      ServiceDay sd = new ServiceDay(routingService.getServiceCodes(), serviceDate, routingService.getCalendarService(), pattern
          .getRoute()
          .getAgency().getId());
      Timetable timetable;
      if (timetableSnapshot != null) {
        timetable = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        timetable = pattern.getScheduledTimetable();
      }

      int secondsSinceMidnight = sd.secondsSinceMidnight(startTime);
      int stopIndex = 0;
      for (Stop currStop : pattern.getStopPattern().getStops()) {
        if (currStop == stop) {

          if (skipByPickUpDropOff(pattern, arrivalDeparture, stopIndex)) { continue; }
          if (skipByStopCancellation(pattern, includeCancellations, stopIndex)) { continue; }

          for (TripTimes tripTimes : timetable.getTripTimes()) {
            if (!sd.serviceRunning(tripTimes.getServiceCode())) { continue; }
            if (skipByTripCancellation(tripTimes, includeCancellations)) { continue; }

            boolean departureTimeInRange =
                tripTimes.getDepartureTime(stopIndex) >= secondsSinceMidnight
                    && tripTimes.getDepartureTime(stopIndex) <= secondsSinceMidnight + timeRange;

            boolean arrivalTimeInRange =
                tripTimes.getArrivalTime(stopIndex) >= secondsSinceMidnight
                    && tripTimes.getArrivalTime(stopIndex) <= secondsSinceMidnight + timeRange;

            // ARRIVAL: Arrival time has to be within range
            // DEPARTURES: Departure time has to be within range
            // BOTH: Either arrival time or departure time has to be within range
            if ((arrivalDeparture != ARRIVALS && departureTimeInRange)
                || (arrivalDeparture != DEPARTURES && arrivalTimeInRange)) {
              pq.add(new TripTimeOnDate(tripTimes, stopIndex, pattern, sd));
            }
          }

          // TODO Add back support for frequency entries
        }
        stopIndex++;
      }
    }
    return pq;
  }

  private static boolean skipByTripCancellation(TripTimes tripTimes, boolean includeCancellations) {
    return (tripTimes.isCanceled()
        || tripTimes.getTrip().getTripAlteration().isCanceledOrReplaced())
        && !includeCancellations;
  }

  private static boolean skipByPickUpDropOff(
      TripPattern pattern, ArrivalDeparture arrivalDeparture, int stopIndex
  ) {
    boolean pickup = pattern.getStopPattern().getPickup(stopIndex) != PickDrop.NONE;
    boolean dropoff = pattern.getStopPattern().getDropoff(stopIndex) != PickDrop.NONE;

    if (!pickup && !dropoff)
      return true;
    if (!pickup && arrivalDeparture == DEPARTURES)
      return true;
    if (!dropoff && arrivalDeparture == ARRIVALS)
      return true;
    return false;
  }

  private static boolean skipByStopCancellation(
      TripPattern pattern, boolean includeCancelledTrips, int stopIndex
  ) {
    boolean pickupCancelled = pattern.getStopPattern().getPickup(stopIndex) == PickDrop.CANCELLED;
    boolean dropOffCancelled = pattern.getStopPattern().getDropoff(stopIndex) == PickDrop.CANCELLED;

    return (pickupCancelled || dropOffCancelled) && !includeCancelledTrips;
  }
}
