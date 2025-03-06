package org.opentripplanner.routing.algorithm.filterchain.framework.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.algorithm.filterchain.framework.sort.SortOrderComparator.numberOfTransfersComparator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class SortOnNumberOfTransfersTest implements PlanTestConstants {

  @Test
  public void sortOnNumberOfTransfers() {
    List<Itinerary> result;

    // Given: alternatives with zero one and two transfers
    Itinerary zeroTransfers = newItinerary(A).bus(1, 0, 200, C).build();

    Itinerary oneTransfer = newItinerary(A).bus(1, 0, 50, B).bus(11, 50, 100, C).build();
    Itinerary twoTransfers = newItinerary(A)
      .bus(1, 0, 50, A)
      .bus(11, 50, 70, B)
      .bus(21, 70, 90, C)
      .build();

    // When: sorting
    result = Stream.of(twoTransfers, oneTransfer, zeroTransfers)
      .sorted(numberOfTransfersComparator())
      .collect(Collectors.toList());

    // Then: expect the results to be in order according to number of transfers
    assertEquals(toStr(List.of(zeroTransfers, oneTransfer, twoTransfers)), toStr(result));
  }
}
