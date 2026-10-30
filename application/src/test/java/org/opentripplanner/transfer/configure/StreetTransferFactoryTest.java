package org.opentripplanner.transfer.configure;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.stopPattern;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.tripPattern;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transfer.dataimport.StopsVisitedByPatterns;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;

class StreetTransferFactoryTest {

  private static final TransitRepositoryForTest MODEL = TransitRepositoryForTest.of();
  private static final RegularStop SCHEDULED = MODEL.stop("scheduled").build();
  private static final RegularStop OTHER_SCHEDULED = MODEL.stop("other-scheduled").build();
  private static final RegularStop REALTIME_ONLY = MODEL.stop("realtime-only")
    .withSometimesUsedRealtime(true)
    .build();
  private static final RegularStop UNUSED = MODEL.stop("unused").build();

  private final SiteRepository siteRepository = MODEL.siteRepositoryBuilder()
    .withRegularStops(List.of(SCHEDULED, OTHER_SCHEDULED, REALTIME_ONLY, UNUSED))
    .build();
  private final StopsVisitedByPatterns stopPatterns = createStopPatterns();

  @Test
  void aStopVisitedByAScheduledPatternIsATransferSource() {
    assertThat(isSource(SCHEDULED)).isTrue();
  }

  @Test
  void aStopNoPatternVisitsIsNotATransferSource() {
    assertThat(isSource(UNUSED)).isFalse();
  }

  @Test
  void aStopUsedByRealtimeOnlyIsATransferSourceWhenTheFeatureIsOn() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() ->
      assertThat(isSource(REALTIME_ONLY)).isTrue()
    );
  }

  @Test
  void aStopUsedByRealtimeOnlyIsNotATransferSourceWhenTheFeatureIsOff() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOff(() ->
      assertThat(isSource(REALTIME_ONLY)).isFalse()
    );
  }

  @Test
  void anUnusedStopIsNotATransferSourceEvenWhenTheFeatureIsOn() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() ->
      assertThat(isSource(UNUSED)).isFalse()
    );
  }

  private boolean isSource(RegularStop stop) {
    return StreetTransferFactory.isTransferSource(siteRepository, stopPatterns, stop.getId());
  }

  private static StopsVisitedByPatterns createStopPatterns() {
    Trip trip = TransitRepositoryForTest.trip("t1").build();
    var pattern = tripPattern("p1", trip.getRoute())
      .withStopPattern(stopPattern(SCHEDULED, OTHER_SCHEDULED))
      .build();
    var timetableDataImport = new TimetableDataImport();
    timetableDataImport.addTripPattern(pattern.getId(), pattern);
    return new StopsVisitedByPatterns(timetableDataImport);
  }
}
