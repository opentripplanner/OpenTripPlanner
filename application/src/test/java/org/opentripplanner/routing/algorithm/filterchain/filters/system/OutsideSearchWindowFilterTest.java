package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;

public class OutsideSearchWindowFilterTest implements PlanTestConstants {

  private static final Duration SEARCH_WINDOW_10m = Duration.ofMinutes(10);
  private final int startTime = time("09:30");
  private final int endTime = time("09:40");

  private final Itinerary itinerary = newItinerary(A).bus(32, startTime, endTime, E).build();
  private final List<Itinerary> input = List.of(itinerary);

  static List<Arguments> filterOnSearchWindowTestCases() {
    return List.of(
      Arguments.of("Departure time(09:30) matches earliest-departure-time", "09:30", false),
      Arguments.of("Departure time(09:30) matches latest-departure-time", "09:20:01", false),
      Arguments.of("Departure time(09:30) is before earliest-departure-time", "09:30:01", true),
      Arguments.of("Departure time(09:30) is after latest-departure-time", "09:20", true)
    );
  }

  @ParameterizedTest(name = "{0}, edt: {1}, sw: 10m, expects flagged for removal: {2}")
  @MethodSource("filterOnSearchWindowTestCases")
  public void filterOnSearchWindow(String description, String edt, boolean flaggedForRemoval) {
    List<Itinerary> expected = flaggedForRemoval ? input : List.of();
    var subject = new OutsideSearchWindowFilter(
      TestItineraryBuilder.newTime(time(edt)).toInstant(),
      SEARCH_WINDOW_10m
    );
    var result = subject.flagForRemoval(input);
    assertEquals(expected, result, description);
  }

  @Test
  public void testTaggedBy() {
    var it = newItinerary(A).bus(32, 0, 60, E).build();
    assertFalse(OutsideSearchWindowFilter.taggedBy(it));

    it.flagForDeletion(new SystemNotice(OutsideSearchWindowFilter.TAG, "Text"));
    assertTrue(OutsideSearchWindowFilter.taggedBy(it));
  }

  private static Stream<Itinerary> onStreetTestCases() {
    int t9_28 = time("9:28");
    int t9_38 = time("9:38");
    return Stream.of(
      newItinerary(A, t9_28).walk(D2m, B),
      newItinerary(A, t9_38).walk(D12m, B),
      newItinerary(A, t9_28).bicycle(t9_28, t9_38, B),
      newItinerary(A, t9_28).flex(t9_28, t9_38, B),
      newItinerary(A, t9_28).flex(t9_38, time("9:48"), B),
      newItinerary(A, time("9:20")).flex(time("9:20"), t9_28, B).walk(D12m, C)
    ).map(b -> b.withIsSearchWindowAware(false).build()); // results from the street & flex routers are not aware of the search window
  }

  @ParameterizedTest
  @MethodSource("onStreetTestCases")
  void onStreetArriveByShouldNotBeRemoved(Itinerary itin) {
    var edt = "9:20";
    var subject = new OutsideSearchWindowFilter(
      TestItineraryBuilder.newTime(time(edt)).toInstant(),
      SEARCH_WINDOW_10m
    );
    assertThat(subject.flagForRemoval(List.of(itin))).isEmpty();
  }
}
