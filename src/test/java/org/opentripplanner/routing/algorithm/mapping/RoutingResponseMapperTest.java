package org.opentripplanner.routing.algorithm.mapping;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.SearchParams;

public class RoutingResponseMapperTest {

  static final ZonedDateTime TRANSIT_TIME_ZERO = TestItineraryBuilder.SERVICE_DAY.atStartOfDay(UTC);

  static final int T12_00 = TimeUtils.hm2time(12, 0);
  static final int T12_30 = TimeUtils.hm2time(12, 30);
  static final int T13_00 = TimeUtils.hm2time(13, 0);
  static final int T13_30 = TimeUtils.hm2time(13, 30);

  static final Duration D1H = Duration.ofHours(1);
  static final Duration D90M = Duration.ofMinutes(90);

  private static final SearchParams SEARCH_PARAMS = new RaptorRequestBuilder<TestTripSchedule>()
    .searchParams()
    .earliestDepartureTime(T12_00)
    .latestArrivalTime(T13_30)
    .searchWindow(D1H)
    .buildSearchParam();

  private static final Itinerary REMOVED_ITINERARY = TestItineraryBuilder
    .newItinerary(A, T12_30)
    .bus(1, T12_30, T13_00, B)
    .build();

  @Test
  public void mapIntoPageCursorFactoryNoTransitSearchParams() {
    var factory = RoutingResponseMapper.mapIntoPageCursorFactory(
      SortOrder.STREET_AND_ARRIVAL_TIME,
      TRANSIT_TIME_ZERO,
      null,
      null,
      null,
      PageType.NEXT_PAGE
    );

    assertNull(factory.nextPageCursor());
    assertNull(factory.previousPageCursor());
  }

  @Test
  void mapIntoPageCursorFactoryWithSearchParamsNoItineraryRemoved() {
    var factory = RoutingResponseMapper.mapIntoPageCursorFactory(
      SortOrder.STREET_AND_ARRIVAL_TIME,
      TRANSIT_TIME_ZERO,
      SEARCH_PARAMS,
      D90M,
      null,
      PageType.NEXT_PAGE
    );

    // There is no way to access the internals of the factory, so we use the toString()
    assertEquals(
      "PageCursorFactory{" +
      "sortOrder: STREET_AND_ARRIVAL_TIME, " +
      "currentPageType: NEXT_PAGE, " +
      "current: SearchTime{edt: 2020-02-02T12:00:00Z, lat: 2020-02-02T13:30:00Z}, " +
      "currentSearchWindow: 1h, " +
      "newSearchWindow: 1h30m" +
      "}",
      factory.toString()
    );
  }

  @Test
  public void testArriveByReversedRemovedInsidePreviousPage() {
    var factory = RoutingResponseMapper.mapIntoPageCursorFactory(
      SortOrder.STREET_AND_DEPARTURE_TIME,
      TRANSIT_TIME_ZERO,
      SEARCH_PARAMS,
      D90M,
      REMOVED_ITINERARY,
      PageType.NEXT_PAGE
    );

    // There is no way to access the internals of the factory, so we use the toString()
    assertEquals(
      "PageCursorFactory{" +
      "sortOrder: STREET_AND_DEPARTURE_TIME, " +
      "currentPageType: NEXT_PAGE, " +
      "current: SearchTime{edt: 2020-02-02T12:00:00Z, lat: 2020-02-02T13:30:00Z}, " +
      "currentSearchWindow: 1h, " +
      "newSearchWindow: 1h30m, " +
      "searchWindowCropped, " +
      "removedItineraryStartTime: 2020-02-02T12:30:00Z, " +
      "removedItineraryEndTime: 2020-02-02T13:00:00Z" +
      "}",
      factory.toString()
    );
  }
}
