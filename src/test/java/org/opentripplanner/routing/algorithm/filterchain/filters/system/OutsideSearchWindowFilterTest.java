package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;

public class OutsideSearchWindowFilterTest implements PlanTestConstants {

  private static final Duration SEARCH_WINDOW_10m = Duration.ofMinutes(10);
  private final int startTime = TimeUtils.time("09:30");
  private final int endTime = TimeUtils.time("09:40");

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
      TestItineraryBuilder.newTime(TimeUtils.time(edt)).toInstant(),
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
}
