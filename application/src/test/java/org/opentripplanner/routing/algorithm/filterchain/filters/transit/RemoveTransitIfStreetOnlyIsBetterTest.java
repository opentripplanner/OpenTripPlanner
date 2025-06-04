package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

public class RemoveTransitIfStreetOnlyIsBetterTest implements PlanTestConstants {

  @Test
  void filterAwayNothingIfNoWalking() {
    // Given:
    var i1 = newItinerary(A).bus(21, 6, 7, E).build();
    var i2 = newItinerary(A).rail(110, 6, 9, E).build();

    // When:
    RemoveTransitIfStreetOnlyIsBetter flagger = new RemoveTransitIfStreetOnlyIsBetter(
      CostLinearFunction.of(Duration.ofSeconds(200), 1.2),
      null
    );
    List<Itinerary> result = flagger.removeMatchesForTest(List.of(i1, i2));

    // Then:
    assertEquals(toStr(List.of(i1, i2)), toStr(result));
    assertEquals(flagger.getRemoveTransitIfStreetOnlyIsBetterResult(), null);
  }

  @Test
  void filterAwayLongTravelTimeWithoutWaitTime() {
    // Given: a walk itinerary with high cost - do not have any effect on filtering
    var walk = newItinerary(A, 6).walk(1, E).build(300);

    // Given: a bicycle itinerary with low cost - transit with clearly higher cost are removed
    var bicycle = newItinerary(A).bicycle(6, 8, E).build(200);

    // transit with almost equal cost should not be dropped
    var i1 = newItinerary(A).bus(21, 6, 8, E).build(220);

    // transit with considerably higher cost will be dropped
    var i2 = newItinerary(A).bus(31, 6, 8, E).build(360);

    // When:
    RemoveTransitIfStreetOnlyIsBetter flagger = new RemoveTransitIfStreetOnlyIsBetter(
      CostLinearFunction.of(Duration.ofSeconds(60), 1.2),
      null
    );
    List<Itinerary> result = flagger.removeMatchesForTest(List.of(i2, bicycle, walk, i1));

    // Then:
    assertEquals(toStr(List.of(bicycle, walk, i1)), toStr(result));
    assertEquals(
      flagger.getRemoveTransitIfStreetOnlyIsBetterResult().generalizedCostMaxLimit(),
      Cost.costOfSeconds(bicycle.generalizedCost())
    );
  }

  @Test
  void filterAwayLongTravelTimeWithoutWaitTimeWithCursorInfoAndWithoutDirectItinerary() {
    // transit with almost equal cost should not be dropped
    Itinerary i1 = newItinerary(A).bus(21, 6, 8, E).build(220);

    // transit with considerably higher cost will be dropped
    Itinerary i2 = newItinerary(A).bus(31, 6, 8, E).build(360);

    // When:
    RemoveTransitIfStreetOnlyIsBetter flagger = new RemoveTransitIfStreetOnlyIsBetter(
      CostLinearFunction.of(Duration.ofSeconds(60), 1.2),
      // This generalized cost that usually comes from the cursor should be used because it is the
      // only cost given to the filter because no direct itineraries exist.
      Cost.costOfSeconds(199)
    );
    List<Itinerary> result = flagger.removeMatchesForTest(List.of(i2, i1));

    // Then:
    assertEquals(toStr(List.of(i1)), toStr(result));
    // The lowest generalized cost value should be saved
    assertEquals(
      flagger.getRemoveTransitIfStreetOnlyIsBetterResult().generalizedCostMaxLimit(),
      Cost.costOfSeconds(199)
    );
  }

  @Nested
  class AccessEgressPenalties {

    private static final RemoveTransitIfStreetOnlyIsBetter FLAGGER =
      new RemoveTransitIfStreetOnlyIsBetter(CostLinearFunction.of(Duration.ZERO, 1.0), null);

    @Test
    void keepBusWithLowCostAndPenalty() {
      var walk = newItinerary(A, 6).walk(1, E).build(300);

      // transit has slightly lower cost, however it also has a high penalty which is
      // not taken into account when comparing the itineraries
      var busWithPenalty = newItinerary(A)
        .bus(21, 6, 8, E)
        .itineraryBuilder()
        .withAccessPenalty(new TimeAndCost(Duration.ZERO, Cost.costOfSeconds(360)))
        .withGeneralizedCost(Cost.costOfSeconds(299 + 360))
        .build();

      // When:
      var itineraries = List.of(walk, busWithPenalty);

      List<Itinerary> result = FLAGGER.removeMatchesForTest(itineraries);

      // Then:
      assertEquals(toStr(itineraries), toStr(result));
    }

    @Test
    void removeBusWithHighCostAndNoPenalty() {
      var walk = newItinerary(A, 6).walk(1, E).build(300);

      // transit has slightly lower cost, however it also has a high penalty which is
      // not taken into account when comparing the itineraries
      var bus = newItinerary(A).bus(21, 6, 8, E).build(301);

      // When:
      var itineraries = List.of(walk, bus);

      List<Itinerary> result = FLAGGER.removeMatchesForTest(itineraries);

      // Then:
      assertEquals(toStr(List.of(walk)), toStr(result));
    }
  }
}
