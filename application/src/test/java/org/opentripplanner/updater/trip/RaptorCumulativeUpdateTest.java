package org.opentripplanner.updater.trip;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;

class RaptorCumulativeUpdateTest implements RealtimeTestConstants {

  private static final LocalDate DATE_1 = LocalDate.of(2024, 5, 7);
  private static final LocalDate DATE_2 = LocalDate.of(2024, 5, 8);

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(DATE_1);
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  // A trip that runs on BOTH service dates.
  private final TripInput TRIP_INPUT = TripInput.of(TRIP_2_ID)
    .withServiceDates(DATE_1, DATE_2)
    .addStop(STOP_A, "0:01:00", "0:01:01")
    .addStop(STOP_B, "0:01:10", "0:01:11")
    .addStop(STOP_C, "0:01:20", "0:01:21");

  @Test
  void updateOnDate1SurvivesCommitTouchingOnlyDate2() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    // Baseline: both dates scheduled.
    assertThat(env.raptorData(DATE_1).summarizePatterns()).containsExactly("F:Pattern1[S]");
    assertThat(env.raptorData(DATE_2).summarizePatterns()).containsExactly("F:Pattern1[S]");

    // Commit 1: delay the trip on DATE_1 only.
    var update1 = rt
      .tripUpdateScheduled(TRIP_2_ID, DATE_1)
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 60)
      .addDelayedStopTime(2, 90)
      .build();
    assertSuccess(rt.applyTripUpdate(update1, DIFFERENTIAL));

    assertThat(env.raptorData(DATE_1).summarizePatterns()).containsExactly("F:Pattern1[U]");

    // Commit 2: delay the trip on DATE_2 only (DATE_1 is NOT in this commit's dirty set).
    var update2 = rt
      .tripUpdateScheduled(TRIP_2_ID, DATE_2)
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 60)
      .addDelayedStopTime(2, 90)
      .build();
    assertSuccess(rt.applyTripUpdate(update2, DIFFERENTIAL));

    // Trips on DATE_1 and DATE_2 are now updated.
    assertThat(env.raptorData(DATE_2).summarizePatterns()).containsExactly("F:Pattern1[U]");
    assertThat(env.raptorData(DATE_1).summarizePatterns()).containsExactly("F:Pattern1[U]");
  }
}
