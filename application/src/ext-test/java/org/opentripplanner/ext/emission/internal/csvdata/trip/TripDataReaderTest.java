package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.EmissionTestData;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.utils.lang.IntBox;

class TripDataReaderTest implements EmissionTestData {

  private final DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();

  @Test
  void testCo2EmissionsFromGtfsDataSource() throws FileNotFoundException {
    var stepCounter = new IntBox(0);
    var subject = new TripDataReader(emissionOnTripHops(), issueStore);

    var emissions = subject.read(stepCounter::inc);

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
    assertEquals(4, stepCounter.get());

    var issues = issueStore.listIssues();

    var expected = List.of(
      "The int value '-1' for from_stop_sequence is outside expected range [0, 1000]: 'E1,A,-1,xyz,25.0' (@line:6)",
      "The double value '-0.01' for co2 is outside expected range [0.0, 1.0E9): 'E2,B,1,xyz,-0.01' (@line:7)"
    );
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), issues.get(i).getMessage());
    }
    assertEquals(expected.size(), issues.size());
  }

  @Test
  void handleMissingDataSource() {
    var stepCounter = new IntBox(0);
    var subject = new TripDataReader(emissionMissingFile(), issueStore);

    var emissions = subject.read(stepCounter::inc);
    assertFalse(subject.isDataProcessed());
    assertTrue(emissions.isEmpty());
    assertEquals(0, stepCounter.get());
  }

  @Test
  void ignoreDataSourceIfHeadersDoNotMatch() {
    var stepCounter = new IntBox(0);
    var subject = new TripDataReader(emissionOnRoutes(), issueStore);

    var emissions = subject.read(stepCounter::inc);

    assertFalse(subject.isDataProcessed());
    assertTrue(emissions.isEmpty());
    assertEquals(0, stepCounter.get());
  }
}
