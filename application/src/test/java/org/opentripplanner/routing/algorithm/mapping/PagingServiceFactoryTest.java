package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

class PagingServiceFactoryTest {

  private static final Instant TRANSIT_START_TIME = Instant.parse("2023-11-11T00:00:00Z");
  private static final int EDT = TimeUtils.time("10:00");
  private static final int LAT = TimeUtils.time("11:30");

  private static final Instant EXP_EDT = Instant.parse("2023-11-11T10:00:00Z");
  private static final Instant EXP_LAT = Instant.parse("2023-11-11T11:30:00Z");

  private static final Duration SEARCH_WINDOW = DurationUtils.duration("2h30m");
  private static final SearchParams SEARCH_PARAMS_DEPART_AFTER = new RaptorRequestBuilder<>()
    .searchParams()
    .earliestDepartureTime(EDT)
    .addAccessPaths(TestAccessEgress.walk(1, 1))
    .addEgressPaths(TestAccessEgress.walk(1, 1))
    .build()
    .searchParams();
  private static final SearchParams SEARCH_PARAMS_ARRIVE_BY = new RaptorRequestBuilder<>()
    .searchParams()
    .latestArrivalTime(LAT)
    .addAccessPaths(TestAccessEgress.walk(1, 1))
    .addEgressPaths(TestAccessEgress.walk(1, 1))
    .build()
    .searchParams();
  private static final SearchParams SEARCH_PARAMS_ALL = new RaptorRequestBuilder<>()
    .searchParams()
    .earliestDepartureTime(EDT)
    .latestArrivalTime(LAT)
    .searchWindow(SEARCH_WINDOW)
    .addAccessPaths(TestAccessEgress.walk(1, 1))
    .addEgressPaths(TestAccessEgress.walk(1, 1))
    .build()
    .searchParams();

  @Test
  void createPagingService() {
    var subject = PagingServiceFactory.createPagingService(
      TRANSIT_START_TIME,
      TransitTuningParameters.FOR_TEST,
      new RaptorTuningParameters() {},
      RouteRequest.defaultValue(),
      SEARCH_PARAMS_ALL,
      null,
      List.of()
    );
    assertEquals(
      "PagingService{" +
      "searchWindowUsed: 2h30m, " +
      "earliestDepartureTime: 2023-11-11T10:00:00Z, " +
      "latestArrivalTime: 2023-11-11T11:30:00Z, " +
      "itinerariesSortOrder: STREET_AND_ARRIVAL_TIME, " +
      "numberOfItineraries: 50" +
      "}",
      subject.toString()
    );
  }

  @Test
  void searchWindowOf() {
    assertNull(PagingServiceFactory.searchWindowOf(null));
    assertNull(PagingServiceFactory.searchWindowOf(SEARCH_PARAMS_DEPART_AFTER));
    assertNull(PagingServiceFactory.searchWindowOf(SEARCH_PARAMS_ARRIVE_BY));
    assertEquals(SEARCH_WINDOW, PagingServiceFactory.searchWindowOf(SEARCH_PARAMS_ALL));
  }

  @Test
  void testEarliestDepartureTime() {
    assertNull(PagingServiceFactory.edt(TRANSIT_START_TIME, null));
    assertNull(PagingServiceFactory.edt(TRANSIT_START_TIME, SEARCH_PARAMS_ARRIVE_BY));
    assertEquals(EXP_EDT, PagingServiceFactory.edt(TRANSIT_START_TIME, SEARCH_PARAMS_DEPART_AFTER));
    assertEquals(EXP_EDT, PagingServiceFactory.edt(TRANSIT_START_TIME, SEARCH_PARAMS_ALL));
  }

  @Test
  void testLatestDepartureTime() {
    assertNull(PagingServiceFactory.lat(TRANSIT_START_TIME, null));
    assertNull(PagingServiceFactory.lat(TRANSIT_START_TIME, SEARCH_PARAMS_DEPART_AFTER));
    assertEquals(EXP_LAT, PagingServiceFactory.lat(TRANSIT_START_TIME, SEARCH_PARAMS_ARRIVE_BY));
    assertEquals(EXP_LAT, PagingServiceFactory.lat(TRANSIT_START_TIME, SEARCH_PARAMS_ALL));
  }
}
