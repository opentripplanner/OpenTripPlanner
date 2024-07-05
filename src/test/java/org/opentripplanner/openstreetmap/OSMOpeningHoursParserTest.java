package org.opentripplanner.openstreetmap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class OSMOpeningHoursParserTest {

  static OpeningHoursCalendarService openingHoursCalendarService = new OpeningHoursCalendarService(
    new Deduplicator(),
    LocalDate.of(2022, Month.SEPTEMBER, 1),
    LocalDate.of(2023, Month.DECEMBER, 31)
  );

  static OSMOpeningHoursParser osmOpeningHoursParser = new OSMOpeningHoursParser(
    openingHoursCalendarService,
    ZoneIds.PARIS
  );

  static Stream<Arguments> osmOpeningHoursTestCases() {
    return Stream.of(
      Arguments.of(
        "Mo-Fr 14:00-19:00",
        List.of("2022-10-25T14:30:00Z"),
        List.of("2022-10-25T08:30:00Z", "2022-10-29T14:30:00Z")
      ),
      Arguments.of(
        "Tu 10:00-15:00",
        List.of("2022-10-25T10:30:00Z"),
        List.of("2022-10-26T00:30:00Z", "2022-10-27T10:30:00Z")
      ),
      Arguments.of(
        "Mo-Fr 08:00-17:00;Sa-Su 10:00-13:00",
        List.of("2022-10-25T07:30:00Z", "2022-10-29T10:30:00Z"),
        List.of("2022-10-25T03:30:00Z", "2022-10-29T15:30:00Z")
      ),
      // TODO implement support for public holidays, currently only the first two rules are handled
      Arguments.of(
        "Mo-Fr 08:00-17:00;Sa-Su 10:00-13:00; PH off",
        List.of("2022-10-25T07:30:00Z", "2022-10-29T10:30:00Z"),
        List.of("2022-10-25T03:30:00Z", "2022-10-29T15:30:00Z")
      ),
      // TODO implement support for public holidays, currently only the first two rules are handled
      Arguments.of(
        "Mo,We,Th,Fr,Su 00:00-24:00; Tu,Sa 00:00-02:00, 14:30-24:00; PH 00:00-24:00",
        List.of("2022-10-24T15:30:00Z", "2022-10-24T23:30:00Z", "2022-10-25T15:30:00Z"),
        List.of("2022-10-25T05:30:00Z")
      ),
      // The second rule overrides the first rule for Wednesdays, i.e. on Wednesdays it should only be
      // open from 10:00 to 13:00
      Arguments.of(
        "Mo-Fr 16:00-02:00; We 10:00-13:00",
        List.of("2022-10-25T23:30:00Z", "2022-10-26T10:30:00Z", "2022-10-27T23:30:00Z"),
        List.of("2022-10-25T10:30:00Z", "2022-10-26T23:30:00Z")
      ),
      // The second rule overrides the first rule for Tuesdays, i.e. it should be closed on Tuesdays
      Arguments.of(
        "Mo - Sa 10:00-18:00; Tu off",
        List.of("2022-10-24T15:30:00Z"),
        List.of("2022-10-24T05:30:00Z", "2022-10-25T10:30:00Z")
      ),
      // Even though the following rules are not additive, they should be handled as such because they
      // have the off modifier (https://github.com/opening-hours/opening_hours.js/issues/53).
      // Therefore, it should be open according to the first rule outside of the defined off periods,
      // so for example, on Tuesdays it should be open from 07:30 to 12:00 and from 16:00 to 22:00.
      // These different off cases are needed because they overlap slightly differently with the first
      // rule and have slightly different handling in code.
      Arguments.of(
        "Mo-Sa 07:30-22:00; Mo 05:00-23:00 off; Tu 12:00-16:00 off; We 06:00-16:00 off; Th 07:30-16:00 off, Fr 16:00-22:00 off, Sa 16:00-23:00 off",
        List.of(
          "2022-10-25T07:30:00Z",
          "2022-10-25T17:30:00Z",
          "2022-10-26T17:30:00Z",
          "2022-10-27T17:30:00Z",
          "2022-10-28T07:30:00Z",
          "2022-10-29T07:30:00Z"
        ),
        List.of(
          "2022-10-24T07:30:00Z",
          "2022-10-25T12:30:00Z",
          "2022-10-26T07:30:00Z",
          "2022-10-27T07:30:00Z",
          "2022-10-28T16:30:00Z",
          "2022-10-29T16:30:00Z",
          "2022-10-30T07:30:00Z"
        )
      ),
      // This tests that it's correctly closed outside with an off rule that extends over midnight
      Arguments.of(
        "Mo-Fr 12:30-04:00; We 18:00-02:00 off",
        List.of(
          "2022-10-25T19:30:00Z",
          "2022-10-25T23:30:00Z",
          "2022-10-26T13:30:00Z",
          "2022-10-27T01:30:00Z",
          "2022-10-27T23:30:00Z"
        ),
        List.of("2022-10-26T19:30:00Z", "2022-10-26T23:30:00Z", "2022-10-27T05:30:00Z")
      ),
      Arguments.of(
        "open; Tu 13:00-16:00 off",
        List.of("2022-10-24T12:30:00Z", "2022-10-25T07:30:00Z", "2022-10-24T18:30:00Z"),
        List.of("2022-10-25T13:30:00Z")
      ),
      Arguments.of(
        "Su-Tu 11:00-02:00, We-Th 11:00-03:00, Fr 11:00-06:00, Sa 11:00-07:00",
        List.of("2022-10-25T14:30:00Z", "2022-10-25T23:30:00Z", "2022-10-29T03:30:00Z"),
        List.of("2022-10-25T02:30:00Z", "2022-10-27T03:30:00Z")
      ),
      Arguments.of(
        "Sep-Oct: Mo-Sa 10:00-02:00",
        List.of("2022-09-30T23:30:00Z", "2022-10-29T10:30:00Z", "2022-10-31T23:30:00Z"),
        List.of("2022-10-30T10:30:00Z", "2022-11-26T10:30:00Z")
      ),
      Arguments.of(
        "Oct: Mo 10:00-02:00",
        List.of("2022-10-24T10:30:00Z", "2022-10-31T23:30:00Z"),
        List.of("2022-09-30T23:30:00Z", "2022-10-30T10:30:00Z", "2022-11-07T10:30:00Z")
      ),
      // TODO implement support for dates with and without year, this is now completely unparsed
      // which means that it's never open
      Arguments.of("Oct 23-Jan 3: 10:00-23:00", List.of(), List.of("2022-10-20T12:30:00Z")),
      // TODO implement support for nth weekday in a month and offsets
      Arguments.of(
        "Mar Su[-1]-Oct Su[1] -2 days: 22:00-23:00",
        List.of(),
        List.of("2022-11-20T12:30:00Z")
      ),
      Arguments.of("24/7", List.of("2022-10-25T07:30:00Z"), List.of()),
      Arguments.of("24/7 closed", List.of(), List.of("2022-10-25T07:30:00Z")),
      // TODO implement support for school holidays and special dates, only the first rule is now
      // handled
      Arguments.of(
        "Mo-Su,SH 15:00-03:00; easter -2 days off",
        List.of("2022-10-25T15:30:00Z"),
        List.of("2022-10-25T05:30:00Z")
      ),
      // TODO implement support for comment modifiers, this is now interpreted to be always closed
      Arguments.of("\"by appointment\"", List.of(), List.of("2022-10-25T07:30:00Z")),
      // TODO implement support for fallbacks if feasible, now the last rule is ignored and the first
      // rule is respected
      Arguments.of(
        "Mo-Sa 08:00-13:00,16:00-18:00 || \"by appointment\"",
        List.of("2022-10-25T07:30:00Z", "2022-10-25T15:30:00Z"),
        List.of("2022-10-25T13:30:00Z", "2022-10-30T07:30:00Z")
      ),
      // TODO implement support for weeks, these rules are now ignored and it's always closed
      Arguments.of(
        "week 1-53/2 Fr 09:00-12:00; week 2-52/2 We 09:00-12:00",
        List.of(),
        List.of("2022-10-28T07:30:00Z")
      ),
      // TODO implement support for events, this is now interpreted to be always closed
      Arguments.of("sunrise-sunset", List.of(), List.of("2022-10-25T07:30:00Z")),
      // This is interpreted to be open on sundays from 10:00 until 23:59
      Arguments.of(
        "Su 10:00+",
        List.of("2022-10-30T10:30:00Z"),
        List.of("2022-10-25T10:30:00Z", "2022-10-30T06:30:00Z")
      )
    );
  }

  @ParameterizedTest(name = "{0} should be open on {1} but not on {2}")
  @MethodSource("osmOpeningHoursTestCases")
  void testOSMOpeningHoursParsing(
    String openingHours,
    List<String> openTimes,
    List<String> closedTimes
  ) {
    try {
      var ohCalendar = osmOpeningHoursParser.parseOpeningHours(openingHours, "test", null);
      openTimes.forEach(openTime -> {
        Instant openDateTime = Instant.parse(openTime);
        assertTrue(ohCalendar.isOpen(openDateTime.getEpochSecond()));
      });
      closedTimes.forEach(closedTime -> {
        Instant closedDateTime = Instant.parse(closedTime);
        assertFalse(ohCalendar.isOpen(closedDateTime.getEpochSecond()));
      });
    } catch (OpeningHoursParseException e) {
      fail(e);
    }
  }
}
