package org.opentripplanner.service.paging;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.PagingSearchWindowAdjuster;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageCursorFactory;
import org.opentripplanner.model.plan.paging.cursor.PageType;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.NumItinerariesFilterResults;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class PagingService {

  private final Instant searchStartTime;
  private final Duration searchWindowUsed;
  private final Instant earliestDepartureTime;
  private final Instant latestArrivalTime;
  private final RouteRequest request;
  private final NumItinerariesFilterResults numItinerariesFilterResults;
  private final PagingSearchWindowAdjuster searchWindowAdjuster;
  private final List<Itinerary> itineraries;

  // Lazy init
  private PageCursorFactory pageCursorFactory = null;

  public PagingService(
    List<Duration> pagingSearchWindowAdjustments,
    Duration minSearchWindowSize,
    Duration maxSearchWindowSize,
    Instant searchStartTime,
    Duration searchWindowUsed,
    Instant earliestDepartureTime,
    Instant latestArrivalTime,
    RouteRequest request,
    NumItinerariesFilterResults numItinerariesFilterResults,
    List<Itinerary> itineraries
  ) {
    this.searchStartTime = searchStartTime;
    this.request = request;
    this.searchWindowUsed = searchWindowUsed;
    this.earliestDepartureTime = earliestDepartureTime;
    this.latestArrivalTime = latestArrivalTime;
    this.numItinerariesFilterResults = numItinerariesFilterResults;
    this.itineraries = itineraries;
    this.searchWindowAdjuster =
      createSearchWindowAdjuster(
        pagingSearchWindowAdjustments,
        minSearchWindowSize,
        maxSearchWindowSize
      );
  }

  public PagingService(
    Instant searchStartTime,
    TransitTuningParameters transitTuningParameters,
    RaptorTuningParameters raptorTuningParameters,
    RouteRequest request,
    SearchParams raptorSearchParamsUsed,
    NumItinerariesFilterResults numItinerariesFilterResults,
    List<Itinerary> itineraries
  ) {
    this(
      transitTuningParameters.pagingSearchWindowAdjustments(),
      raptorTuningParameters.dynamicSearchWindowCoefficients().minWindow(),
      raptorTuningParameters.dynamicSearchWindowCoefficients().maxWindow(),
      searchStartTime,
      searchWindowOf(raptorSearchParamsUsed),
      edt(searchStartTime, raptorSearchParamsUsed),
      lat(searchStartTime, raptorSearchParamsUsed),
      request,
      numItinerariesFilterResults,
      itineraries
    );
  }

  public PageCursor nextPageCursor() {
    return pageCursorFactory().nextPageCursor();
  }

  public PageCursor previousPageCursor() {
    return pageCursorFactory().previousPageCursor();
  }

  private PageCursorFactory pageCursorFactory() {
    if (pageCursorFactory == null) {
      Duration searchWindowForNextSearch = calculateSearchWindowNextSearch();

      this.pageCursorFactory =
        mapIntoPageCursorFactory(
          request.itinerariesSortOrder(),
          searchWindowForNextSearch,
          request.pageCursor() == null ? null : request.pageCursor().type()
        );
    }
    return pageCursorFactory;
  }

  @Nullable
  public TripSearchMetadata createTripSearchMetadata() {
    if (searchWindowUsed == null) {
      return null;
    }

    Instant reqTime = request.dateTime();

    if (request.arriveBy()) {
      return TripSearchMetadata.createForArriveBy(
        reqTime,
        searchWindowUsed,
        firstRemovedArrivalTime()
      );
    } else {
      return TripSearchMetadata.createForDepartAfter(
        reqTime,
        searchWindowUsed,
        firstRemovedDepartureTime()
      );
    }
  }

  private Duration calculateSearchWindowNextSearch() {
    // No transit search performed
    if (searchWindowUsed == null) {
      return null;
    }

    // SearchWindow cropped -> decrease search-window
    if (numItinerariesFilterResults != null) {
      boolean cropSWHead = doCropSearchWindowAtTail(
        request.pageCursor(),
        request.itinerariesSortOrder()
      );
      Instant rmItineraryStartTime = numItinerariesFilterResults
        .firstRemoved()
        .startTimeAsInstant();

      return searchWindowAdjuster.decreaseSearchWindow(
        searchWindowUsed,
        earliestDepartureTime,
        rmItineraryStartTime,
        cropSWHead
      );
    }
    // (num-of-itineraries found <= numItineraries)  ->  increase or keep search-window
    else {
      int nRequested = request.numItineraries();
      int nFound = (int) itineraries
        .stream()
        .filter(it -> !it.isFlaggedForDeletion() && it.hasTransit())
        .count();

      return searchWindowAdjuster.increaseOrKeepSearchWindow(searchWindowUsed, nRequested, nFound);
    }
  }

  public Instant firstRemovedDepartureTime() {
    return numItinerariesFilterResults == null
      ? null
      : numItinerariesFilterResults.firstRemoved().startTimeAsInstant();
  }

  public Instant firstRemovedArrivalTime() {
    return numItinerariesFilterResults == null
      ? null
      : numItinerariesFilterResults.firstRemoved().endTimeAsInstant();
  }

  private PagingSearchWindowAdjuster createSearchWindowAdjuster(
    List<Duration> pagingSearchWindowAdjustments,
    Duration minSearchWindowSize,
    Duration maxSearchWindowSize
  ) {
    return new PagingSearchWindowAdjuster(
      minSearchWindowSize,
      maxSearchWindowSize,
      pagingSearchWindowAdjustments
    );
  }

  /**
   * Related to {@link RouteRequest#cropItinerariesAt()}, but is {@code true} if we should crop the
   * search-window head(in the beginning) or tail(in the end).
   * <p>
   * For the first search we look if the sort is ascending(crop tail) or descending(crop head), and
   * for paged results we look at the paging type: next(tail) and previous(head).
   */
  private boolean doCropSearchWindowAtTail(
    @Nullable PageCursor pageCursor,
    SortOrder itinerariesSortOrder
  ) {
    if (pageCursor == null) {
      return itinerariesSortOrder.isSortedByAscendingArrivalTime();
    }
    return pageCursor.type().isNext();
  }

  PageCursorFactory mapIntoPageCursorFactory(
    SortOrder sortOrder,
    Duration searchWindowNextSearch,
    @Nullable PageType currentPageType
  ) {
    Objects.requireNonNull(sortOrder);
    Objects.requireNonNull(searchStartTime);

    PageCursorFactory factory = new PageCursorFactory(sortOrder, searchWindowNextSearch);

    // No transit search performed
    if (noTransitSearchPerformed()) {
      return factory;
    }

    assertRequestPrerequisites();

    factory =
      factory.withOriginalSearch(
        currentPageType,
        earliestDepartureTime,
        latestArrivalTime,
        searchWindowUsed
      );

    if (numItinerariesFilterResults != null) {
      factory = factory.withRemovedItineraries(numItinerariesFilterResults);
    }
    return factory;
  }

  private void assertRequestPrerequisites() {
    if (searchWindowUsed == null) {
      throw new IllegalStateException("SearchWindow not set");
    }
    if (earliestDepartureTime == null) {
      throw new IllegalStateException("Earliest departure time not set");
    }
  }

  private boolean noTransitSearchPerformed() {
    return searchWindowUsed == null;
  }

  static Duration searchWindowOf(SearchParams searchParamsUsed) {
    return searchParamsUsed == null
      ? null
      : Duration.ofSeconds(searchParamsUsed.searchWindowInSeconds());
  }

  static Instant edt(Instant transitSearchStartTime, SearchParams searchParamsUsed) {
    if (searchParamsUsed == null || !searchParamsUsed.isEarliestDepartureTimeSet()) {
      return null;
    }
    return transitSearchStartTime.plusSeconds(searchParamsUsed.earliestDepartureTime());
  }

  static Instant lat(Instant transitSearchStartTime, SearchParams searchParamsUsed) {
    if (searchParamsUsed == null || !searchParamsUsed.isLatestArrivalTimeSet()) {
      return null;
    }
    return transitSearchStartTime.plusSeconds(searchParamsUsed.latestArrivalTime());
  }
}
