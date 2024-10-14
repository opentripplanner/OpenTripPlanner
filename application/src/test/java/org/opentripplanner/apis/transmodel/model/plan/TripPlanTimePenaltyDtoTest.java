package org.opentripplanner.apis.transmodel.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;

class TripPlanTimePenaltyDtoTest {

  private static final TimeAndCost PENALTY = new TimeAndCost(
    DurationUtils.duration("20m30s"),
    Cost.costOfSeconds(21)
  );

  private final TransitModelForTest testModel = TransitModelForTest.of();
  private final Place placeA = Place.forStop(testModel.stop("A").build());
  private final Place placeB = Place.forStop(testModel.stop("B").build());

  @Test
  void testCreateFromSingeEntry() {
    assertNull(TripPlanTimePenaltyDto.of("access", null));
    assertNull(TripPlanTimePenaltyDto.of("access", TimeAndCost.ZERO));
    assertEquals(
      new TripPlanTimePenaltyDto("access", PENALTY),
      TripPlanTimePenaltyDto.of("access", PENALTY)
    );
  }

  @Test
  void testCreateFromItineraryWithNoPenalty() {
    var i = itinerary();
    assertEquals(List.of(), TripPlanTimePenaltyDto.of(i));
  }

  @Test
  void testCreateFromItineraryWithAccess() {
    var i = itinerary();
    i.setAccessPenalty(PENALTY);
    assertEquals(
      List.of(new TripPlanTimePenaltyDto("access", PENALTY)),
      TripPlanTimePenaltyDto.of(i)
    );
  }

  @Test
  void testCreateFromItineraryWithEgress() {
    var i = itinerary();
    i.setEgressPenalty(PENALTY);
    assertEquals(
      List.of(new TripPlanTimePenaltyDto("egress", PENALTY)),
      TripPlanTimePenaltyDto.of(i)
    );
  }

  private Itinerary itinerary() {
    return TestItineraryBuilder.newItinerary(placeA).drive(100, 200, placeB).build();
  }
}
