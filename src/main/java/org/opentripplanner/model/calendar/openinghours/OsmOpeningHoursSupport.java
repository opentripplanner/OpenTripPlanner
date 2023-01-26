package org.opentripplanner.model.calendar.openinghours;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.opentripplanner.framework.time.TimeUtils;

public class OsmOpeningHoursSupport {

  /**
   * This converts instances of {@link OHCalendar} to an OSM-formatted opening hours string. It
   * works best if the periodDescriptions of the opening hours is already in an OSM formatted string
   * but a limited number of other formats (like the HSL one) are also supported. The conversion
   * happens on best effort basis.
   */
  public static String osmFormat(OHCalendar calendar) {
    return calendar
      .openingHours()
      .stream()
      .map(OsmOpeningHoursSupport::osmFormat)
      .collect(Collectors.joining("; "));
  }

  /**
   * This converts instances of {@link OpeningHours} to an OSM-formatted opening hours string. It
   * works best if the periodDescription of the opening hours is already in an OSM formatted string
   * but a limited number of other formats (like the HSL one) are also supported. The conversion
   * happens on best effort basis.
   */
  public static String osmFormat(OpeningHours oh) {
    return (
      toOsm(oh.periodDescription()) +
      " " +
      TimeUtils.timeToStrCompact(truncateToMinute(oh.startTime())) +
      "-" +
      TimeUtils.timeToStrCompact(truncateToMinute(oh.endTime()))
    );
  }

  private static int truncateToMinute(long startTime) {
    return (int) Duration.ofSeconds(startTime).truncatedTo(ChronoUnit.MINUTES).toSeconds();
  }

  private static String toOsm(String description) {
    return switch (description.toLowerCase()) {
      case "business days" -> "Mo-Fr";
      case "monday" -> "Mo";
      case "tuesday" -> "Tu";
      case "wednesday" -> "We";
      case "thursday" -> "Th";
      case "friday" -> "Fr";
      case "saturday" -> "Sa";
      case "sunday" -> "Su";
      case "every day" -> "Mo-Su";
      default -> description;
    };
  }
}
