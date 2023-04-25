package org.opentripplanner.netex.support;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Utility class with helpers for NeTEx ServiceJourney.
 */
public class ServiceJourneyHelper {

  private ServiceJourneyHelper() {}

  /**
   * Return the JourneyPattern ID of a given ServiceJourney.
   */
  public static String getPatternId(ServiceJourney sj) {
    return sj.getJourneyPatternRef().getValue().getRef();
  }

  /**
   * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
   */
  public static String getStopPointId(TimetabledPassingTime timetabledPassingTime) {
    return timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
  }

  /**
   * Return the elapsed time since midnight for a given local time, taking into account the day
   * offset.
   */
  public static Duration getElapsedTimeSinceMidnight(LocalTime time, BigInteger dayOffset) {
    return getElapsedTimeSinceMidnight(time, getDayOffset(dayOffset));
  }

  private static Duration getElapsedTimeSinceMidnight(LocalTime time, int dayOffset) {
    return Duration.between(LocalTime.MIDNIGHT, time).plus(Duration.ofDays(dayOffset));
  }

  /**
   * Return the elapsed time since midnight for a given departure time, taking into account the day
   * offset. Fallback to arrival time if departure time is missing. Return null if neither the
   * departure time nor the arrival time are set (which is the case for flex stops).
   */
  public static Duration getElapsedDepartureOrArrivalTimeSinceMidnight(
    TimetabledPassingTime timetabledPassingTime
  ) {
    if (timetabledPassingTime.getDepartureTime() != null) {
      return getElapsedTimeSinceMidnight(
        timetabledPassingTime.getDepartureTime(),
        timetabledPassingTime.getDepartureDayOffset()
      );
    } else if (timetabledPassingTime.getArrivalTime() != null) {
      return getElapsedTimeSinceMidnight(
        timetabledPassingTime.getArrivalTime(),
        timetabledPassingTime.getArrivalDayOffset()
      );
    }
    return null;
  }

  /**
   * Sort the timetabled passing times according to their order in the journey pattern.
   */
  public static List<TimetabledPassingTime> getOrderedPassingTimes(
    JourneyPattern_VersionStructure journeyPattern,
    ServiceJourney serviceJourney
  ) {
    Map<String, Integer> stopPointIdToOrder = journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .collect(Collectors.toMap(EntityStructure::getId, point -> point.getOrder().intValueExact()));
    return serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .stream()
      .sorted(
        Comparator.comparing(timetabledPassingTime ->
          stopPointIdToOrder.get(getStopPointId(timetabledPassingTime))
        )
      )
      .toList();
  }

  private static int getDayOffset(BigInteger offset) {
    return offset != null ? offset.intValueExact() : 0;
  }
}
