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
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithCode;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithLatestExpectedArrivalTime;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithMovedDestination;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.malformedNonCancelledJourney;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.CarpoolingParametersTestData;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTripResolver;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import uk.org.siri.siri21.EstimatedVehicleJourney;

class SiriETCarpoolingUpdaterTest {

  private static final String FEED_ID = "EN";

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
      params(),
      repository,
      resolver,
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

    // Same geometry, later times: a re-delivery on an expected-time update must not re-resolve a
    // failed geometry.
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());
    updater.processEstimatedVehicleJourney(arrivingLater(minimalCompleteJourney()));

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
  void processEstimatedVehicleJourney_corridorRecomputationThrows_dropsStaleTrip() {
    updater.processEstimatedVehicleJourney(journeyWithLatestExpectedArrivalTime(0, 10));

    doThrow(new RuntimeException("corridor blew up"))
      .when(resolver)
      .resolveOnVertices(any(), any());
    // Same geometry, a wider budget: the corridor is recomputed on the stored vertices, and when
    // that throws, the stored trip with the old budget must be dropped.
    var widened = journeyWithLatestExpectedArrivalTime(0, 20);
    updater.processEstimatedVehicleJourney(widened);

    assertFalse(tripIsInRepository(mapper.tripId(widened)));
  }

  @Test
  void processEstimatedVehicleJourney_resolutionThrowsRedelivered_resolvesOnlyOnce() {
    doThrow(new RuntimeException("resolution blew up")).when(resolver).resolve(any());

    // A thrown resolution is memoized like a null one: re-delivering the same geometry, with later
    // times, must not re-resolve.
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());
    updater.processEstimatedVehicleJourney(arrivingLater(minimalCompleteJourney()));

    verify(resolver, times(1)).resolve(any());
    assertTrue(repository.getCarpoolTrips().isEmpty());
  }

  @Test
  void processEstimatedVehicleJourney_anotherTripOnTheSameRoute_reusesItsCorridor() {
    updater.processEstimatedVehicleJourney(minimalCompleteJourney());
    var otherTrip = minimalCompleteJourney();
    otherTrip.setEstimatedVehicleJourneyCode("other");

    updater.processEstimatedVehicleJourney(otherTrip);

    verify(resolver, times(1)).resolve(any());
    assertEquals(2, repository.getCarpoolTrips().size());
  }

  @Test
  void processEstimatedVehicleJourney_unchangedGeometry_reusesStoredVertices() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    var storedVertices = repository.getCarpoolTrips().iterator().next().vertices();

    // Same geometry, later times: a changed journey, which is not dropped as a re-delivery.
    updater.processEstimatedVehicleJourney(arrivingLater(minimalCompleteJourney()));

    verify(resolver, times(1)).resolve(any());
    assertEquals(storedVertices, repository.getCarpoolTrips().iterator().next().vertices());
  }

  @Test
  void processEstimatedVehicleJourney_unchangedRedelivery_keepsStoredTrip() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    var storedTrip = repository.getCarpoolTrips().iterator().next();

    updater.processEstimatedVehicleJourney(journey);

    assertSame(storedTrip, repository.getCarpoolTrips().iterator().next());
  }

  @Test
  void processEstimatedVehicleJourney_redeliveryAfterCancellation_upsertsTrip() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    updater.processEstimatedVehicleJourney(cancelledJourney());

    updater.processEstimatedVehicleJourney(journey);

    assertTrue(tripIsInRepository(mapper.tripId(journey)));
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

  @Test
  void newTripsBeyondTheLimitAreDropped() {
    var limited = updaterHoldingAtMost(2);

    limited.processEstimatedVehicleJourney(journeyWithCode("a"));
    limited.processEstimatedVehicleJourney(journeyWithCode("b"));
    limited.processEstimatedVehicleJourney(journeyWithCode("c"));

    assertTrue(held("a"));
    assertTrue(held("b"));
    assertFalse(held("c"));
  }

  @Test
  void aHeldTripCanStillChangeWhenTheFeedIsFull() {
    var limited = updaterHoldingAtMost(2);
    var changedA = arrivingLater(journeyWithCode("a"));

    limited.processEstimatedVehicleJourney(journeyWithCode("a"));
    limited.processEstimatedVehicleJourney(journeyWithCode("b"));
    limited.processEstimatedVehicleJourney(changedA);

    var stored = repository.getCarpoolTrip(mapper.tripId(changedA)).trip();
    assertEquals(mapper.mapSiriToCarpoolTrip(changedA).endTime(), stored.endTime());
  }

  @Test
  void aCancellationFreesASlot() {
    var limited = updaterHoldingAtMost(2);
    var cancelledA = journeyWithCode("a");
    cancelledA.setCancellation(true);

    limited.processEstimatedVehicleJourney(journeyWithCode("a"));
    limited.processEstimatedVehicleJourney(journeyWithCode("b"));
    limited.processEstimatedVehicleJourney(cancelledA);
    limited.processEstimatedVehicleJourney(journeyWithCode("c"));

    assertFalse(held("a"));
    assertTrue(held("b"));
    assertTrue(held("c"));
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

  private SiriETCarpoolingUpdater updaterHoldingAtMost(int maxTrips) {
    return new SiriETCarpoolingUpdater(
      params(),
      repository,
      resolver,
      CarpoolingParametersTestData.withMaxTrips(maxTrips)
    );
  }

  private boolean held(String code) {
    return tripIsInRepository(mapper.tripId(journeyWithCode(code)));
  }

  /** The journey, arriving 30 minutes later: the same trip, changed. */
  private static EstimatedVehicleJourney arrivingLater(EstimatedVehicleJourney journey) {
    var last = journey.getEstimatedCalls().getEstimatedCalls().getLast();
    last.setAimedArrivalTime(last.getAimedArrivalTime().plusMinutes(30));
    return journey;
  }

  private static DefaultSiriETUpdaterParameters params() {
    return new DefaultSiriETUpdaterParameters(
      "carpool-test",
      FEED_ID,
      false,
      "http://localhost/never-fetched",
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
