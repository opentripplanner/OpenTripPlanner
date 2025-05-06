package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

class TripLegMapperTest {

  private static final String FEED_ID = "E";
  private static final String STOP_ID_A = "A";
  private static final String STOP_ID_B = "B";
  private static final String STOP_ID_C = "C";
  private static final String STOP_ID_D = "D";
  private static final StopLocation STOP_A;
  private static final StopLocation STOP_B;
  private static final StopLocation STOP_C;
  private static final StopLocation STOP_D;
  private static final String TRIP_ID_1 = "T:1";
  private static final String TRIP_ID_2 = "T:2";
  private static final FeedScopedId SCOPED_TRIP_ID_1 = new FeedScopedId(FEED_ID, TRIP_ID_1);
  private static final FeedScopedId SCOPED_TRIP_ID_2 = new FeedScopedId(FEED_ID, TRIP_ID_2);
  private static final Gram CO2_AB = Gram.of(2.0);
  private static final Gram CO2_BC = Gram.of(3.0);
  private static final Gram CO2_CD = Gram.of(4.0);
  private static final Gram CO2_AD = Gram.of(5.0);

  static {
    var builder = TimetableRepositoryForTest.of();
    STOP_A = builder.stop(STOP_ID_A).build();
    STOP_B = builder.stop(STOP_ID_B).build();
    STOP_C = builder.stop(STOP_ID_C).build();
    STOP_D = builder.stop(STOP_ID_D).build();
  }

  private final DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
  private final Map<FeedScopedId, List<StopLocation>> stopsByTripId = Map.ofEntries(
    Map.entry(SCOPED_TRIP_ID_1, List.of(STOP_A, STOP_B, STOP_C, STOP_D)),
    Map.entry(SCOPED_TRIP_ID_2, List.of(STOP_A, STOP_D))
  );

  private final TripLegMapper subject = new TripLegMapper(stopsByTripId, issueStore);

  @Test
  void testCaseOk() {
    subject.setCurrentFeedId(FEED_ID);
    var result = subject.map(
      List.of(
        new TripLegsRow(TRIP_ID_1, STOP_ID_A, 1, CO2_AB),
        new TripLegsRow(TRIP_ID_1, STOP_ID_B, 2, CO2_BC),
        new TripLegsRow(TRIP_ID_1, STOP_ID_C, 3, CO2_CD),
        new TripLegsRow(TRIP_ID_2, STOP_ID_A, 1, CO2_AD)
      )
    );

    assertEquals(2, result.size(), () -> result.toString());
    assertEquals(
      "TripPatternEmission{emissions: [Emission{CO₂: 2.0g}, Emission{CO₂: 3.0g}, Emission{CO₂: 4.0g}]}",
      result.get(SCOPED_TRIP_ID_1).toString()
    );
    assertEquals(
      "TripPatternEmission{emissions: [Emission{CO₂: 5.0g}]}",
      result.get(SCOPED_TRIP_ID_2).toString()
    );
  }

  @Test
  void testCaseError() {
    subject.setCurrentFeedId(FEED_ID);
    var result = subject.map(List.of(new TripLegsRow(TRIP_ID_2, STOP_ID_A, 2, CO2_AB)));
    assertTrue(result.isEmpty());
    assertEquals(
      "Emission 'from_stop_id'(A) not found in stop pattern for trip(E:T:2): " +
      "TripLegsRow[tripId=T:2, fromStopId=A, fromStopSequence=2, co2=2.0g]",
      issueStore.listIssues().getFirst().getMessage()
    );
  }

  @Test
  void currentFeedIdNotSet() {
    var ex = assertThrows(IllegalStateException.class, () -> subject.map(List.of()));
    assertEquals("currentFeedId is not set", ex.getMessage());
  }
}
