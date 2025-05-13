package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

class EmissionAggregatorTest {

  private static final String FEED_ID = "E";

  private static final StopLocation STOP_A;
  private static final StopLocation STOP_B;
  private static final StopLocation STOP_C;
  private static final StopLocation STOP_D;

  private static final String STOP_A_ID;
  private static final String STOP_B_ID;
  private static final String STOP_C_ID;

  static {
    var builder = TimetableRepositoryForTest.of();
    STOP_A = builder.stop("A").build();
    STOP_B = builder.stop("B").build();
    STOP_C = builder.stop("C").build();
    STOP_D = builder.stop("D").build();

    STOP_A_ID = STOP_A.getId().getId();
    STOP_B_ID = STOP_B.getId().getId();
    STOP_C_ID = STOP_C.getId().getId();
  }

  private static final String TRIP_ID = "T:1";
  private static final FeedScopedId FEED_SCOPED_TRIP_ID = new FeedScopedId(FEED_ID, TRIP_ID);

  private EmissionAggregator subject = new EmissionAggregator(
    FEED_SCOPED_TRIP_ID,
    List.of(STOP_A, STOP_B, STOP_C, STOP_D)
  );

  @Test
  void mergeAFewRowsOk() {
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(3.0)));
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_B_ID, 2, Gram.of(7.0)));
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_C_ID, 3, Gram.of(10.0)));

    var emission = subject.build();

    assertTrue(emission.isPresent(), () -> subject.listIssues().toString());
    assertEquals(List.of(), subject.listIssues());
    assertEquals(
      "TripPatternEmission{emissions: [Emission{CO₂: 3g}, Emission{CO₂: 7g}, Emission{CO₂: 10g}]}",
      emission.get().toString()
    );
  }

  @Test
  void mergeWithMissingHops() {
    // Add same row twice, but no row for 2nd and 3rd hop
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(2.5)));
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(3.5)));

    var emission = subject.build();

    assertTrue(emission.isPresent());
    assertEquals(2, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionMissingTripHop(Warning! All hops in a trip (E:T:1) should have an emission value. " +
      "Hop 2 and 3 does not have an emission value.)",
      subject.listIssues().get(0).toString()
    );
    assertEquals(
      "EmissionTripHopDuplicates(Warning! The emission import contains duplicate rows for " +
      "the same hop for trip (E:T:1). An average value is used.)",
      subject.listIssues().get(1).toString()
    );
    assertEquals(
      "TripPatternEmission{emissions: [Emission{CO₂: 3g}, Emission{}, Emission{}]}",
      emission.get().toString()
    );
  }

  @Test
  void mergeWithStopIdMismatch() {
    // Stop B and C are switched
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(3.0)));
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_C_ID, 2, Gram.of(7.0)));
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_B_ID, 3, Gram.of(10.0)));

    var emission = subject.build();

    assertTrue(emission.isEmpty());
    assertEquals(2, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionStopIdMismatch(Emission 'from_stop_id' (C) not found in stop pattern for trip " +
      "(E:T:1): TripHopsRow[tripId=T:1, fromStopId=C, fromStopSequence=2, co2=7g])",
      subject.listIssues().get(0).toString()
    );
    assertEquals(
      "EmissionStopIdMismatch(Emission 'from_stop_id' (B) not found in stop pattern for trip " +
      "(E:T:1): TripHopsRow[tripId=T:1, fromStopId=B, fromStopSequence=3, co2=10g])",
      subject.listIssues().get(1).toString()
    );
  }

  @Test
  void mergeWithStopIndexOutOfBound() {
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_A_ID, 0, Gram.of(3.0)));
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_C_ID, 4, Gram.of(3.0)));

    var emission = subject.build();

    assertTrue(emission.isEmpty());
    assertEquals(2, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionStopSeqNr(The emission 'from_stop_sequence' (0) is out of bounds [1, 3]: " +
      "TripHopsRow[tripId=T:1, fromStopId=A, fromStopSequence=0, co2=3g])",
      subject.listIssues().get(0).toString()
    );
    assertEquals(
      "EmissionStopSeqNr(The emission 'from_stop_sequence' (4) is out of bounds [1, 3]: " +
      "TripHopsRow[tripId=T:1, fromStopId=C, fromStopSequence=4, co2=3g])",
      subject.listIssues().get(1).toString()
    );
  }

  @Test
  void mergeWithMissingStops() {
    subject = new EmissionAggregator(FEED_SCOPED_TRIP_ID, null);

    // Make sure mapping does not fail
    subject.mergeEmissionsForHop(new TripHopsRow(TRIP_ID, STOP_A_ID, -1, Gram.of(3.0)));

    var emission = subject.build();
    assertTrue(emission.isEmpty());
    assertEquals(1, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionMissingTripStopPattern(No trip with stop pattern found for trip (E:T:1). " +
      "Trip or stop-pattern is missing. The trip is skipped.)",
      subject.listIssues().get(0).toString()
    );
  }
}
