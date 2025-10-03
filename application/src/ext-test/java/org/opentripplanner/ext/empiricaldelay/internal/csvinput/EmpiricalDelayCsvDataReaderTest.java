package org.opentripplanner.ext.empiricaldelay.internal.csvinput;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.datastore.api.FileType.EMPIRICAL_DATA;
import static org.opentripplanner.ext.empiricaldelay.internal.csvinput.EmpiricalDelayCsvDataReader.CALENDAR_FILE_NAME;
import static org.opentripplanner.ext.empiricaldelay.internal.csvinput.EmpiricalDelayCsvDataReader.TRIP_TIME_DELAY_FILE_NAME;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.base.ListCompositeDataSource;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.ext.empiricaldelay.internal.model.DelayAtStopDto;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class EmpiricalDelayCsvDataReaderTest {

  private static final String MON_FRI = "Mon-Fri";
  private static final String SAT_SUN = "Sat-Sun";
  private final EmpiricalDelayCsvDataReader subject = new EmpiricalDelayCsvDataReader(
    DataImportIssueStore.NOOP
  );

  @Test
  void read() {
    var tripTimesDataSource = DataStoreModule.dataSource(
      CALENDAR_FILE_NAME,
      EMPIRICAL_DATA,
      """
      empirical_delay_service_id, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start_date, end_date
      Mon-Fri, 1, 1, 1, 1, 1, 0, 0, 2025-01-01, 2030-12-31
      Sat-Sun, 0, 0, 0, 0, 0, 1, 1, 2025-01-01, 2030-12-31
      """
    );
    var calendarDataSource = DataStoreModule.dataSource(
      TRIP_TIME_DELAY_FILE_NAME,
      EMPIRICAL_DATA,
      """
      empirical_delay_service_id, trip_id, stop_id, stop_sequence, p50, p90
      Mon-Fri, ServiceJourney:1001, Quay:3, 2, 340, 456
      Mon-Fri, ServiceJourney:1001, Quay:2, 1, 200, 512
      Mon-Fri, ServiceJourney:1001, Quay:1, 0, 23, 23
      Sat-Sun, ServiceJourney:1001, Quay:2, 1, 19, 200
      Sat-Sun, ServiceJourney:1001, Quay:3, 0, 7, 24
      Sat-Sun, ServiceJourney:1001, Quay:1, 2, 17, 90
      """
    );
    var feed = new ListCompositeDataSource(
      "EmpiricalDelayTest",
      EMPIRICAL_DATA,
      List.of(calendarDataSource, tripTimesDataSource)
    );

    subject.read(feed, "F");

    assertEquals(List.of(MON_FRI, SAT_SUN), subject.calendar().listServiceIds());
    var t1 = subject.trips().stream().findFirst().orElseThrow();
    assertEquals("F:ServiceJourney:1001", t1.tripId().toString());
    assertThat(t1.serviceIds()).containsExactly(MON_FRI, SAT_SUN);
    assertEquals(
      new DelayAtStopDto(0, new FeedScopedId("F", "Quay:1"), new EmpiricalDelay(23, 23)),
      t1.delaysSortedForServiceId(MON_FRI).get(0)
    );
    assertEquals(
      new DelayAtStopDto(1, new FeedScopedId("F", "Quay:2"), new EmpiricalDelay(200, 512)),
      t1.delaysSortedForServiceId(MON_FRI).get(1)
    );
    assertEquals(
      new DelayAtStopDto(2, new FeedScopedId("F", "Quay:3"), new EmpiricalDelay(340, 456)),
      t1.delaysSortedForServiceId(MON_FRI).get(2)
    );
    assertEquals(
      new DelayAtStopDto(0, new FeedScopedId("F", "Quay:3"), new EmpiricalDelay(7, 24)),
      t1.delaysSortedForServiceId(SAT_SUN).get(0)
    );
    assertEquals(
      new DelayAtStopDto(1, new FeedScopedId("F", "Quay:2"), new EmpiricalDelay(19, 200)),
      t1.delaysSortedForServiceId(SAT_SUN).get(1)
    );
    assertEquals(
      new DelayAtStopDto(2, new FeedScopedId("F", "Quay:1"), new EmpiricalDelay(17, 90)),
      t1.delaysSortedForServiceId(SAT_SUN).get(2)
    );
  }
}
