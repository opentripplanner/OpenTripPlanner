package org.opentripplanner.updater.trip.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.transit.model.timetable.StopRealTimeState.CANCELLED;
import static org.opentripplanner.transit.model.timetable.StopRealTimeState.DEFAULT;
import static org.opentripplanner.transit.model.timetable.StopRealTimeState.NO_DATA;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.utils.collection.ListUtils;

class NoDataBackwardsEarlinessInterpolatorTest {

  private static final Trip TRIP = TimetableRepositoryForTest.trip("TRIP_ID").build();
  private static final int STOP_COUNT = 5;
  private static final ScheduledTripTimes SCHEDULED_TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    TimetableRepositoryForTest.of().stopTimesEvery5Minutes(STOP_COUNT, TRIP, "00:00"),
    DeduplicatorService.NOOP
  );
  private static final int SIX_MINUTES_EARLY = -6 * 60;

  private static List<BackwardsDelayInterpolator> requiredInterpolators() {
    return List.of(
      new BackwardsDelayRequiredInterpolator(true),
      new BackwardsDelayRequiredInterpolator(false)
    );
  }

  @ParameterizedTest
  @MethodSource("requiredInterpolators")
  void precedingNoDataWithEarlyArrival(BackwardsDelayInterpolator interpolator) {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withNoData(0)
      .withNoData(1)
      .withNoData(2)
      .withArrivalDelay(3, SIX_MINUTES_EARLY)
      .withDepartureDelay(3, SIX_MINUTES_EARLY)
      .withArrivalDelay(4, SIX_MINUTES_EARLY)
      .withDepartureDelay(4, SIX_MINUTES_EARLY);

    assertThat(interpolator.propagateBackwards(builder)).hasValue(3);

    assertEquals(-60, builder.getArrivalDelay(2));
    assertEquals(-60, builder.getDepartureDelay(2));
    assertEquals(NO_DATA, builder.getStopRealTimeState(2));
    List.of(0, 1).forEach(i -> {
      assertEquals(0, builder.getArrivalDelay(i));
      assertEquals(0, builder.getDepartureDelay(i));
      assertEquals(NO_DATA, builder.getStopRealTimeState(i));
    });

    assertNotNull(builder.build());
  }

  private static List<BackwardsDelayInterpolator> allInterpolators() {
    return ListUtils.combine(
      requiredInterpolators(),
      List.of(new BackwardsDelayAlwaysInterpolator())
    );
  }

  @ParameterizedTest
  @MethodSource("allInterpolators")
  void leadingNoDataAndCancelledWithEarlyArrival(BackwardsDelayInterpolator interpolator) {
    var builder = SCHEDULED_TRIP_TIMES.createRealTimeWithoutScheduledTimes()
      .withNoData(0)
      .withCanceled(1)
      .withNoData(2)
      .withArrivalDelay(3, SIX_MINUTES_EARLY)
      .withDepartureDelay(3, SIX_MINUTES_EARLY)
      .withArrivalDelay(4, SIX_MINUTES_EARLY)
      .withDepartureDelay(4, SIX_MINUTES_EARLY);

    assertThat(interpolator.propagateBackwards(builder)).hasValue(3);

    assertEquals(NO_DATA, builder.getStopRealTimeState(2));

    assertThat(builder.stopRealTimeStates())
      .asList()
      .containsExactly(NO_DATA, CANCELLED, NO_DATA, DEFAULT, DEFAULT);

    assertNotNull(builder.build());
  }
}
