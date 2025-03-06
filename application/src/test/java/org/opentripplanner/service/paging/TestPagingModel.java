package org.opentripplanner.service.paging;

import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.utils.time.TimeUtils.hm2time;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.algorithm.filterchain.framework.sort.SortOrderComparator;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.utils.time.TimeUtils;

class TestPagingModel {

  // Times CASE - A
  static final int T12_00 = hm2time(12, 0);
  static final int T12_09 = hm2time(12, 9);
  static final int T12_10 = hm2time(12, 10);
  static final int T12_25 = hm2time(12, 25);
  static final int T12_29 = hm2time(12, 29);
  static final int T12_30 = hm2time(12, 30);
  static final int T12_39 = hm2time(12, 39);
  static final int T12_40 = hm2time(12, 40);
  static final int T12_41 = hm2time(12, 41);
  static final int T12_55 = hm2time(12, 55);
  static final int T12_59 = hm2time(12, 59);
  static final int T13_00 = hm2time(13, 0);
  static final int T13_10 = hm2time(13, 10);
  static final int T13_11 = hm2time(13, 11);
  static final int T13_25 = hm2time(13, 25);
  static final int T13_30 = hm2time(13, 30);
  static final int TIME_NOT_SET = -9_999_999;

  // Times CASE - B
  static final int T15_00_MINUS_1d = TimeUtils.time("15:00:00-1d");
  static final int T15_30_MINUS_1d = TimeUtils.time("15:30:00-1d");
  static final int T09_00 = hm2time(9, 0);
  static final int T09_30 = hm2time(9, 30);
  static final int T15_00 = hm2time(15, 0);
  static final int T15_30 = hm2time(15, 30);
  static final int T09_00_PLUS_1d = TimeUtils.time("09:00:00+1d");
  static final int T09_30_PLUS_1d = TimeUtils.time("09:30:00+1d");

  static final Duration D30m = Duration.ofMinutes(30);

  // The SEARCH-WINDOW is set to "fixed" 30m in this test for simplicity
  private static final List<Duration> SEARCH_WINDOW_ADJUSTMENTS = List.of();

  private static final Instant TRANSIT_START_TIME = TestItineraryBuilder.newTime(0).toInstant();

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Place A = Place.forStop(TEST_MODEL.stop("A").build());
  private static final Place B = Place.forStop(TEST_MODEL.stop("B").build());

  static final boolean ON_STREET = false;
  static final boolean TRANSIT = true;
  static final int COST_HIGH = 10;
  static final int COST_LOW = 9;
  static final int TX_1 = 1;
  static final int TX_0 = 0;

  private static final List<Itinerary> ITINERARIES_CASE_A = List.of(
    // EDT time-shifted onStreet result (apply to first depart-after search)
    itinerary(T12_00, T12_30, COST_HIGH, TX_0, ON_STREET),
    // Next  itineraries are almost the same - a criterion is better for each
    itinerary(T12_10, T12_41, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_10, T12_40, COST_LOW, TX_1, TRANSIT),
    itinerary(T12_10, T12_40, COST_HIGH, TX_0, TRANSIT),
    itinerary(T12_09, T12_40, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_25, T12_55, COST_HIGH, TX_1, TRANSIT),
    // An itinerary with a very long duration
    itinerary(T12_29, T13_11, COST_LOW, TX_0, TRANSIT),
    // Next  itineraries are almost the same - a criterion is better for each
    itinerary(T12_40, T13_11, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_40, T13_10, COST_LOW, TX_1, TRANSIT),
    itinerary(T12_40, T13_10, COST_HIGH, TX_0, TRANSIT),
    itinerary(T12_39, T13_10, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_55, T13_25, COST_HIGH, TX_1, TRANSIT),
    // LAT time-shifted onStreet result (apply to first arrive-by search)
    itinerary(T12_59, T13_30, COST_HIGH, TX_0, ON_STREET)
  );
  static final List<Itinerary> ITINERARIES_CASE_A_DEPART_AFTER = ITINERARIES_CASE_A
    // Skip last itinerary (onStreet arriveBy)
    .subList(0, ITINERARIES_CASE_A.size() - 1)
    .stream()
    .sorted(SortOrderComparator.comparator(STREET_AND_ARRIVAL_TIME))
    .toList();

