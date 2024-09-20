package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.framework.time.TimeUtils.time;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;

class FlexSearchWindowFilterTest implements PlanTestConstants {

  private static final Instant LATEST_DEPARTURE_TIME = TestItineraryBuilder
    .newTime(time("9:20"))
    .toInstant();

  @ParameterizedTest
  @ValueSource(strings = { "9:20", "9:21", "13:20" })
  void keepFlexItinerariesAfterLDT(String startTime) {
    var edt = "9:20";
    var subject = new FlexSearchWindowFilter(TestItineraryBuilder.newTime(time(edt)).toInstant());

    var itin = newItinerary(A, time(startTime))
      .flex(T11_00, T11_30, B)
      .withIsSearchWindowAware(false)
      .build();

    assertThat(subject.flagForRemoval(List.of(itin))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = { "0:0", "0:01", "9:19" })
  void removeFlexItinerariesBeforeLDT(String startTime) {
    var subject = new FlexSearchWindowFilter(LATEST_DEPARTURE_TIME);

    var itin = newItinerary(A, time(startTime))
      .flex(T11_00, T11_30, B)
      .withIsSearchWindowAware(false)
      .build();

    assertThat(subject.flagForRemoval(List.of(itin))).isEmpty();
  }
}
