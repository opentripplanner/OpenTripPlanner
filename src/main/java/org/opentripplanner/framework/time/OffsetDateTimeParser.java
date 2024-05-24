package org.opentripplanner.framework.time;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public class OffsetDateTimeParser {

  /**
   * We need to have two offsets, in order to parse both "+0200" and "+02:00". The first is not
   * really ISO-8601 compatible with the extended date and time. We need to make parsing strict, in
   * order to keep the minute mandatory, otherwise we would be left with an unparsed minute
   */
  public static final DateTimeFormatter LENIENT_PARSER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .parseLenient()
    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    .optionalStart()
    .parseStrict()
    .appendOffset("+HH:MM:ss", "Z")
    .parseLenient()
    .optionalEnd()
    .optionalStart()
    .appendOffset("+HHmmss", "Z")
    .optionalEnd()
    .toFormatter();

  /**
   * Parses a ISO-8601 string into am OffsetDateTime instance allowing the offset to be both in
   * '02:00' and '0200' format.
   * @throws ParseException if the string cannot be parsed
   */
  public static OffsetDateTime parseLeniently(CharSequence input) throws ParseException {
    try {
      return OffsetDateTime.parse(input, LENIENT_PARSER);
    } catch (DateTimeParseException e) {
      throw new ParseException(e.getParsedString(), e.getErrorIndex());
    }
  }
}
