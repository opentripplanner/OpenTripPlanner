package org.opentripplanner.apis.transmodel.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TripTimeOnDateHelper {

  /** Utility class with private constructor to prevent instantiation. */
  private TripTimeOnDateHelper() {}

  /**
   * Find trip time short for the from place in transit leg, or null.
   */
  @Nullable
  public static TripTimeOnDate getTripTimeOnDateForFromPlace(Leg leg) {
    if (!leg.isScheduledTransitLeg()) {
      return null;
    }
    ScheduledTransitLeg transitLeg = leg.asScheduledTransitLeg();
    return new TripTimeOnDate(
      transitLeg.getTripTimes(),
      transitLeg.getBoardStopPosInPattern(),
      transitLeg.getTripPattern(),
      transitLeg.getServiceDate(),
      transitLeg.getServiceDateMidnight()
    );
    /* TODO OTP2 This method is only used for EstimatedCalls for from place. We have to decide
                     if EstimatedCalls are applicable to flex trips, and if that is the case, add
                     the necessary mappings.
        if (leg.isFlexible()) {
            TripTimeShort tripTimeShort = tripTimes.get(leg.from.stopSequence);
            tripTimeShort.scheduledDeparture = (int) startTimeSeconds;
            tripTimeShort.realtimeDeparture = (int) startTimeSeconds;
            return tripTimeShort;
        }
         */
  }

  /**
   * Find trip time short for the to place in transit leg, or null.
   */
  @Nullable
  public static TripTimeOnDate getTripTimeOnDateForToPlace(Leg leg) {
    if (!leg.isScheduledTransitLeg()) {
      return null;
    }
    ScheduledTransitLeg transitLeg = leg.asScheduledTransitLeg();
    return new TripTimeOnDate(
      transitLeg.getTripTimes(),
      transitLeg.getAlightStopPosInPattern(),
      transitLeg.getTripPattern(),
      transitLeg.getServiceDate(),
      transitLeg.getServiceDateMidnight()
    );
    /* TODO OTP2 This method is only used for EstimatedCalls for to place. We have to decide
                     if EstimatedCalls are applicable to flex trips, and if that is the case, add
                     the necessary mappings.
        if (leg.isFlexible()) {
            TripTimeShort tripTimeShort = tripTimes.get(leg.to.stopSequence);
            tripTimeShort.scheduledArrival = (int) endTimeSeconds;
            tripTimeShort.realtimeArrival = (int) endTimeSeconds;
            return tripTimeShort;
        }
        */

  }

  /**
   * Find trip time shorts for all stops for the full trip of a leg.
   */
  public static List<TripTimeOnDate> getAllTripTimeOnDatesForLegsTrip(Leg leg) {
    if (!leg.isScheduledTransitLeg()) {
      return List.of();
    }
    ScheduledTransitLeg transitLeg = leg.asScheduledTransitLeg();
    TripTimes tripTimes = transitLeg.getTripTimes();
    TripPattern tripPattern = transitLeg.getTripPattern();
    Instant serviceDateMidnight = transitLeg.getServiceDateMidnight();
    LocalDate serviceDate = transitLeg.getServiceDate();
    return IntStream.range(0, tripPattern.numberOfStops())
      .mapToObj(i -> new TripTimeOnDate(tripTimes, i, tripPattern, serviceDate, serviceDateMidnight)
      )
      .collect(Collectors.toList());
  }

  /**
   * Find trip time shorts for all intermediate stops for a leg.
   */
  public static List<TripTimeOnDate> getIntermediateTripTimeOnDatesForLeg(Leg leg) {
    if (!leg.isScheduledTransitLeg()) {
      return List.of();
    }
    ScheduledTransitLeg transitLeg = leg.asScheduledTransitLeg();
    TripTimes tripTimes = transitLeg.getTripTimes();
    TripPattern tripPattern = transitLeg.getTripPattern();
    Instant serviceDateMidnight = transitLeg.getServiceDateMidnight();
    LocalDate serviceDate = transitLeg.getServiceDate();
    return IntStream.range(leg.getBoardStopPosInPattern() + 1, leg.getAlightStopPosInPattern())
      .mapToObj(i -> new TripTimeOnDate(tripTimes, i, tripPattern, serviceDate, serviceDateMidnight)
      )
      .collect(Collectors.toList());
  }
}
