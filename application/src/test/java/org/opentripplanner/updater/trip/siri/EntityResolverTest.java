package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

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
    var timetableRepository = new TimetableRepository(SITE_REPOSITORY);
    var transitService = new DefaultTransitService(timetableRepository);
    var resolver = new EntityResolver(transitService, FEED_ID);
    var stop = resolver.resolveQuay(STOP_1.getId().getId());
    assertEquals(STOP_1, stop);
  }

  @Test
  void scheduledStopPointTakesPrecedence() {
    var timetableRepository = new TimetableRepository(SITE_REPOSITORY);
    var transitService = new DefaultTransitService(timetableRepository);
    timetableRepository.addScheduledStopPointMapping(Map.of(SSP_ID, STOP_2));
    var resolver = new EntityResolver(transitService, FEED_ID);
    assertEquals(STOP_2, resolver.resolveQuay(SSP_ID.getId()));
    assertEquals(STOP_1, resolver.resolveQuay(STOP_1.getId().getId()));
  }

  /**
   * When the dated service journey is resolved from the EstimatedVehicleJourneyCode, the code must
   * be normalized to a DatedServiceJourney id, matching the id under which {@link AddedTripBuilder}
   * registers the added TripOnServiceDate. A code supplied in ServiceJourney form must be swapped.
   */
  @Test
  void resolveDatedServiceJourneyIdFromServiceJourneyCode() {
    var journey = journey(builder ->
      builder.withEstimatedVehicleJourneyCode("RUT:ServiceJourney:1234")
    );

    assertEquals(
      new FeedScopedId(FEED_ID, "RUT:DatedServiceJourney:1234"),
      newResolver().resolveDatedServiceJourneyId(journey)
    );
  }

  @Test
  void resolveDatedServiceJourneyIdFromDatedServiceJourneyCode() {
    var journey = journey(builder ->
      builder.withEstimatedVehicleJourneyCode("RUT:DatedServiceJourney:1234")
    );

    assertEquals(
      new FeedScopedId(FEED_ID, "RUT:DatedServiceJourney:1234"),
      newResolver().resolveDatedServiceJourneyId(journey)
    );
  }

  /**
   * A DatedVehicleJourneyRef takes precedence over the EstimatedVehicleJourneyCode and is resolved
   * verbatim, without entity-type normalization.
   */
  @Test
  void resolveDatedServiceJourneyIdFromDatedVehicleJourneyRef() {
    var journey = journey(builder ->
      builder
        .withDatedVehicleJourneyRef("RUT:DatedServiceJourney:5678")
        .withEstimatedVehicleJourneyCode("RUT:ServiceJourney:1234")
    );

    assertEquals(
      new FeedScopedId(FEED_ID, "RUT:DatedServiceJourney:5678"),
      newResolver().resolveDatedServiceJourneyId(journey)
    );
  }

  private static EntityResolver newResolver() {
    var transitService = new DefaultTransitService(new TimetableRepository());
    return new EntityResolver(transitService, FEED_ID);
  }

  private static EstimatedVehicleJourneyWrapper journey(UnaryOperator<SiriEtBuilder> configure) {
    var timeParser = new LocalTimeParser(ZoneId.of("Europe/Oslo"), LocalDate.of(2026, 6, 29));
    return EstimatedVehicleJourneyWrapper.of(
      configure.apply(new SiriEtBuilder(timeParser)).buildEstimatedVehicleJourney()
    );
  }
}
