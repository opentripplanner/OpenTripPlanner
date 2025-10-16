package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.EmissionTestData;
import org.opentripplanner.framework.csv.HeadersDoNotMatch;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;

class TripDataReaderTest implements EmissionTestData {

  private final DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();

  @Test
  void testCo2EmissionsFromGtfsDataSource() throws FileNotFoundException, HeadersDoNotMatch {
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
  void handleMissingDataSource() throws HeadersDoNotMatch {
    var subject = new TripDataReader(emissionMissingFile(), issueStore);
    var ex = assertThrows(IllegalStateException.class, () -> subject.read(null));
    assertEquals("DataSource is missing: file-does-not-exist.txt", ex.getMessage());
  }

  @Test
  void ignoreDataSourceIfHeadersDoNotMatch() {
    var subject = new TripDataReader(emissionOnRoutes(), issueStore);

    var ex = assertThrows(HeadersDoNotMatch.class, () -> subject.read(null));
    assertEquals(
      "The header does not match the expected values. File: em-on-routes.txt\n" +
      "\twas: [route_id,avg_co2_per_vehicle_per_km,avg_passenger_count]\n" +
      "\texpected: [trip_id, from_stop_id, from_stop_sequence, co2]",
      ex.getMessage()
    );
  }
}
