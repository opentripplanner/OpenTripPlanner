package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csvreader.CsvReader;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

class TripHopsCsvParserTest {

  private static final String DATA =
    """
    trip_id, from_stop_id, from_stop_sequence, not_used, co2
    F:1, NSR:Quay:1, 1, xyz, 28.0
    F:1, NSR:Quay:2, 2, abc, 38.0
    """;

  @Test
  void test() {
    var subject = new TripHopsCsvParser(DataImportIssueStore.NOOP, CsvReader.parse(DATA));
    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertEquals(
      "TripHopsRow[tripId=F:1, fromStopId=NSR:Quay:1, fromStopSequence=1, co2=28g]",
      subject.next().toString()
    );
    assertTrue(subject.hasNext());
    assertEquals(
      "TripHopsRow[tripId=F:1, fromStopId=NSR:Quay:2, fromStopSequence=2, co2=38g]",
      subject.next().toString()
    );
    assertFalse(subject.hasNext());
  }
}
