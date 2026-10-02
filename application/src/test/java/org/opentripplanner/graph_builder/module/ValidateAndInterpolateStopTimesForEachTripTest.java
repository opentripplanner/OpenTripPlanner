package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.FlexStopTimesFactory;
import org.opentripplanner.model.TripStopTimes;

class ValidateAndInterpolateStopTimesForEachTripTest {

  @Test
  void flexTripWithWindowedStopsDoesNotThrowAndPreservesStopTimes() {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimesByTrip = new TripStopTimes();

    var st0 = FlexStopTimesFactory.regularStop("10:00");
    st0.setStopSequence(0);
    var st1 = FlexStopTimesFactory.regularStopWithWindow("10:05", "10:30");
    st1.setStopSequence(1);
    var st2 = FlexStopTimesFactory.regularStopWithWindow("10:05", "10:30");
    st2.setStopSequence(2);

    stopTimesByTrip.addAll(List.of(st0, st1, st2));

    var validator = new ValidateAndInterpolateStopTimesForEachTrip(
      stopTimesByTrip,
      true,
      issueStore
    );

    validator.run();

    var trip = st0.getTrip();
    var result = stopTimesByTrip.get(trip);
    assertEquals(3, result.size());
    assertTrue(issueStore.listIssues().isEmpty());
  }

  @Test
  void missingFinalScheduledStopTimeReportsIssueAndDropsTripGracefully() {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimesByTrip = new TripStopTimes();

    var st0 = FlexStopTimesFactory.regularStop("10:00");
    st0.setStopSequence(0);
    var st1 = FlexStopTimesFactory.regularStop("10:00");
    st1.setStopSequence(1);
    st1.setArrivalTime(org.opentripplanner.model.StopTime.MISSING_VALUE);
    st1.setDepartureTime(org.opentripplanner.model.StopTime.MISSING_VALUE);
    var st2 = FlexStopTimesFactory.regularStop("10:00");
    st2.setStopSequence(2);
    st2.setArrivalTime(org.opentripplanner.model.StopTime.MISSING_VALUE);
    st2.setDepartureTime(org.opentripplanner.model.StopTime.MISSING_VALUE);

    stopTimesByTrip.addAll(List.of(st0, st1, st2));

    var validator = new ValidateAndInterpolateStopTimesForEachTrip(
      stopTimesByTrip,
      true,
      issueStore
    );

    // Previously this would throw an unhandled RuntimeException killing graph build
    validator.run();

    var trip = st0.getTrip();
    var result = stopTimesByTrip.get(trip);
    assertTrue(result.isEmpty());
    assertFalse(issueStore.listIssues().isEmpty());
    assertTrue(
      issueStore
        .listIssues()
        .stream()
        .anyMatch(issue -> "TripInterpolationFailed".equals(issue.getType()))
    );
  }
}
