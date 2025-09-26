package org.opentripplanner.ext.empiricaldelay.internal.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.empiricaldelay.internal.model.TripDelaysDto;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TripDelaysMapperTest {

  private static final String FEED_ID = "F";
  private static final String WEEKEND = "Weekend";
  private static final String MON_FRI = "Mon-Fri";
  private static final FeedScopedId TRIP_ID_A = new FeedScopedId(FEED_ID, "Trip-A");
  private static final FeedScopedId TRIP_ID_OTHER = new FeedScopedId(FEED_ID, "Trip-OTHER");
  private static final FeedScopedId STOP_ID_A = new FeedScopedId(FEED_ID, "Stop-A");
  private static final FeedScopedId STOP_ID_B = new FeedScopedId(FEED_ID, "Stop-B");
  private static final FeedScopedId STOP_ID_C = new FeedScopedId(FEED_ID, "Stop-C");
  private static final FeedScopedId STOP_ID_D = new FeedScopedId(FEED_ID, "Stop-D");
  private static final FeedScopedId STOP_ID_E = new FeedScopedId(FEED_ID, "Stop-E");
  private static final Map<FeedScopedId, List<FeedScopedId>> STOP_PATTERNS = Map.of(
    TRIP_ID_A,
    List.of(STOP_ID_A, STOP_ID_B, STOP_ID_C)
  );
  private static final EmpiricalDelay EMPIRICAL_DELAY_A = new EmpiricalDelay(19, 73);
  private static final EmpiricalDelay EMPIRICAL_DELAY_B = new EmpiricalDelay(15, 63);
  private static final EmpiricalDelay EMPIRICAL_DELAY_C = new EmpiricalDelay(12, 53);
  private static final EmpiricalDelay EMPIRICAL_DELAY_SAME_AS_A = new EmpiricalDelay(19, 73);

  private final Deduplicator deduplicator = new Deduplicator();
  private final DataImportIssueStore issueStore = new DefaultDataImportIssueStore();
  private final TripDelaysMapper subject = new TripDelaysMapper(
    STOP_PATTERNS,
    issueStore,
    deduplicator
  );

  @Test
  void testMap() {
    var dto = new TripDelaysDto(TRIP_ID_A);
    dto.addDelay(WEEKEND, 30, STOP_ID_C, EMPIRICAL_DELAY_C);
    dto.addDelay(WEEKEND, 20, STOP_ID_B, EMPIRICAL_DELAY_B);
    dto.addDelay(WEEKEND, 10, STOP_ID_A, EMPIRICAL_DELAY_A);

    var res = subject.map(dto).get();

    assertEquals("TripDelays{tripId: F:Trip-A, serviceIds: [Weekend]}", res.toString());
    assertEquals(EMPIRICAL_DELAY_A, res.get(WEEKEND, 0).get());
    assertEquals(EMPIRICAL_DELAY_B, res.get(WEEKEND, 1).get());
    assertEquals(EMPIRICAL_DELAY_C, res.get(WEEKEND, 2).get());
    assertEquals(
      "Deduplicator{EmpiricalDelay: 3(3), TIntObjectHashMap: 1(1)}",
      deduplicator.toString()
    );
    assertTrue(issueStore.listIssues().isEmpty());
  }

  @Test
  void testMapAndDeduplicator() {
    var dto = new TripDelaysDto(TRIP_ID_A);
    dto.addDelay(WEEKEND, 11, STOP_ID_A, EMPIRICAL_DELAY_A);
    dto.addDelay(WEEKEND, 22, STOP_ID_B, EMPIRICAL_DELAY_B);
    dto.addDelay(WEEKEND, 23, STOP_ID_C, EMPIRICAL_DELAY_C);
    dto.addDelay(MON_FRI, 5, STOP_ID_A, EMPIRICAL_DELAY_SAME_AS_A);
    dto.addDelay(MON_FRI, 6, STOP_ID_B, EMPIRICAL_DELAY_B);
    dto.addDelay(MON_FRI, 6, STOP_ID_C, EMPIRICAL_DELAY_C);

    var res = subject.map(dto).get();

    assertEquals("TripDelays{tripId: F:Trip-A, serviceIds: [Mon-Fri, Weekend]}", res.toString());
    assertEquals(EMPIRICAL_DELAY_A, res.get(WEEKEND, 0).get());
    assertEquals(EMPIRICAL_DELAY_B, res.get(WEEKEND, 1).get());
    assertEquals(EMPIRICAL_DELAY_A, res.get(MON_FRI, 0).get());
    assertEquals(EMPIRICAL_DELAY_B, res.get(MON_FRI, 1).get());
    assertEquals(
      "Deduplicator{EmpiricalDelay: 3(6), TIntObjectHashMap: 1(2)}",
      deduplicator.toString()
    );
    assertTrue(issueStore.listIssues().isEmpty(), () -> issueStore.listIssues().toString());
  }

  @Test
  void testMapWithSkippedStops() {
    var dto = new TripDelaysDto(TRIP_ID_A);
    dto.addDelay(WEEKEND, 1, STOP_ID_B, EMPIRICAL_DELAY_B);
    dto.addDelay(MON_FRI, 1, STOP_ID_A, EMPIRICAL_DELAY_A);
    dto.addDelay(MON_FRI, 2, STOP_ID_C, EMPIRICAL_DELAY_C);

    var res = subject.map(dto).get();

    assertEquals("TripDelays{tripId: F:Trip-A, serviceIds: [Mon-Fri, Weekend]}", res.toString());
    assertEquals(EMPIRICAL_DELAY_B, res.get(WEEKEND, 1).get());
    assertEquals(EMPIRICAL_DELAY_A, res.get(MON_FRI, 0).get());
    assertEquals(EMPIRICAL_DELAY_C, res.get(MON_FRI, 2).get());
    assertEquals(
      "There is no empirical delay data for listed stops. Trip: F:Trip-A, ServiceId: " +
      "Mon-Fri, Missing stops: [F:Stop-B]",
      issueStore.listIssues().get(0).getMessage()
    );
    assertEquals(
      "There is no empirical delay data for listed stops. Trip: F:Trip-A, ServiceId: Weekend, " +
      "Missing stops: [F:Stop-A, F:Stop-C]",
      issueStore.listIssues().get(1).getMessage()
    );
    assertEquals(2, issueStore.listIssues().size(), () -> issueStore.listIssues().toString());
  }

  @Test
  void testMapWithNoneExistingTrip() {
    var dto = new TripDelaysDto(TRIP_ID_OTHER);
    dto.addDelay(MON_FRI, 1, STOP_ID_A, EMPIRICAL_DELAY_A);
    dto.addDelay(MON_FRI, 2, STOP_ID_C, EMPIRICAL_DELAY_C);

    assertTrue(subject.map(dto).isEmpty());

    assertEquals(
      "Trip pattern not found for trip. Trip: F:Trip-OTHER, ServiceId: Mon-Fri.",
      issueStore.listIssues().get(0).getMessage()
    );
    assertEquals(1, issueStore.listIssues().size(), () -> issueStore.listIssues().toString());
  }

  @Test
  void testMapWithStopsOutOfOrder() {
    var dto = new TripDelaysDto(TRIP_ID_A);
    // C then A, is out of order
    dto.addDelay(WEEKEND, 1, STOP_ID_C, EMPIRICAL_DELAY_C);
    dto.addDelay(WEEKEND, 2, STOP_ID_A, EMPIRICAL_DELAY_A);

    assertTrue(subject.map(dto).isEmpty());

    assertEquals(
      "The stop sequence is wrong or the stop is not in the trip pattern. TripId: " +
      "F:Trip-A, ServiceId: Weekend, delay: DelayAtStopDto[sequence=2, stopId=F:Stop-A, " +
      "empiricalDelay=[19s, 1m13s]]",
      issueStore.listIssues().get(0).getMessage()
    );
    assertEquals(1, issueStore.listIssues().size(), () -> issueStore.listIssues().toString());
  }
}
