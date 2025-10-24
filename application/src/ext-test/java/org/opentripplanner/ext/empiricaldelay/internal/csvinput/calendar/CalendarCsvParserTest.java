package org.opentripplanner.ext.empiricaldelay.internal.csvinput.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csvreader.CsvReader;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

class CalendarCsvParserTest {

  private static final String DEFAUT_INPUT =
    String.join(", ", CalendarCsvParser.HEADERS) +
    "\n" +
    """
    WEEKDAYS,1,1,1,1,1,0,0,2025-01-01, 2025-12-31
    WEEKEND,0,0,0,0,0,1,1,2025-02-03, 2025-11-20
    """;
  private final CalendarCsvParser subject = new CalendarCsvParser(
    DataImportIssueStore.NOOP,
    CsvReader.parse(DEFAUT_INPUT)
  );

  @Test
  void headersMatch() {
    var subject = new CalendarCsvParser(
      DataImportIssueStore.NOOP,
      CsvReader.parse(
        """
        No, matching, header
        """
      )
    );
    assertFalse(subject.headersMatch());
  }

  @Test
  void createNextRow() {
    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertEquals(
      new CalendarRow(
        "WEEKDAYS",
        true,
        true,
        true,
        true,
        true,
        false,
        false,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31)
      ),
      subject.next()
    );
    assertTrue(subject.hasNext());
    assertEquals(
      new CalendarRow(
        "WEEKEND",
        false,
        false,
        false,
        false,
        false,
        true,
        true,
        LocalDate.of(2025, 2, 3),
        LocalDate.of(2025, 11, 20)
      ),
      subject.next()
    );
    assertFalse(subject.hasNext());
  }
}
