package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.collection.ListSection;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.ItineraryPageCut;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class PagingFilterTest implements PlanTestConstants {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  private static final Place A = TEST_MODEL.place("A", 10, 11);
  private static final Place B = TEST_MODEL.place("B", 10, 13);
  private static final Place C = TEST_MODEL.place("C", 10, 14);
  private static final Place D = TEST_MODEL.place("D", 10, 15);

  private static final Itinerary early = newItinerary(A).bus(1, T11_04, T11_07, B).build();

  private static final Itinerary middle = newItinerary(A)
    .bus(2, T11_03, T11_05, B)
    .bus(21, T11_07, T11_10, C)
    .build();
  private static final Itinerary late = newItinerary(A).bus(3, T11_00, T11_12, B).build();
  private static final Instant oldSearchWindowEndTime = newTime(T11_05).toInstant();

  private static PagingFilter pagingFilter;

  /**
   * This set of itineraries contains all combinations of itineraries with the following values:
   * <ol>
   *   <li>departure-time: 10:00 and 10:01</li>
   *   <li>arrival-time: 11:00 and 11:01</li>
   *   <li>number of transfers: zero or one</li>
   *   <li>cost: 5 or 7</li>
   *   <li>mode: car or transit</li>
   * </ol>
   * There is 8 car itineraries and 16 transit (car do not have transfers) = 24 itineraries
   */
  private final List<Itinerary> allItineraries = allPossibleSortingCombinationsOfItineraries();

  private static final int[] INDEXES = intArrayFromTo(0, 24);

  @BeforeEach
  public void setup() {
    pagingFilter =
      new PagingFilter(
        SortOrder.STREET_AND_ARRIVAL_TIME,
        ListSection.HEAD,
        new ItineraryPageCut(
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
    assertEquals("paging-filter", pagingFilter.name());
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithEarlierArrival() {
    List<Itinerary> itineraries = List.of(early, middle, late);

    assertEquals(
      toStr(List.of(middle, late)),
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingFilter))
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
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingFilter))
    );
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithFewerNumberOfTransfers() {
    Itinerary middleNumberOfTransfers = newItinerary(A).bus(21, T11_03, T11_10, C).build();

    middleNumberOfTransfers.setGeneralizedCost(middle.getGeneralizedCost());

    List<Itinerary> itineraries = List.of(middleNumberOfTransfers, middle, late);

    assertEquals(
      toStr(List.of(middle, late)),
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingFilter))
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
      toStr(DeletionFlaggerTestHelper.process(itineraries, pagingFilter))
    );
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testDepartAfterSearchHeadFilter(int index) {
    var itineraries = allPossibleSortingCombinationsOfItineraries();

    // DEPART AFTER SEARCH

    itineraries.sort(SortOrderComparator.defaultComparatorDepartAfter());

    // Crop at top of list
    var itinerary = itineraries.get(index);
    var f = createFilter(SortOrder.STREET_AND_ARRIVAL_TIME, ListSection.HEAD, itinerary);

    var result = DeletionFlaggerTestHelper.process(itineraries, f);

    //result.forEach(it -> System.out.println(it.toStr()));

    assertEquals(toStr(itineraries.subList(index, 24)), toStr(result));
    //assertFalse(result.contains(itinerary), itinerary.toStr());
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testDepartAfterSearchTailFilter(int index) {
    var itineraries = allPossibleSortingCombinationsOfItineraries();

    // DEPART AFTER SEARCH

    itineraries.sort(SortOrderComparator.defaultComparatorDepartAfter());

    // Crop at top of list
    var itinerary = itineraries.get(index);
    var f = createFilter(SortOrder.STREET_AND_ARRIVAL_TIME, ListSection.TAIL, itinerary);

    var result = DeletionFlaggerTestHelper.process(itineraries, f);

    //result.forEach(it -> System.out.println(it.toStr()));

    assertEquals(toStr(itineraries.subList(0, index + 1)), toStr(result));
    //assertFalse(result.contains(itinerary), itinerary.toStr());
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testArrivaBySearchHeadFilter(int index) {
    var itineraries = allPossibleSortingCombinationsOfItineraries();

    // DEPART AFTER SEARCH

    itineraries.sort(SortOrderComparator.defaultComparatorArriveBy());

    // Crop at top of list
    var itinerary = itineraries.get(index);
    var f = createFilter(SortOrder.STREET_AND_DEPARTURE_TIME, ListSection.HEAD, itinerary);

    var result = DeletionFlaggerTestHelper.process(itineraries, f);

    //result.forEach(it -> System.out.println(it.toStr()));

    assertEquals(toStr(itineraries.subList(index, 24)), toStr(result));
    //assertFalse(result.contains(itinerary), itinerary.toStr());
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testArrivaBySearchTailFilter(int index) {
    var itineraries = allPossibleSortingCombinationsOfItineraries();

    // DEPART AFTER SEARCH

    itineraries.sort(SortOrderComparator.defaultComparatorArriveBy());

    // Crop at top of list
    var itinerary = itineraries.get(index);
    var f = createFilter(SortOrder.STREET_AND_DEPARTURE_TIME, ListSection.TAIL, itinerary);

    var result = DeletionFlaggerTestHelper.process(itineraries, f);

    //result.forEach(it -> System.out.println(it.toStr()));

    assertEquals(toStr(itineraries.subList(0, index + 1)), toStr(result));
    //assertFalse(result.contains(itinerary), itinerary.toStr());
  }

  private static List<Itinerary> allPossibleSortingCombinationsOfItineraries() {
    int tx_0 = 0;
    int tx_1 = 1;
    boolean car = false;
    boolean transit = true;
    List<Itinerary> itineraries = new ArrayList<>();

    for (int start : List.of(TimeUtils.time("10:00"), TimeUtils.time("10:01"))) {
      for (int end : List.of(TimeUtils.time("11:00"), TimeUtils.time("11:01"))) {
        for (int cost : List.of(5, 7)) {
          itineraries.add(itinerary(start, end, cost, tx_0, car));
          itineraries.add(itinerary(start, end, cost, tx_0, transit));
          itineraries.add(itinerary(start, end, cost, tx_1, transit));
        }
      }
    }
    return itineraries;
  }

  private PagingFilter createFilter(SortOrder sortOrder, ListSection pageCut, Itinerary cut) {
    int edt = TimeUtils.time("09:00");
    int lat = TimeUtils.time("12:00");
    return new PagingFilter(
      sortOrder,
      pageCut,
      new ItineraryPageCut(
        cut.endTimeAsInstant(),
        cut.startTimeAsInstant(),
        cut.getGeneralizedCost(),
        cut.getNumberOfTransfers(),
        cut.isOnStreetAllTheWay()
      )
    );
  }

  private static Itinerary itinerary(
    int departureTime,
    int arrivalTime,
    int cost,
    int nTransfers,
    boolean transit
  ) {
    var builder = newItinerary(A);

    if (transit) {
      if (nTransfers == 0) {
        builder.bus(10, departureTime, arrivalTime, B);
      } else if (nTransfers == 1) {
        builder
          .bus(20, departureTime, departureTime + 120, B)
          .bus(21, departureTime + 240, arrivalTime, B);
      } else {
        throw new IllegalArgumentException("nTransfers not supported: " + nTransfers);
      }
    } else {
      builder.drive(departureTime, arrivalTime, B);
    }
    var it = builder.build();
    it.setGeneralizedCost(cost);
    return it;
  }

  private static int[] intArrayFromTo(int from, int to) {
    var a = new int[to - from];
    for (int i = 0; i < a.length; ++i) {
      a[i] = i + from;
    }
    return a;
  }
}
