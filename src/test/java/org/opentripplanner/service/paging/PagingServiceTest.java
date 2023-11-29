package org.opentripplanner.service.paging;

import static java.time.ZoneOffset.UTC;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.SearchParams;

class PagingServiceTest {

  static final Instant TRANSIT_TIME_ZERO = TestItineraryBuilder.SERVICE_DAY
    .atStartOfDay(UTC)
    .toInstant();

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

  private static final Itinerary KEPT_ITINERARY = TestItineraryBuilder
    .newItinerary(A, T12_00)
    .bus(1, T12_00, T12_30, B)
    .build();

  private PagingService pagingService() {
    return null;
  }
}
