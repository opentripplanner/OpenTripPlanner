package org.opentripplanner.netex.support;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
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
   * Return the mapping between stop point id and scheduled stop point id
   * for a given a journey pattern.
   */
  public static Map<String, String> getScheduledStopPointIdByStopPointId(
    JourneyPattern_VersionStructure journeyPattern
  ) {
    return journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .collect(
        Collectors.toMap(
          EntityStructure::getId,
          p -> ((StopPointInJourneyPattern) p).getScheduledStopPointRef().getValue().getRef()
        )
      );
  }

  /**
   * Return the elapsed time in second between midnight and the departure time,
   * taking into account the day offset.
   */
  public static int normalizedDepartureTime(TimetabledPassingTime timetabledPassingTime) {
    Objects.requireNonNull(timetabledPassingTime.getDepartureTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getDepartureTime(),
      timetabledPassingTime.getDepartureDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time,
   * taking into account the day offset.
   */
  public static int normalizedArrivalTime(TimetabledPassingTime timetabledPassingTime) {
    Objects.requireNonNull(timetabledPassingTime.getArrivalTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getArrivalTime(),
      timetabledPassingTime.getArrivalDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the earliest departure time,
   * taking into account the day offset.
   */
  public static int normalizedEarliestDepartureTime(TimetabledPassingTime timetabledPassingTime) {
    Objects.requireNonNull(timetabledPassingTime.getEarliestDepartureTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getEarliestDepartureTime(),
      timetabledPassingTime.getEarliestDepartureDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the latest arrival time,
   * taking into account the day offset.
   */
  public static int normalizedLatestArrivalTime(TimetabledPassingTime timetabledPassingTime) {
    Objects.requireNonNull(timetabledPassingTime.getLatestArrivalTime());
    return elapsedTimeSinceMidnight(
      timetabledPassingTime.getLatestArrivalTime(),
      timetabledPassingTime.getLatestArrivalDayOffset()
    );
  }

  /**
   * Return the elapsed time in second between midnight and the departure time,
   * taking into account the day offset. Fallback to arrival time if departure time is missing.
   */
  public static int normalizedDepartureTimeOrElseArrivalTime(
    TimetabledPassingTime timetabledPassingTime
  ) {
    if (timetabledPassingTime.getDepartureTime() != null) {
      return elapsedTimeSinceMidnight(
        timetabledPassingTime.getDepartureTime(),
        timetabledPassingTime.getDepartureDayOffset()
      );
    } else {
      return elapsedTimeSinceMidnight(
        timetabledPassingTime.getArrivalTime(),
        timetabledPassingTime.getArrivalDayOffset()
      );
    }
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time,
   * taking into account the day offset. Fallback to departure time if arrival time is missing.
   */
  public static int normalizedArrivalTimeOrElseDepartureTime(
    TimetabledPassingTime timetabledPassingTime
  ) {
    if (timetabledPassingTime.getArrivalTime() != null) {
      return elapsedTimeSinceMidnight(
        timetabledPassingTime.getArrivalTime(),
        timetabledPassingTime.getArrivalDayOffset()
      );
    } else return elapsedTimeSinceMidnight(
      timetabledPassingTime.getDepartureTime(),
      timetabledPassingTime.getDepartureDayOffset()
    );
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

  /**
   * Return the elapsed time in second since midnight for a given local time,
   * taking into account the day offset.
   */
  private static int elapsedTimeSinceMidnight(LocalTime time, BigInteger dayOffset) {
    return elapsedTimeSinceMidnight(time, getDayOffset(dayOffset));
  }

  private static int elapsedTimeSinceMidnight(LocalTime time, int dayOffset) {
    return (int) Duration
      .between(LocalTime.MIDNIGHT, time)
      .plus(Duration.ofDays(dayOffset))
      .toSeconds();
  }

  private static int getDayOffset(BigInteger offset) {
    return offset != null ? offset.intValueExact() : 0;
  }

  /**
   * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
   */
  private static String getStopPointId(TimetabledPassingTime timetabledPassingTime) {
    return timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
  }
}
