package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;

class FlexSearchWindowFilterTest implements PlanTestConstants {

  private static final Instant LATEST_DEPARTURE_TIME = TestItineraryBuilder.newTime(
    time("09:20")
  ).toInstant();

  @ParameterizedTest
  @ValueSource(strings = { "09:20", "09:21", "13:20" })
  void keepArriveByFlexItinerariesAfterEDT(String startTime) {
    var edt = "9:20";
    var subject = new FlexSearchWindowFilter(
      TestItineraryBuilder.newTime(time(edt)).toInstant(),
      Duration.ofMinutes(30),
      SortOrder.STREET_AND_DEPARTURE_TIME
    );

    var itin = newItinerary(A, time(startTime))
      .flex(T11_00, T11_30, B)
      .withIsSearchWindowAware(false)
      .build();

    assertThat(subject.flagForRemoval(List.of(itin))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = { "00:00", "00:01", "09:19" })
  void removeArriveByFlexItinerariesBeforeEDT(String startTime) {
    var subject = new FlexSearchWindowFilter(
      LATEST_DEPARTURE_TIME,
      Duration.ofMinutes(30),
      SortOrder.STREET_AND_DEPARTURE_TIME
    );

    var itin = newItinerary(A, time(startTime))
      .flex(T11_00, T11_30, B)
      .withIsSearchWindowAware(false)
      .build();

    assertThat(subject.flagForRemoval(List.of(itin))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = { "12:00" })
  void removeDepartAtFlexItinerariesAfterLAT(String startTime) {
    var subject = new FlexSearchWindowFilter(
      LATEST_DEPARTURE_TIME,
      Duration.ofMinutes(30),
      SortOrder.STREET_AND_ARRIVAL_TIME
    );

    var itin = newItinerary(A, time(startTime))
      .flex(T11_00, T11_30, B)
      .withIsSearchWindowAware(false)
      .build();

    assertEquals(subject.flagForRemoval(List.of(itin)), List.of(itin));
  }
}
