package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.service.paging.PagingService;

class RoutingResponseMapperTest {

  private static final Instant DATE_TIME = Instant.parse("2026-07-28T12:00:00Z");
  private static final PageCursor NEXT_PAGE_CURSOR = pageCursor(PageType.NEXT_PAGE);
  private static final PageCursor PREVIOUS_PAGE_CURSOR = pageCursor(PageType.PREVIOUS_PAGE);

  @Test
  void pageCursorsAreReturnedForARegularSearch() {
    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(60.0, 25.0))
      .withTo(GenericLocation.fromCoordinate(60.1, 25.1))
      .withDateTime(DATE_TIME)
      .buildRequest();

    var response = map(request);

    assertEquals(NEXT_PAGE_CURSOR, response.getNextPageCursor());
    assertEquals(PREVIOUS_PAGE_CURSOR, response.getPreviousPageCursor());
  }

  /**
   * A start-on-board search is pinned to the boarding time of a single dated trip, so there are
   * no other pages and no page cursors should be returned.
   */
  @Test
  void pageCursorsAreOmittedForAStartOnBoardSearch() {
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(
        new FeedScopedId("F", "trip-1"),
        LocalDate.of(2026, 7, 28)
      ),
      new FeedScopedId("F", "stop-1")
    );
    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromTripLocation(tripLocation))
      .withTo(GenericLocation.fromCoordinate(60.1, 25.1))
      .withDateTime(DATE_TIME)
      .buildRequest();

    var response = map(request);

    assertNull(response.getNextPageCursor());
    assertNull(response.getPreviousPageCursor());
  }

  private static RoutingResponse map(RouteRequest request) {
    var pagingService = mock(PagingService.class);
    when(pagingService.nextPageCursor()).thenReturn(NEXT_PAGE_CURSOR);
    when(pagingService.previousPageCursor()).thenReturn(PREVIOUS_PAGE_CURSOR);

    return RoutingResponseMapper.map(
      request,
      List.of(),
      Set.of(),
      new DebugTimingAggregator(),
      null,
      pagingService
    );
  }

  private static PageCursor pageCursor(PageType type) {
    return new PageCursor(
      type,
      SortOrder.STREET_AND_ARRIVAL_TIME,
      DATE_TIME,
      null,
      Duration.ofMinutes(30),
      null,
      null
    );
  }
}
