package org.opentripplanner.integration.benchmark;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateLibrary {

  // Something very close to ISO 8601 time format
  private static final SimpleDateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ssZ");

  public static String getTimeAsIso8601String(Date date) {
    String timeString = DateLibrary._format.format(date);
    return timeString.substring(0, timeString.length() - 2) + ":"
        + timeString.substring(timeString.length() - 2);
  }

  public static Date getIso8601StringAsDate(String value)
      throws java.text.ParseException {
    int index = value.lastIndexOf(':');
    if (index == value.length() - 3)
      value = value.substring(0, index) + value.substring(index + 1);
    return DateLibrary._format.parse(value);
  }

}
