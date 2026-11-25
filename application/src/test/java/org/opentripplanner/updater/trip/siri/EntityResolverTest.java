package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitDataTestFactory;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;

class EntityResolverTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_1 = TEST_MODEL.stop("stop-1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("stop-2").build();
  private static final SiteRepository SITE_REPOSITORY = TEST_MODEL.siteRepositoryBuilder()
    .withRegularStops(List.of(STOP_1, STOP_2))
    .build();
  private static final String FEED_ID = STOP_1.getId().getFeedId();
  private static final FeedScopedId SSP_ID = new FeedScopedId(FEED_ID, "ssp-1");
  private static final Operator OPERATOR = Operator.of(new FeedScopedId(FEED_ID, "operator-1"))
    .withName("Operator 1")
    .build();

  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Oslo");
  private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 11, 25);
  private static final LocalDate NEXT_DAY = SERVICE_DATE.plusDays(1);
  private static final Trip REPLACED_TRIP = TransitRepositoryForTest.trip(
    FEED_ID,
    "trip-1"
  ).build();
  private static final String REPLACED_DATED_JOURNEY_ID = "replaced-journey";
  private static final TripOnServiceDate REPLACED_TRIP_ON_SERVICE_DATE = TripOnServiceDate.of(
    new FeedScopedId(FEED_ID, REPLACED_DATED_JOURNEY_ID)
  )
    .withTrip(REPLACED_TRIP)
    .withServiceDate(SERVICE_DATE)
    .build();

  @Test
  void resolveScheduledStopPointId() {
    var transitRepository = new TransitRepository();
    transitRepository.addScheduledStopPointMapping(Map.of(SSP_ID, STOP_1));
    var resolver = newResolver(transitRepository);
    var stop = resolver.resolveQuay(SSP_ID.getId());
    assertEquals(STOP_1, stop);
  }

  @Test
  void resolveQuayId() {
    var transitRepository = new TransitRepository(SITE_REPOSITORY);
    var resolver = newResolver(transitRepository);
    var stop = resolver.resolveQuay(STOP_1.getId().getId());
    assertEquals(STOP_1, stop);
  }

  @Test
  void scheduledStopPointTakesPrecedence() {
    var transitRepository = new TransitRepository(SITE_REPOSITORY);
    transitRepository.addScheduledStopPointMapping(Map.of(SSP_ID, STOP_2));
    var resolver = newResolver(transitRepository);
    assertEquals(STOP_2, resolver.resolveQuay(SSP_ID.getId()));
    assertEquals(STOP_1, resolver.resolveQuay(STOP_1.getId().getId()));
  }

  @Test
  void resolveOperator() {
    var transitRepository = new TransitRepository();
    transitRepository.addOperators(List.of(OPERATOR));
    transitRepository.index();
    var resolver = newResolver(transitRepository);

    assertEquals(OPERATOR, resolver.resolveOperator(OPERATOR.getId().getId()));
    assertNull(resolver.resolveOperator("unknown-operator"));
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

  @Test
  void extraJourneyGetsServiceDateOfReplacedJourney() {
    var journey = journeyDepartingAfterMidnight(builder ->
      builder.withIsExtraJourney(true).withVehicleJourneyRef(REPLACED_DATED_JOURNEY_ID)
    );

    assertEquals(SERVICE_DATE, resolverWithReplacedJourney().resolveServiceDate(journey));
  }

  @Test
  void extraJourneyGetsServiceDateOfFirstAdditionalReplacedJourneyThatExists() {
    var evj = journeyBuilderDepartingAfterMidnight(builder ->
      builder.withIsExtraJourney(true).withVehicleJourneyRef("unknown")
    ).buildEstimatedVehicleJourney();
    evj.getAdditionalVehicleJourneyReves().add(framedRef("unknown", NEXT_DAY));
    evj
      .getAdditionalVehicleJourneyReves()
      .add(framedRef(REPLACED_TRIP.getId().getId(), SERVICE_DATE));

    assertEquals(
      SERVICE_DATE,
      resolverWithReplacedJourney().resolveServiceDate(EstimatedVehicleJourneyWrapper.of(evj))
    );
  }

  @Test
  void extraJourneyWithoutKnownReplacedJourneyGetsDateOfDeparture() {
    var journey = journeyDepartingAfterMidnight(builder ->
      builder.withIsExtraJourney(true).withVehicleJourneyRef("unknown")
    );

    assertEquals(NEXT_DAY, resolverWithReplacedJourney().resolveServiceDate(journey));
  }

  @Test
  void framedVehicleJourneyRefTakesPrecedenceOverReplacedJourney() {
    var framedDate = SERVICE_DATE.minusDays(1);
    var journey = journeyDepartingAfterMidnight(builder ->
      builder
        .withIsExtraJourney(true)
        .withVehicleJourneyRef(REPLACED_DATED_JOURNEY_ID)
        .withFramedVehicleJourneyRef("framed", framedDate)
    );

    assertEquals(framedDate, resolverWithReplacedJourney().resolveServiceDate(journey));
  }

  @Test
  void journeyThatIsNotExtraIgnoresReplacedJourney() {
    var journey = journeyDepartingAfterMidnight(builder ->
      builder.withVehicleJourneyRef(REPLACED_DATED_JOURNEY_ID)
    );

    assertEquals(NEXT_DAY, resolverWithReplacedJourney().resolveServiceDate(journey));
  }

  private static EntityResolver newResolver() {
    return newResolver(new TransitRepository());
  }

  private static EntityResolver newResolver(TransitRepository transitRepository) {
    var timetableDataImport = new TimetableDataImport();
    var scheduledTimetableData = timetableDataImport.toScheduledTimetableData();
    var timetableRepository = new DefaultTimetableRepository(
      RaptorTransitDataTestFactory.empty(),
      scheduledTimetableData
    );
    return new EntityResolver(timetableRepository, transitRepository, FEED_ID);
  }

  private static EntityResolver resolverWithReplacedJourney() {
    var timetableDataImport = new TimetableDataImport();
    timetableDataImport.addTripOnServiceDate(REPLACED_TRIP_ON_SERVICE_DATE);
    var timetableRepository = new DefaultTimetableRepository(
      RaptorTransitDataTestFactory.empty(),
      timetableDataImport.toScheduledTimetableData()
    );
    return new EntityResolver(timetableRepository, new TransitRepository(), FEED_ID);
  }

  /** A journey whose first call departs at 00:08 on the day after {@link #SERVICE_DATE}. */
  private static EstimatedVehicleJourneyWrapper journeyDepartingAfterMidnight(
    UnaryOperator<SiriEtBuilder> configure
  ) {
    return EstimatedVehicleJourneyWrapper.of(
      journeyBuilderDepartingAfterMidnight(configure).buildEstimatedVehicleJourney()
    );
  }

  private static SiriEtBuilder journeyBuilderDepartingAfterMidnight(
    UnaryOperator<SiriEtBuilder> configure
  ) {
    var builder = new SiriEtBuilder(new LocalTimeParser(TIME_ZONE, NEXT_DAY)).withEstimatedCalls(
      calls ->
        calls
          .call(STOP_1)
          .departAimedExpected("00:08", "00:08")
          .call(STOP_2)
          .arriveAimedExpected("00:25", "00:25")
    );
    return configure.apply(builder);
  }

  private static FramedVehicleJourneyRefStructure framedRef(String journeyId, LocalDate date) {
    return new SiriEtBuilder.FramedVehicleRefBuilder()
      .withVehicleJourneyRef(journeyId)
      .withServiceDate(date)
      .build();
  }

  private static EstimatedVehicleJourneyWrapper journey(UnaryOperator<SiriEtBuilder> configure) {
    var timeParser = new LocalTimeParser(ZoneId.of("Europe/Oslo"), LocalDate.of(2026, 6, 29));
    return EstimatedVehicleJourneyWrapper.of(
      configure.apply(new SiriEtBuilder(timeParser)).buildEstimatedVehicleJourney()
    );
  }
}
