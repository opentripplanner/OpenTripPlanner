package org.opentripplanner.framework.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frank Purcell (p u r c e l l f @ t r i m e t . o r g) October 20, 2009
 */
public class DateUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DateUtils.class);

  private static final int SANITY_CHECK_CUTOFF_YEAR = 1000;

  // NOTE: don't change the order of these strings...the simplest should be on the
  // bottom...you risk parsing the wrong thing (and ending up with year 0012)
  private static final List<DateTimeFormatter> DF_LIST = List.of(
    DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss"),
    DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm"),
    DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss.SS"),
    DateTimeFormatter.ofPattern("M.d.yy h.mm a"),
    DateTimeFormatter.ofPattern("M.d.yyyy h.mm a"),
    DateTimeFormatter.ofPattern("M.d.yyyy h.mma"),
    DateTimeFormatter.ofPattern("M.d.yyyy h.mm"),
    DateTimeFormatter.ofPattern("M.d.yyyy k.mm"),
    DateTimeFormatter.ofPattern("M.d.yyyy"),
    DateTimeFormatter.ofPattern("yyyy.M.d"),
    DateTimeFormatter.ofPattern("h.mm a")
    // NOTE: don't change the order of these strings...the simplest should be on the
    // bottom...you risk parsing the wrong thing (and ending up with year 0012)
  );

  private static final List<DateTimeFormatter> SMALL_DF_LIST = List.of(
    DateTimeFormatter.ofPattern("M.d.yy"),
    DateTimeFormatter.ofPattern("yy.M.d"),
    DateTimeFormatter.ofPattern("h.mm a")
  );

  /**
   * Returns a Date object based on input date and time parameters Defaults to today / now (when
   * date / time are null)
   */
  public static ZonedDateTime toZonedDateTime(String date, String time, ZoneId tz) {
    //LOG.debug("JVM default timezone is {}", TimeZone.getDefault());
    LOG.debug("Parsing date {} and time {}", date, time);
    LOG.debug("using timezone {}", tz);
    ZonedDateTime retVal = ZonedDateTime.ofInstant(Instant.now(), tz);
    if (date != null) {
      LocalDate localDate = parseDate(date);
      if (localDate == null) {
        return null; //unparseable date
      }
      boolean timed = false;
      if (time != null) {
        LocalTime localTime = parseTime(time);
        if (localTime != null) {
          retVal = LocalDateTime.of(localDate, localTime).atZone(tz);
          timed = true;
        }
      }
      if (!timed) {
        //assume t = now
        retVal = LocalDateTime.of(localDate, retVal.toLocalTime()).atZone(tz);
      }
    } else if (time != null) {
      LocalTime localTime = parseTime(time);
      if (localTime != null) {
        retVal = LocalDateTime.of(retVal.toLocalDate(), localTime).atZone(tz);
      }
    }
    LOG.debug("resulting date is {}", retVal);
    return retVal;
  }

  // TODO: could be replaced with Apache's DateFormat.parseDate ???
  public static LocalDate parseDate(@Nonnull String input) {
    LocalDate retVal = null;
    try {
      String newString = input
        .trim()
        .replace('_', '.')
        .replace('-', '.')
        .replace(':', '.')
        .replace('/', '.');
      List<DateTimeFormatter> dateTimeFormatterList = DF_LIST;

      if (newString.length() <= 8) {
        if (newString.matches("\\d\\d\\d\\d\\d\\d\\d\\d")) {
          // Accept dates without punctuation if they consist of exactly eight digits.
          newString =
            newString.substring(0, 4) +
            '.' +
            newString.substring(4, 6) +
            '.' +
            newString.substring(6, 8);
        } else if (!(newString.matches(".*20\\d\\d.*"))) {
          // if it looks like we have a small date format, ala 11.4.09, then use
          // another set of compares
          dateTimeFormatterList = SMALL_DF_LIST;
        }
      }

      for (DateTimeFormatter dateTimeFormatter : dateTimeFormatterList) {
        try {
          retVal = LocalDate.parse(newString, dateTimeFormatter);
          if (retVal != null) {
            int year = retVal.getYear();
            if (year >= SANITY_CHECK_CUTOFF_YEAR) {
              break;
            }
          }
        } catch (DateTimeParseException ex) {}
      }
    } catch (Exception ex) {
      throw new RuntimeException("Could not parse " + input);
    }

    return retVal;
  }

  public static int getIntegerFromString(String input) {
    try {
      return Integer.parseInt(input);
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Converts the given time in seconds to a <code>String</code> in the format h:mm.
   *
   * @param seconds the time in seconds.
   * @return a <code>String</code> representing the time in the format h:mm
   */
  public static String secToHHMM(int seconds) {
    int min;
    String sign = "";

    if (seconds >= 0) {
      min = seconds / 60;
      sign = "";
    } else {
      min = -seconds / 60;
      sign = "-";
    }

    int mm = min % 60;
    int hh = min / 60;

    return String.format("%s%d:%02d", sign, hh, mm);
  }

  public static String trim(String str) {
    String retVal = str;
    try {
      retVal = str.trim();
      retVal = retVal.replaceAll("%20;", "");
      retVal = retVal.replaceAll("%20", "");
    } catch (Exception ex) {}
    return retVal;
  }

  public static long absoluteTimeout(Duration timeout) {
    if (timeout == null) {
      return Long.MAX_VALUE;
    } else {
      return System.currentTimeMillis() + timeout.toMillis();
    }
  }

  private static LocalTime parseTime(String time) {
    boolean amPm = false;
    int addHours = 0;
    int hour = 0, min = 0, sec = 0;
    try {
      String[] hms = time.toUpperCase().split(":");

      // if we don't have a colon sep string, assume string is int and represents seconds past
      // midnight
      if (hms.length < 2) {
        return LocalTime.ofSecondOfDay(getIntegerFromString(time));
      }

      if (hms[1].endsWith("PM") || hms[1].endsWith("AM")) {
        amPm = true;

        if (hms[1].contains("PM")) addHours = 12;

        int suffex = hms[1].lastIndexOf(' ');
        if (suffex < 1) {
          suffex = hms[1].lastIndexOf("AM");
          if (suffex < 1) {
            suffex = hms[1].lastIndexOf("PM");
          }
        }
        hms[1] = hms[1].substring(0, suffex);
      }

      int h = Integer.parseInt(trim(hms[0]));
      if (amPm && h == 12) h = 0;
      hour = h + addHours;

      min = Integer.parseInt(trim(hms[1]));
      if (hms.length > 2) {
        sec = Integer.parseInt(trim(hms[2]));
      }

      return LocalTime.of(hour, min, sec);
    } catch (Exception ignore) {
      LOG.info("Time '{}' didn't parse", time);
      return null;
    }
  }
}