  static final List<Itinerary> ITINERARIES_CASE_A_ARRIVE_BY = ITINERARIES_CASE_A
    // Skip first itinerary (onStreet departAfter)
    .subList(1, ITINERARIES_CASE_A.size())
    .stream()
    .sorted(SortOrderComparator.comparator(SortOrder.STREET_AND_DEPARTURE_TIME))
    .toList();

  /**
   * Case B only have 4 itineraries over 3 days:
   * <pre>
   *  - 15:00-1d
   *  - 12:00
   *  - 15:00
   *  - 12:00+1d
   *  </pre>
   */
  private static final List<Itinerary> ITINERARIES_CASE_B = List.of(
    itinerary(T15_00_MINUS_1d, T15_30_MINUS_1d, COST_HIGH, TX_1, TRANSIT),
    itinerary(T09_00, T09_30, COST_HIGH, TX_1, TRANSIT),
    itinerary(T15_00, T15_30, COST_HIGH, TX_1, TRANSIT),
    itinerary(T09_00_PLUS_1d, T09_30_PLUS_1d, COST_HIGH, TX_1, TRANSIT)
  );

  static final List<Itinerary> ITINERARIES_CASE_B_DEPART_AFTER = ITINERARIES_CASE_B.stream()
    .sorted(SortOrderComparator.comparator(STREET_AND_ARRIVAL_TIME))
    .toList();

  static final List<Itinerary> ITINERARIES_CASE_B_ARRIVE_BY = ITINERARIES_CASE_B.stream()
    .sorted(SortOrderComparator.comparator(SortOrder.STREET_AND_DEPARTURE_TIME))
    .toList();

  private final List<Itinerary> itinerariesDepartAfter;
  private final List<Itinerary> itinerariesArriveBy;

  private TestPagingModel(
    List<Itinerary> itinerariesDepartAfter,
    List<Itinerary> itinerariesArriveBy
  ) {
    this.itinerariesDepartAfter = itinerariesDepartAfter;
    this.itinerariesArriveBy = itinerariesArriveBy;
  }

  static TestPagingModel testDataWithManyItinerariesCaseA() {
    return new TestPagingModel(ITINERARIES_CASE_A_DEPART_AFTER, ITINERARIES_CASE_A_ARRIVE_BY);
  }

  static TestPagingModel testDataWithFewItinerariesCaseB() {
    return new TestPagingModel(ITINERARIES_CASE_B_DEPART_AFTER, ITINERARIES_CASE_B_ARRIVE_BY);
  }

  static PagingService pagingService(TestDriver testDriver) {
    return new PagingService(
      SEARCH_WINDOW_ADJUSTMENTS,
      testDriver.searchWindow(),
      testDriver.searchWindow(),
      testDriver.searchWindow(),
      testDriver.earliestDepartureTime(),
      testDriver.latestArrivalTime(),
      testDriver.sortOrder(),
      testDriver.arrivedBy(),
      testDriver.nResults(),
      null,
      testDriver.filterResults(),
      testDriver.kept()
    );
  }

  static PagingService pagingService(TestDriver testDriver, PageCursor pageCursor) {
    return new PagingService(
      SEARCH_WINDOW_ADJUSTMENTS,
      testDriver.searchWindow(),
      testDriver.searchWindow(),
      testDriver.searchWindow(),
      pageCursor.earliestDepartureTime(),
      pageCursor.latestArrivalTime(),
      pageCursor.originalSortOrder(),
      testDriver.arrivedBy(),
      testDriver.nResults(),
      pageCursor,
      testDriver.filterResults(),
      testDriver.kept()
    );
  }

  TestDriver arriveByDriver(int edt, int lat, Duration searchWindow, int nResults) {
    return TestDriver.driver(
      edt,
      lat,
      searchWindow,
      nResults,
      STREET_AND_DEPARTURE_TIME,
      itinerariesArriveBy
    );
  }

  TestDriver departAfterDriver(int edt, Duration searchWindow, int nResults) {
    return TestDriver.driver(
      edt,
      TIME_NOT_SET,
      searchWindow,
      nResults,
      STREET_AND_ARRIVAL_TIME,
      itinerariesDepartAfter
    );
  }

  static Instant time(int time) {
    return time == TIME_NOT_SET ? null : TRANSIT_START_TIME.plusSeconds(time);
  }

  private static Itinerary itinerary(
    int departureTime,
    int arrivalTime,
    int cost,
    int nTransfers,
    boolean transit
  ) {
    var builder = TestItineraryBuilder.newItinerary(A);

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
}
