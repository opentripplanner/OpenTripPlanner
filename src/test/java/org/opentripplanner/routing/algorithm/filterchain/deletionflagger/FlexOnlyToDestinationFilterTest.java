package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;

class FlexOnlyToDestinationFilterTest implements PlanTestConstants {

  FlexOnlyToDestinationFilter filter = new FlexOnlyToDestinationFilter();

  @Test
  void transit() {
    var regularTransit = newItinerary(A)
      .rail(20, T11_05, T11_14, B)
      .bus(30, T11_16, T11_20, C)
      .build();

    var result = filter.filter(List.of(regularTransit, regularTransit, regularTransit));
    assertEquals(List.of(regularTransit, regularTransit, regularTransit), result);
  }

  @Test
  void flexWithLongWalk() {
    var regularTransit = newItinerary(A).flex(T11_16, T11_20, C).walk(mins(3), D).build();

    // if there is a "long" walk, we want to filter it out
    var result = filter.filter(List.of(regularTransit));
    assertEquals(List.of(), result);
  }

  @Test
  void flexWithShortWalk() {
    var flex = newItinerary(A).flex(T11_16, T11_20, C).walk(mins(1), D).build();

    // if there is a "long" walk, we want to filter it out
    var result = filter.filter(List.of(flex));
    assertEquals(List.of(flex), result);
  }

  @Test
  void longAndShortWalk() {
    var shortWalk = newItinerary(A).flex(T11_16, T11_20, C).walk(mins(1), D).build();
    var longWalk = newItinerary(A).flex(T11_16, T11_20, C).walk(mins(3), D).build();

    var result = filter.filter(List.of(shortWalk, longWalk));
    assertEquals(List.of(shortWalk), result);
  }

  private static int mins(int minutes) {
    return (int) Duration.ofMinutes(minutes).toSeconds();
  }
}
