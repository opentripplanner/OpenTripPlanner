package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.EmissionTestData;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;

class TripDataReaderTest implements EmissionTestData {

  private final DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();

  @Test
  void testCo2EmissionsFromGtfsDataSource() throws FileNotFoundException {
    var subject = new TripDataReader(emissionOnTripHops(), issueStore);

    var emissions = subject.read(null);

    assertEquals(
      "TripHopsRow[tripId=T1, fromStopId=A, fromStopSequence=1, co2=5g]",
      emissions.getFirst().toString()
    );
    assertEquals(
      "TripHopsRow[tripId=T2, fromStopId=B, fromStopSequence=2, co2=17g]",
      emissions.getLast().toString()
    );
    assertTrue(subject.isDataProcessed());
    assertEquals(4, emissions.size());

    var issues = issueStore.listIssues();

    var expected = List.of(
      "The int value '-1' for from_stop_sequence is outside expected range [0, 10000]: 'E1,A,-1,xyz,25.0' (@line:6)",
      "The double value '-1000001.0' for co2 is outside expected range [-1000000.0, 1.0E9): 'E2,B,1,xyz,-1000001' (@line:7)",
      "The double value '1.000000001E9' for co2 is outside expected range [-1000000.0, 1.0E9): 'E3,B,1,xyz,1000000001' (@line:8)"
    );
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), issues.get(i).getMessage());
    }
    assertEquals(expected.size(), issues.size(), () -> issues.toString());
  }

  @Test
  void handleMissingDataSource() {
    var subject = new TripDataReader(emissionMissingFile(), issueStore);

    var emissions = subject.read(null);
    assertFalse(subject.isDataProcessed());
    assertTrue(emissions.isEmpty());
  }

  @Test
  void ignoreDataSourceIfHeadersDoNotMatch() {
    var subject = new TripDataReader(emissionOnRoutes(), issueStore);

    var emissions = subject.read(null);

    assertFalse(subject.isDataProcessed());
    assertTrue(emissions.isEmpty());
  }
}
