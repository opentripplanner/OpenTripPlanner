package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.cancelledJourney;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithAllButOneCallCancelled;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithMovedDestination;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.malformedNonCancelledJourney;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.CarpoolingParametersTestData;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTripResolver;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;

class SiriETCarpoolingUpdaterTest {

  private static final String FEED_ID = "EN";

  /** A SIRI-ET delivery without journeys that claims more data follows. */
  private static final String PAGE_WITH_MORE_DATA = """
  <Siri xmlns="http://www.siri.org.uk/siri" version="2.1">
    <ServiceDelivery>
      <ResponseTimestamp>%s</ResponseTimestamp>
      <MoreData>true</MoreData>
    </ServiceDelivery>
  </Siri>
  """;

  private DefaultCarpoolingRepository repository;
  private SiriETCarpoolingUpdater updater;
  private RoutableCarpoolTripResolver resolver;
  private final CarpoolSiriMapper mapper = new CarpoolSiriMapper(
    FEED_ID,
    CarpoolingParameters.DEFAULT.maxTripDuration()
  );

  @BeforeEach
  void setUp() {
    repository = new DefaultCarpoolingRepository(
      CarpoolingParameters.DEFAULT.expirySweepInterval()
    );
    resolver = mock(RoutableCarpoolTripResolver.class);
    when(resolver.resolve(any())).thenAnswer(invocation ->
      RoutableCarpoolTripTestData.withDummyVertices(invocation.getArgument(0))
    );
    updater = new SiriETCarpoolingUpdater(
      params("http://localhost/never-fetched"),
      repository,
      resolver,
      Runnable::run,
      CarpoolingParameters.DEFAULT
    );
  }

  @Test
  void processEstimatedVehicleJourney_active_upsertsTrip() {
    var journey = minimalCompleteJourney();

    updater.processEstimatedVehicleJourney(journey);

    assertTrue(tripIsInRepository(mapper.tripId(journey)));
  }

  @Test
  void processEstimatedVehicleJourney_wholeTripCancellation_removesTrip() {
    var tripId = seedActiveTrip();

    updater.processEstimatedVehicleJourney(cancelledJourney());

    assertFalse(tripIsInRepository(tripId));
  }

  @Test
  void processEstimatedVehicleJourney_fewerThanTwoActiveCalls_removesTrip() {
    var tripId = seedActiveTrip();

    updater.processEstimatedVehicleJourney(journeyWithAllButOneCallCancelled());

    assertFalse(tripIsInRepository(tripId));
  }

  @Test
  void processEstimatedVehicleJourney_cancellationForUnknownTrip_isNoOp() {
    updater.processEstimatedVehicleJourney(cancelledJourney());

    assertTrue(repository.getCarpoolTrips().isEmpty());
  }

  @Test
  void processEstimatedVehicleJourney_unresolvableRoutePoint_dropsTrip() {
    doReturn(null).when(resolver).resolve(any());

    updater.processEstimatedVehicleJourney(minimalCompleteJourney());

    assertTrue(repository.getCarpoolTrips().isEmpty());
  }

  @Test
  void processEstimatedVehicleJourney_unresolvableGeometryRedelivered_resolvesOnlyOnce() {
    doReturn(null).when(resolver).resolve(any());

    // Same geometry, fresh times: a re-delivery on an expected-time update must not re-resolve a
    // failed geometry.
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());

    verify(resolver, times(1)).resolve(any());
    assertTrue(repository.getCarpoolTrips().isEmpty());
  }

  @Test
  void processEstimatedVehicleJourney_unresolvableGeometryChanged_retriesResolution() {
    doReturn(null).when(resolver).resolve(any());
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());

    doAnswer(invocation -> RoutableCarpoolTripTestData.withDummyVertices(invocation.getArgument(0)))
      .when(resolver)
      .resolve(any());
    var changedJourney = journeyWithMovedDestination();
    updater.processEstimatedVehicleJourney(changedJourney);

    verify(resolver, times(2)).resolve(any());
    assertTrue(tripIsInRepository(mapper.tripId(changedJourney)));
  }

  @Test
  void processEstimatedVehicleJourney_resolutionThrows_dropsStaleTrip() {
    var tripId = seedActiveTrip();

    doThrow(new RuntimeException("resolution blew up")).when(resolver).resolve(any());
    // Changed geometry forces re-resolution; when it throws, the stale stored trip must be dropped.
    updater.processEstimatedVehicleJourney(journeyWithMovedDestination());

    assertFalse(tripIsInRepository(tripId));
  }

  @Test
  void processEstimatedVehicleJourney_resolutionThrowsRedelivered_resolvesOnlyOnce() {
    doThrow(new RuntimeException("resolution blew up")).when(resolver).resolve(any());

    // A thrown resolution is memoized like a null one: re-delivering the same geometry must not
    // re-resolve.
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());

    verify(resolver, times(1)).resolve(any());
    assertTrue(repository.getCarpoolTrips().isEmpty());
  }

  @Test
  void processEstimatedVehicleJourney_unchangedGeometry_reusesStoredVertices() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    var storedVertices = repository.getCarpoolTrips().iterator().next().vertices();

    updater.processEstimatedVehicleJourney(journey);

    verify(resolver, times(1)).resolve(any());
    assertEquals(storedVertices, repository.getCarpoolTrips().iterator().next().vertices());
  }

  @Test
  void processEstimatedVehicleJourney_malformedNonCancellation_doesNotRemoveOrReplaceExistingTrip() {
    // Sanity-check the fixture: a direct mapper call must throw, otherwise the updater test
    // below would silently degrade into "upsert replaces the seeded trip" and still pass.
    assertThrows(Exception.class, () ->
      mapper.mapSiriToCarpoolTrip(malformedNonCancelledJourney())
    );

    seedActiveTrip();
    var seededTrip = repository.getCarpoolTrips().iterator().next();

    updater.processEstimatedVehicleJourney(malformedNonCancelledJourney());

    assertSame(seededTrip, repository.getCarpoolTrips().iterator().next());
  }

  private FeedScopedId seedActiveTrip() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    var tripId = mapper.tripId(journey);
    assertTrue(tripIsInRepository(tripId));
    return tripId;
  }

  private boolean tripIsInRepository(FeedScopedId id) {
    return repository
      .getCarpoolTrips()
      .stream()
      .anyMatch(t -> t.trip().getId().equals(id));
  }

  @Test
  void aPollReadsAtMostTheMaximumNumberOfPages(@TempDir Path feed) throws IOException {
    // Three deliveries, each claiming that more data follows.
    var page = PAGE_WITH_MORE_DATA.formatted(Instant.now());
    for (int i = 1; i <= 3; i++) {
      Files.writeString(feed.resolve("page-" + i + ".xml"), page);
    }
    var cappedUpdater = new SiriETCarpoolingUpdater(
      params(feed.toUri().toString()),
      repository,
      resolver,
      Runnable::run,
      CarpoolingParametersTestData.withMaxPagesPerPoll(2)
    );

    cappedUpdater.runPolling();

    try (var unread = Files.list(feed)) {
      assertEquals(1, unread.filter(f -> f.toString().endsWith(".xml")).count());
    }
  }

  private static DefaultSiriETUpdaterParameters params(String url) {
    return new DefaultSiriETUpdaterParameters(
      "carpool-test",
      FEED_ID,
      false,
      url,
      Duration.ofMinutes(1),
      "test-requestor",
      Duration.ofSeconds(30),
      Duration.ofMinutes(15),
      false,
      HttpHeaders.empty(),
      false
    );
  }
}
