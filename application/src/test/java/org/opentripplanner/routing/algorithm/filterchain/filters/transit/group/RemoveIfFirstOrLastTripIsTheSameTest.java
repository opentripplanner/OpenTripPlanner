package org.opentripplanner.routing.algorithm.filterchain.filters.transit.group;

import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class RemoveIfFirstOrLastTripIsTheSameTest implements PlanTestConstants {

  /**
   * This test ensures that the filter works as intended regarding comparison order and exclusion
   * logic.
   */
  @Test
  public void testMatchOrderOnFirstStation() {
    final int ID_1 = 1;
    final int ID_2 = 2;
    final int ID_3 = 3;
    final int ID_4 = 4;
    final int ID_5 = 5;

    Itinerary i1 = newItinerary(A).bus(ID_1, 0, 50, B).bus(ID_2, 52, 100, C).build();

    Itinerary i2 = newItinerary(A).bus(ID_2, 0, 50, B).bus(ID_3, 52, 100, C).build();

    Itinerary i3 = newItinerary(A).bus(ID_1, 0, 50, B).bus(ID_4, 52, 100, C).build();

    Itinerary i4 = newItinerary(A).bus(ID_5, 0, 50, B).bus(ID_3, 52, 100, C).build();

    Itinerary i5 = newItinerary(A).bus(ID_5, 0, 50, B).bus(ID_4, 52, 100, C).build();

    List<Itinerary> input = List.of(i1, i2, i3, i4, i5);

    final RemoveIfFirstOrLastTripIsTheSame filter = new RemoveIfFirstOrLastTripIsTheSame();
    var flagged = filter.flagForRemoval(input);

    // First journey should always be included
    assertFalse(flagged.contains(i1));
    // Does not match with i1
    assertFalse(flagged.contains(i2));
    // Matches with i1
    assertTrue(flagged.contains(i3));
    // Matches with i2
    assertTrue(flagged.contains(i4));
    // Would match with i3 and i4, but they are filtered out
    assertFalse(flagged.contains(i5));
  }
}
