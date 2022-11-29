package org.opentripplanner.street.model;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

/**
 * Represents a repeating time period, used for opening hours etc. For instance: Monday - Friday 8AM
 * to 8PM, Satuday 10AM to 5PM, Sunday closed. For now it is week-based so doesn't handle every
 * possible case, but since it is encapsulated that could conceivably be changed.
 *
 * @author mattwigway
 */
public class RepeatingTimePeriod implements Serializable {

  /**
   * The timezone this is represented in.
   */
  private final ZoneId timeZone;
  /**
   * This stores the time periods this is active/open, stored as seconds from noon (positive or
   * negative) on the given day.
   */
  private int[][] monday;
  private int[][] tuesday;
  private int[][] wednesday;
  private int[][] thursday;
  private int[][] friday;
  private int[][] saturday;
  private int[][] sunday;

  public RepeatingTimePeriod(ZoneId timeZone) {
    this.timeZone = timeZone;
  }

  /**
   * Parse the time specification from an OSM turn restriction
   */
  public static RepeatingTimePeriod parseFromOsmTurnRestriction(
    String day_on,
    String day_off,
    String hour_on,
    String hour_off,
    Supplier<ZoneId> timeZoneSupplier
  ) {
    ZoneId timeZone = timeZoneSupplier.get();
    if (timeZone == null) {
      return null;
    }

    // first, create the opening and closing times. This is easy because there is the same one
    // every day of the week that this restriction is in force.
    String[] parsedOn = hour_on.split(";");
    String[] parsedOff = hour_off.split(";");
    if (parsedOn.length != parsedOff.length) {
      return null;
    }

    int[][] onOff = new int[parsedOn.length][];

    for (int i = 0; i < parsedOn.length; i++) {
      onOff[i] = new int[] { parseHour(parsedOn[i]), parseHour(parsedOff[i]) };
    }

    boolean active = false;
    RepeatingTimePeriod ret = new RepeatingTimePeriod(timeZone);

    // loop through twice to handle cases like Saturday - Tuesday
    for (String today : new String[] {
      "monday",
      "tuesday",
      "wednesday",
      "thursday",
      "friday",
      "saturday",
      "sunday",
      "monday",
      "tuesday",
      "wednesday",
      "thursday",
      "friday",
      "saturday",
      "sunday",
    }) {
      if (today.startsWith(day_on.toLowerCase())) active = true;

      if (active) {
        switch (today) {
          case "monday" -> ret.monday = onOff;
          case "tuesday" -> ret.tuesday = onOff;
          case "wednesday" -> ret.wednesday = onOff;
          case "thursday" -> ret.thursday = onOff;
          case "friday" -> ret.friday = onOff;
          case "saturday" -> ret.saturday = onOff;
          case "sunday" -> ret.sunday = onOff;
        }
      }

      if (today.startsWith(day_off.toLowerCase())) {
        active = false;
      }
    }

    return ret;
  }

  public boolean active(long time) {
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), timeZone);
    DayOfWeek dayOfWeek = zonedDateTime.getDayOfWeek();

    int[][] times =
      switch (dayOfWeek) {
        case MONDAY -> monday;
        case TUESDAY -> tuesday;
        case WEDNESDAY -> wednesday;
        case THURSDAY -> thursday;
        case FRIDAY -> friday;
        case SATURDAY -> saturday;
        case SUNDAY -> sunday;
      };

    if (times == null) {
      //no restriction today
      return false;
    }

    int timeOfDay = zonedDateTime.toLocalTime().toSecondOfDay() - 12 * 3600;

    for (int[] range : times) {
      if (timeOfDay >= range[0] && timeOfDay <= range[1]) {
        return true;
      }
    }

    return false;
  }

  /**
   * Return seconds before or after noon for the given hour.
   */
  private static int parseHour(String hour) {
    String[] parsed = hour.split(":");
    int ret = Integer.parseInt(parsed[0]) * 3600;

    if (parsed.length >= 2) {
      ret += Integer.parseInt(parsed[1]) * 60;
    }

    // subtract 12 hours to make it noon-relative. This implicitly handles DST.
    ret -= 12 * 3600;

    return ret;
  }
}
