package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.ItineraryPageCut;
import org.opentripplanner.model.plan.pagecursor.PagingDeduplicationSection;

public class PagingDuplicateFilterTest implements PlanTestConstants {

  private static final Itinerary early = newItinerary(A).bus(1, T11_04, T11_07, B).build();

  private static final Itinerary middle = newItinerary(A)
    .bus(2, T11_03, T11_05, B)
    .bus(21, T11_07, T11_10, C)
    .build();
  private static final Itinerary late = newItinerary(A).bus(3, T11_00, T11_12, B).build();
  private static final Instant oldSearchWindowEndTime = newTime(T11_05).toInstant();

  private static PagingDuplicateFilter pagingDuplicateFilter;

  @BeforeEach
  public void setup() {
    pagingDuplicateFilter =
      new PagingDuplicateFilter(
        new ItineraryPageCut(
          late.startTime().toInstant(),
          oldSearchWindowEndTime,
          SortOrder.STREET_AND_ARRIVAL_TIME,
          PagingDeduplicationSection.HEAD,
          middle.endTime().toInstant(),
          middle.startTime().toInstant(),
          middle.getGeneralizedCost(),
          middle.getNumberOfTransfers(),
          false
        )
      );
  }

  @Test
  public void testName() {
    assertEquals("paging-deduplication-filter", pagingDuplicateFilter.name());
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithEarlierArrival() {
    List<Itinerary> itineraries = List.of(early, middle, late);

    assertEquals(
      toStr(List.of(middle, late)),
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingDuplicateFilter))
    );
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithLowerGeneralizedCost() {
    Itinerary middleLowCost = newItinerary(A)
      .bus(2, T11_03, T11_05, B)
      .bus(21, T11_07, T11_10, C)
      .build();

    middleLowCost.setGeneralizedCost(1);

    List<Itinerary> itineraries = List.of(middleLowCost, middle, late);

    assertEquals(
      toStr(List.of(middle, late)),
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingDuplicateFilter))
    );
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithFewerNumberOfTransfers() {
    Itinerary middleNumberOfTransfers = newItinerary(A).bus(21, T11_03, T11_10, C).build();

    middleNumberOfTransfers.setGeneralizedCost(middle.getGeneralizedCost());

    List<Itinerary> itineraries = List.of(middleNumberOfTransfers, middle, late);

    assertEquals(
      toStr(List.of(middle, late)),
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingDuplicateFilter))
    );
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithLaterDepartureTime() {
    Itinerary middleLaterDepartureTime = newItinerary(A)
      .bus(2, T11_04, T11_05, B)
      .bus(21, T11_07, T11_10, C)
      .build();

    middleLaterDepartureTime.setGeneralizedCost(middle.getGeneralizedCost());

    List<Itinerary> itineraries = List.of(middleLaterDepartureTime, middle, late);

    assertEquals(
      toStr(List.of(middle, late)),
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingDuplicateFilter))
    );
  }
}
