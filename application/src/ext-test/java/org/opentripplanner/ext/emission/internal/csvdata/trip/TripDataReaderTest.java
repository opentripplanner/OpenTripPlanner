package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    var subject = new TripDataReader(emissionOnTripLegs(), issueStore);

    var emissions = subject.read();

    assertEquals(
      "TripLegsRow[tripId=T1, fromStopId=A, fromStopSequence=1, co2=5g]",
      emissions.getFirst().toString()
    );
    assertEquals(
      "TripLegsRow[tripId=T2, fromStopId=B, fromStopSequence=2, co2=17g]",
      emissions.getLast().toString()
    );
    assertEquals(4, emissions.size());

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
  void handleMissingDdataSource() {
    var subject = new TripDataReader(emissionMissingFile(), issueStore);

    var emissions = subject.read();
    assertTrue(emissions.isEmpty());
  }

  @Test
  void ignoreDataSourceIfHeadersDoesNotMatch() {
    var subject = new TripDataReader(emissionOnRoutes(), issueStore);

    var emissions = subject.read();

    assertTrue(emissions.isEmpty());
  }
}
