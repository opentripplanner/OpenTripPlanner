package org.opentripplanner.service.paging;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner._support.debug.TestDebug;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.OutsideSearchWindowFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.PagingFilter;
import org.opentripplanner.routing.algorithm.filterchain.paging.DefaultPageCursorInput;
import org.opentripplanner.utils.collection.ListSection;
import org.opentripplanner.utils.lang.Box;

/**
 * This class simulate/mock the context the paging is operating in.
 */
final class TestDriver {

  private final int nResults;
  private final Duration searchWindow;
  private final List<Itinerary> all;
  private final List<Itinerary> kept;
  private final Instant edt;
  private final Instant lat;
  private final SortOrder sortOrder;
  private final ListSection cropSection;
  private final PageCursorInput results;

  public TestDriver(
    int nResults,
    Duration searchWindow,
    List<Itinerary> all,
    List<Itinerary> kept,
    Instant edt,
    Instant lat,
    SortOrder sortOrder,
    ListSection cropSection,
    PageCursorInput results
  ) {
    this.nResults = nResults;
    this.searchWindow = searchWindow;
    this.all = all;
    this.kept = kept;
    this.edt = edt;
    this.lat = lat;
    this.sortOrder = sortOrder;
    this.cropSection = cropSection;
    this.results = results;
    debug();
  }

  static TestDriver driver(
    int edt,
    int lat,
    Duration searchWindow,
    int nResults,
    SortOrder sortOrder,
    List<Itinerary> all
  ) {
    return createNewDriver(
      TestPagingModel.time(edt),
      TestPagingModel.time(lat),
      searchWindow,
      nResults,
      sortOrder,
      all,
      ListSection.TAIL,
      null
    );
  }

  int nResults() {
    return nResults;
  }

  public Duration searchWindow() {
    return searchWindow;
  }

  List<Itinerary> kept() {
    return kept;
  }

  List<Itinerary> all() {
    return all;
  }

  Instant earliestDepartureTime() {
    return edt;
  }

  Instant latestArrivalTime() {
    return lat;
  }

  SortOrder sortOrder() {
    return sortOrder;
  }

  boolean arrivedBy() {
    return !sortOrder.isSortedByAscendingArrivalTime();
  }

  PageCursorInput filterResults() {
    return results;
  }

  ItinerarySortKey expectedCut() {
    return results == null ? null : results.pageCut();
  }

  TestDriver newPage(PageCursor cursor) {
    return createNewDriver(
      cursor.earliestDepartureTime(),
      cursor.latestArrivalTime(),
      searchWindow,
      nResults,
      sortOrder,
      all,
      cursor.cropItinerariesAt(),
      cursor.itineraryPageCut()
    );
  }

  PagingService pagingService() {
    return TestPagingModel.pagingService(this);
  }

  PagingService pagingService(PageCursor cursor) {
    return TestPagingModel.pagingService(this, cursor);
  }

  private static TestDriver createNewDriver(
    Instant edt,
    Instant lat,
    Duration searchWindow,
    int nResults,
    SortOrder sortOrder,
    List<Itinerary> all,
    ListSection cropItineraries,
    @Nullable ItinerarySortKey pageCut
  ) {
    List<Itinerary> kept = all;

    // Filter search-window
    var swFilter = new OutsideSearchWindowFilter(edt, searchWindow);
    kept = swFilter.removeMatchesForTest(kept);

    // Simulate Raptor - apply LAT filtering done by raptor
    if (lat != null) {
      kept = kept.stream().filter(it -> !lat.isBefore(it.endTime().toInstant())).toList();
    }

    //Page filter
    if (pageCut != null) {
      var pageFilter = new PagingFilter(sortOrder, cropItineraries.invert(), pageCut);
      kept = pageFilter.removeMatchesForTest(kept);
    }

    // Filter nResults
    var filterResultBox = new Box<PageCursorInput>();
    var maxNumFilter = new NumItinerariesFilter(nResults, cropItineraries);
    kept = maxNumFilter.removeMatchesForTest(kept);
    DefaultPageCursorInput.Builder pageCursorInputBuilder = DefaultPageCursorInput.of();
    if (maxNumFilter.getNumItinerariesFilterResult() != null) {
      pageCursorInputBuilder = pageCursorInputBuilder
        .withEarliestRemovedDeparture(
          maxNumFilter.getNumItinerariesFilterResult().earliestRemovedDeparture()
        )
        .withLatestRemovedDeparture(
          maxNumFilter.getNumItinerariesFilterResult().latestRemovedDeparture()
        )
        .withPageCut(maxNumFilter.getNumItinerariesFilterResult().pageCut());
    }
    filterResultBox.set(pageCursorInputBuilder.build());

    return new TestDriver(
      nResults,
      searchWindow,
      all,
      kept,
      edt,
      lat,
      sortOrder,
      ListSection.TAIL,
      filterResultBox.get()
    );
  }

  private void debug() {
    if (TestDebug.off()) {
      return;
    }
    TestDebug.println();
    TestDebug.println("ITINERARIES:");
    all.forEach(it -> {
      var value = TestPagingUtils.toString(it);
      if (kept.contains(it)) {
        value += " *";
      }
      TestDebug.println(value);
    });
  }
}
