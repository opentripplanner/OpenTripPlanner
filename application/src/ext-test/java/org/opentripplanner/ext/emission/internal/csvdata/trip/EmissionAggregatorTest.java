package org.opentripplanner.ext.emission.internal.csvdata.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(3.0)));
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_B_ID, 2, Gram.of(7.0)));
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_C_ID, 3, Gram.of(10.0)));

    assertTrue(subject.validate());
    assertEquals(List.of(), subject.listIssues());

    assertEquals(
      "TripPatternEmission{emissions: [Emission{CO₂: 3g}, Emission{CO₂: 7g}, Emission{CO₂: 10g}]}",
      subject.build().toString()
    );
  }

  @Test
  void mergeWithMissingLegs() {
    // Add same row twice, but no row for 2nd and 3rd leg
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(2.5)));
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(3.5)));

    assertFalse(subject.validate());
    assertEquals(2, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionMissingLeg(All legs in a trip(E:T:1) must have an emission value. " +
      "Leg number 2 and 3 does not have an emission value.)",
      subject.listIssues().get(0).toString()
    );
    assertEquals(
      "EmissionTripLegDuplicates(Warn! The emission import contains duplicate rows for the same " +
      "leg for trip(E:T:1). A average value is used.)",
      subject.listIssues().get(1).toString()
    );
    var ex = assertThrows(IllegalStateException.class, () -> subject.build());
    assertEquals("Can not build when there are issues!", ex.getMessage());
  }

  @Test
  void mergeWithStopIdMissmatch() {
    // Stop B and C are switched
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(3.0)));
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_C_ID, 2, Gram.of(7.0)));
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_B_ID, 3, Gram.of(10.0)));

    assertFalse(subject.validate());
    assertEquals(2, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionStopIdMissmatch(Emission 'from_stop_id'(C) not found in stop pattern for trip(E:T:1): " +
      "TripLegsRow[tripId=T:1, fromStopId=C, fromStopSequence=2, co2=7g])",
      subject.listIssues().get(0).toString()
    );
    assertEquals(
      "EmissionStopIdMissmatch(Emission 'from_stop_id'(B) not found in stop pattern for trip(E:T:1): " +
      "TripLegsRow[tripId=T:1, fromStopId=B, fromStopSequence=3, co2=10g])",
      subject.listIssues().get(1).toString()
    );
    var ex = assertThrows(IllegalStateException.class, () -> subject.build());
    assertEquals("Can not build when there are issues!", ex.getMessage());
  }

  @Test
  void mergeWithStopIndexOutOfBound() {
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, -1, Gram.of(3.0)));
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_C_ID, 4, Gram.of(3.0)));

    assertFalse(subject.validate());
    assertEquals(2, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionStopSeqNr(The emission 'from_stop_sequence'(-1) is out of bounds[1, 3]: " +
      "TripLegsRow[tripId=T:1, fromStopId=A, fromStopSequence=-1, co2=3g])",
      subject.listIssues().get(0).toString()
    );
    assertEquals(
      "EmissionStopIdMissmatch(Emission 'from_stop_id'(C) not found in stop pattern for trip(E:T:1): " +
      "TripLegsRow[tripId=T:1, fromStopId=C, fromStopSequence=4, co2=3g])",
      subject.listIssues().get(1).toString()
    );
  }

  @Test
  void mergeWithMissingStops() {
    subject = new EmissionAggregator(FEED_SCOPED_TRIP_ID, null);

    // Make sure mapping does not fail
    subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, -1, Gram.of(3.0)));

    assertFalse(subject.validate());
    assertEquals(1, subject.listIssues().size(), () -> subject.listIssues().toString());
    assertEquals(
      "EmissionTripLegMissingTripStopPattern(Warn! No trip with a stop pattern found for " +
      "trip(E:T:1). The trip is skipped.)",
      subject.listIssues().get(0).toString()
    );
  }

  @Test
  void mergeWithoutCallingValidationMethod() {
    var ex = assertThrows(IllegalStateException.class, () -> subject.build());
    assertEquals("Forgot to call validate()?", ex.getMessage());
  }

  @Test
  void addRowsAfterValidationIsCalled() {
    subject.validate();
    var ex = assertThrows(IllegalStateException.class, () ->
      subject.mergeEmissionForleg(new TripLegsRow(TRIP_ID, STOP_A_ID, 1, Gram.of(2.5)))
    );
    assertEquals("Rows can not be added after validate() is called.", ex.getMessage());
  }
}
