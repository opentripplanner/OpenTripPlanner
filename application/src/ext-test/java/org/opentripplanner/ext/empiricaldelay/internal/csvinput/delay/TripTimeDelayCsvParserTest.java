package org.opentripplanner.ext.empiricaldelay.internal.csvinput.delay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csvreader.CsvReader;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TripTimeDelayCsvParserTest {

  private static final String DEFAUT_INPUT =
    """
    empirical_delay_service_id, trip_id, stop_id, stop_sequence, p50, p90
    WEEKDAYS, VYG:ServiceJourney:BUS-2280_374435-R, NSR:Quay:1, 1, 2, 8
    WEEKEND, RUT:ServiceJourney:bc0092f0102605437e3e49cc3a88d0ba, NSR:Quay:10001, 39, 0, 200
    """;
  private static final String FEED_ID = "F";

  private final TripTimeDelayCsvParser subject = new TripTimeDelayCsvParser(
    DataImportIssueStore.NOOP,
    CsvReader.parse(DEFAUT_INPUT),
    FEED_ID
  );

  @Test
  void headersMatch() {
    var subject = new TripTimeDelayCsvParser(
      DataImportIssueStore.NOOP,
      CsvReader.parse(
        """
        No, matching, header
        """
      ),
      FEED_ID
    );
    assertFalse(subject.headersMatch());
  }

  @Test
  void createNextRow() {
    System.out.println(subject.headers());

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertEquals(
      new TripTimeDelayRow(
        "WEEKDAYS",
        new FeedScopedId(FEED_ID, "VYG:ServiceJourney:BUS-2280_374435-R"),
        new FeedScopedId(FEED_ID, "NSR:Quay:1"),
        1,
        2,
        8
      ),
      subject.next()
    );
    assertTrue(subject.hasNext());
    assertEquals(
      new TripTimeDelayRow(
        "WEEKEND",
        new FeedScopedId(FEED_ID, "RUT:ServiceJourney:bc0092f0102605437e3e49cc3a88d0ba"),
        new FeedScopedId(FEED_ID, "NSR:Quay:10001"),
        39,
        0,
        200
      ),
      subject.next()
    );
  }
}
