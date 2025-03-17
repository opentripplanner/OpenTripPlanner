package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.utils.collection.ListUtils.first;
import static org.opentripplanner.utils.collection.ListUtils.last;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner._support.debug.TestDebug;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.algorithm.filterchain.framework.sort.SortOrderComparator;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.utils.collection.ListSection;
import org.opentripplanner.utils.time.TimeUtils;

public class PagingFilterTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Place A = TEST_MODEL.place("A", 10, 11);
  private static final Place B = TEST_MODEL.place("B", 10, 13);
  private static final Place C = TEST_MODEL.place("C", 10, 14);
  private static final Place D = TEST_MODEL.place("D", 10, 15);

  private static final int EARLY_START = T11_04;
  private static final int EARLY_END = T11_07;
  private static final int MIDDLE_START = T11_03;
  private static final int MIDDLE_END = T11_10;
  private static final int LATE_START = T11_00;
  private static final int LATE_END = T11_12;

  /** [11:04, 11:07, $300, Tx0, transit] */
  private static final Itinerary early = newItinerary(A).bus(1, EARLY_START, EARLY_END, D).build();

  /**  [11:03, 11:10, $636, Tx1, transit] */
  private static final Itinerary middle = createMiddle(636);

  /** [11:00, 11:12, $840, Tx0, transit] */
  private static final Itinerary late = newItinerary(A).bus(3, LATE_START, LATE_END, D).build();

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
   * There are 8 car itineraries and 16 transit (car do not have transfers) = 24 itineraries
   */
  private final List<Itinerary> allItineraries = allPossibleSortingCombinationsOfItineraries();

  @BeforeEach
  public void setup() {
    pagingFilter = new PagingFilter(SortOrder.STREET_AND_ARRIVAL_TIME, ListSection.HEAD, middle);
  }

  @Test
  public void testName() {
    assertEquals("paging-filter", pagingFilter.name());
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithEarlierArrival() {
    List<Itinerary> itineraries = List.of(early, middle, late);

    itineraries.forEach(it -> TestDebug.println(it.keyAsString()));
    assertEquals(toStr(List.of(late)), toStr(pagingFilter.removeMatchesForTest(itineraries)));
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithLowerGeneralizedCost() {
    Itinerary middleHighCost = createMiddle(middle.generalizedCost() + 1);

    List<Itinerary> itineraries = List.of(middleHighCost, middle, late);

    assertEquals(
      toStr(List.of(middleHighCost, late)),
      toStr(pagingFilter.removeMatchesForTest(itineraries))
    );
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithFewerNumberOfTransfers() {
    int t0 = MIDDLE_START;

    Itinerary middleHighNumberOfTransfers = newItinerary(A)
      .bus(21, t0, t0 + D1m, B)
      .bus(22, t0 + D2m, t0 + D3m, C)
      .bus(23, t0 + D4m, MIDDLE_END, D)
      .build(middle.generalizedCost());

    List<Itinerary> itineraries = List.of(middleHighNumberOfTransfers, middle, late);

    assertEquals(
      toStr(List.of(middleHighNumberOfTransfers, late)),
      toStr(pagingFilter.removeMatchesForTest(itineraries))
    );
  }

  @Test
  public void testPotentialDuplicateMarkedForDeletionWithLaterDepartureTime() {
    int t0 = MIDDLE_START;
    Itinerary middleEarlierDepartureTime = newItinerary(A)
      .bus(2, t0 - D1m, t0 + D3m, B)
      .bus(21, t0 + D4m, MIDDLE_END, C)
      .build(middle.generalizedCost());

    List<Itinerary> itineraries = List.of(middleEarlierDepartureTime, middle, late);

    assertEquals(
      toStr(List.of(middleEarlierDepartureTime, late)),
      toStr(pagingFilter.removeMatchesForTest(itineraries))
    );
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testDepartAfterSearchHeadFilter(int index) {
    allItineraries.sort(SortOrderComparator.defaultComparatorDepartAfter());

    // Crop at top of list
    var itinerary = allItineraries.get(index);
    var f = new PagingFilter(SortOrder.STREET_AND_ARRIVAL_TIME, ListSection.HEAD, itinerary);

    var result = f.removeMatchesForTest(allItineraries);

    result.forEach(it -> TestDebug.println(it.toStr()));

    if (index == 23) {
      assertEquals("", toStr(result));
    } else {
      assertItineraryEq(allItineraries.get(index + 1), first(result));
    }
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testDepartAfterSearchTailFilter(int index) {
    allItineraries.sort(SortOrderComparator.defaultComparatorDepartAfter());

    // Crop at top of list
    var itinerary = allItineraries.get(index);
    var f = new PagingFilter(SortOrder.STREET_AND_ARRIVAL_TIME, ListSection.TAIL, itinerary);

    var result = f.removeMatchesForTest(allItineraries);

    result.forEach(it -> TestDebug.println(it.toStr()));

    if (index == 0) {
      assertEquals("", toStr(result));
    } else {
      assertItineraryEq(allItineraries.get(index - 1), last(result));
    }
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testArriveBySearchHeadFilter(int index) {
    allItineraries.sort(SortOrderComparator.defaultComparatorArriveBy());

    // Crop at top of list
    var itinerary = allItineraries.get(index);
    var f = new PagingFilter(SortOrder.STREET_AND_DEPARTURE_TIME, ListSection.HEAD, itinerary);

    var result = f.removeMatchesForTest(allItineraries);

    result.forEach(it -> TestDebug.println(it.toStr()));

    if (index == 23) {
      assertEquals("", toStr(result));
    } else {
      assertItineraryEq(allItineraries.get(index + 1), first(result));
    }
  }

  @ParameterizedTest
  @ValueSource(
    ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }
  )
  public void testArriveBySearchTailFilter(int index) {
    allItineraries.sort(SortOrderComparator.defaultComparatorArriveBy());

    // Crop at top of list
    var pageCut = allItineraries.get(index);
    var f = new PagingFilter(SortOrder.STREET_AND_DEPARTURE_TIME, ListSection.TAIL, pageCut);

    var result = f.removeMatchesForTest(allItineraries);

    result.forEach(it -> TestDebug.println(it.toStr()));

    if (index == 0) {
      assertEquals("", toStr(result));
    } else {
      assertItineraryEq(allItineraries.get(index - 1), last(result));
    }
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
    var it = builder.build(cost);
    return it;
  }

  private static Itinerary createMiddle(int generalizedCost) {
    return createMiddleBuilder().build(generalizedCost);
  }

  private static TestItineraryBuilder createMiddleBuilder() {
    return newItinerary(A)
      .bus(2, MIDDLE_START, MIDDLE_START + D2m, B)
      .bus(21, MIDDLE_END - D3m, MIDDLE_END, D);
  }

  private static void assertItineraryEq(Itinerary expected, Itinerary actual) {
    assertEquals(expected.keyAsString(), actual.keyAsString());
  }
}
