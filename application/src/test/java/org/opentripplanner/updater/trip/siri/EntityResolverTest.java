package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.trip.siri.EntityResolver;

class EntityResolverTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final RegularStop STOP_1 = TEST_MODEL.stop("stop-1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("stop-2").build();
  private static final SiteRepository SITE_REPOSITORY = TEST_MODEL.siteRepositoryBuilder()
    .withRegularStops(List.of(STOP_1, STOP_2))
    .build();
  private static final String FEED_ID = STOP_1.getId().getFeedId();
  private static final FeedScopedId SSP_ID = new FeedScopedId(FEED_ID, "ssp-1");

  @Test
  void resolveScheduledStopPointId() {
    var timetableRepository = new TimetableRepository();
    timetableRepository.addScheduledStopPointMapping(Map.of(SSP_ID, STOP_1));
    var transitService = new DefaultTransitService(timetableRepository);
    var resolver = new EntityResolver(transitService, FEED_ID);
    var stop = resolver.resolveQuay(SSP_ID.getId());
    assertEquals(STOP_1, stop);
  }

  @Test
  void resolveQuayId() {
    var timetableRepository = new TimetableRepository(SITE_REPOSITORY, new Deduplicator());
    var transitService = new DefaultTransitService(timetableRepository);
    var resolver = new EntityResolver(transitService, FEED_ID);
    var stop = resolver.resolveQuay(STOP_1.getId().getId());
    assertEquals(STOP_1, stop);
  }

  @Test
  void scheduledStopPointTakesPrecedence() {
    var timetableRepository = new TimetableRepository(SITE_REPOSITORY, new Deduplicator());
    var transitService = new DefaultTransitService(timetableRepository);
    timetableRepository.addScheduledStopPointMapping(Map.of(SSP_ID, STOP_2));
    var resolver = new EntityResolver(transitService, FEED_ID);
    assertEquals(STOP_2, resolver.resolveQuay(SSP_ID.getId()));
    assertEquals(STOP_1, resolver.resolveQuay(STOP_1.getId().getId()));
  }
}
