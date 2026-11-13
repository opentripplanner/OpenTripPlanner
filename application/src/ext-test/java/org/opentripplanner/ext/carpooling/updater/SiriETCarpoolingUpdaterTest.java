package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.recordedAt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.CarpoolingParametersTestData;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTripResolver;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.EstimatedVersionFrameStructure;

class SiriETCarpoolingUpdaterTest {

  private static final String FEED_ID = "EN";
  private static final ZonedDateTime T = ZonedDateTime.parse("2026-10-23T09:39:45+02:00");

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
      params("http://localhost/never-fetched", Duration.ofMinutes(1)),
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
    var stored = journeyWithLatestExpectedArrivalTime(0, 10);
    updater.processEstimatedVehicleJourney(stored);
    var storedTripId = mapper.tripId(stored);
    assertTrue(tripIsInRepository(storedTripId));

    doThrow(new RuntimeException("corridor blew up")).when(resolver).resolve(any(), any());
    // The same trip with the same geometry and a wider budget: the corridor is recomputed on the
    // stored vertices, and when that throws, the stored trip with the old budget must be dropped.
    updater.processEstimatedVehicleJourney(journeyWithLatestExpectedArrivalTime(0, 20));

    assertFalse(tripIsInRepository(storedTripId));
  }

  @Test
  void processEstimatedVehicleJourney_budgetChanged_recomputesCorridorOnStoredVertices() {
    updater.processEstimatedVehicleJourney(journeyWithLatestExpectedArrivalTime(0, 10));
    var storedVertices = repository.getCarpoolTrips().iterator().next().vertices();
    doAnswer(invocation ->
      RoutableCarpoolTripTestData.withVertices(invocation.getArgument(0), invocation.getArgument(1))
    )
      .when(resolver)
      .resolve(any(), any());

    // The same geometry with a wider budget.
    updater.processEstimatedVehicleJourney(journeyWithLatestExpectedArrivalTime(0, 20));

    verify(resolver, times(1)).resolve(any());
    verify(resolver, times(1)).resolve(any(), eq(storedVertices));
    assertEquals(storedVertices, repository.getCarpoolTrips().iterator().next().vertices());
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
  void processEstimatedVehicleJourney_otherTripOnSameRoute_reusesItsCorridor() {
    var trip = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(trip);
    var otherTrip = minimalCompleteJourney();
    otherTrip.setEstimatedVehicleJourneyCode("other");

    updater.processEstimatedVehicleJourney(otherTrip);

    verify(resolver, times(1)).resolve(any());
    verify(resolver, never()).resolve(any(), any());
    assertSame(corridorOf(trip), corridorOf(otherTrip));
  }

  @Test
  void processEstimatedVehicleJourney_otherTripOnSameRouteWithOtherBudgets_getsItsOwnCorridor() {
    var trip = journeyWithLatestExpectedArrivalTime(0, 10);
    updater.processEstimatedVehicleJourney(trip);
    var otherTrip = journeyWithLatestExpectedArrivalTime(0, 20);
    otherTrip.setEstimatedVehicleJourneyCode("other");

    updater.processEstimatedVehicleJourney(otherTrip);

    assertNotSame(corridorOf(trip), corridorOf(otherTrip));
  }

  @Test
  void processEstimatedVehicleJourney_otherTripOnOtherRoutePoints_getsItsOwnCorridor() {
    var trip = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(trip);
    var otherTrip = journeyWithMovedDestination();
    otherTrip.setEstimatedVehicleJourneyCode("other");

    updater.processEstimatedVehicleJourney(otherTrip);

    assertNotSame(corridorOf(trip), corridorOf(otherTrip));
  }

  @Test
  void processEstimatedVehicleJourney_unchangedGeometry_reusesStoredVerticesAndCorridor() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    var stored = repository.getCarpoolTrips().iterator().next();

    // Same geometry, later times: a changed journey, which is not dropped as a re-delivery.
    var later = arrivingLater(minimalCompleteJourney());
    updater.processEstimatedVehicleJourney(later);

    verify(resolver, times(1)).resolve(any());
    verify(resolver, never()).resolve(any(), any());
    var reused = repository.getCarpoolTrips().iterator().next();
    assertEquals(mapper.mapSiriToCarpoolTrip(later).endTime(), reused.trip().endTime());
    assertEquals(stored.vertices(), reused.vertices());
    assertSame(stored.corridor(), reused.corridor());
  }

  @Test
  void processEstimatedVehicleJourney_unchangedRedelivery_keepsStoredTrip() {
    var departure = ZonedDateTime.now();
    updater.processEstimatedVehicleJourney(minimalCompleteJourney(departure));
    var storedTrip = repository.getCarpoolTrips().iterator().next();

    updater.processEstimatedVehicleJourney(minimalCompleteJourney(departure));

    assertSame(storedTrip, repository.getCarpoolTrips().iterator().next());
  }

  @Test
  void processEstimatedVehicleJourney_unchangedRedeliveryRecordedLater_storesTheNewStamp() {
    // v1 at T, then v3 at T+2 identical to v1, then v2 at T+1 arriving late. Unless v3 advances
    // the stored stamp, v2 passes as newer than v1 and reverts the trip.
    var departure = ZonedDateTime.now();
    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(departure), T));
    updater.processEstimatedVehicleJourney(
      recordedAt(minimalCompleteJourney(departure), T.plusSeconds(2))
    );
    assertEquals(T.plusSeconds(2), storedTrip().trip().recordedAt());
    var stored = storedTrip();

    updater.processEstimatedVehicleJourney(
      recordedAt(arrivingLater(minimalCompleteJourney(departure)), T.plusSeconds(1))
    );

    assertSame(stored, storedTrip());
    verify(resolver, times(1)).resolve(any());
  }

  @Test
  void processEstimatedVehicleJourney_unchangedRedeliveryStampedAfterUnstamped_storesTheStamp() {
    var departure = ZonedDateTime.now();
    updater.processEstimatedVehicleJourney(minimalCompleteJourney(departure));

    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(departure), T));

    assertEquals(T, storedTrip().trip().recordedAt());
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

  @Test
  void aTripThatCannotBeResolvedHoldsASlot() {
    var limited = updaterHoldingAtMost(1);
    doReturn(null).when(resolver).resolve(any());

    limited.processEstimatedVehicleJourney(journeyWithCode("a"));
    limited.processEstimatedVehicleJourney(journeyWithCode("b"));

    verify(resolver, times(1)).resolve(any());
  }

  @Test
  void aCancellationFreesTheSlotOfATripThatCannotBeResolved() {
    var limited = updaterHoldingAtMost(1);
    var cancelledA = journeyWithCode("a");
    cancelledA.setCancellation(true);
    doReturn(null).when(resolver).resolve(any());
    limited.processEstimatedVehicleJourney(journeyWithCode("a"));
    limited.processEstimatedVehicleJourney(cancelledA);

    doAnswer(invocation -> RoutableCarpoolTripTestData.withDummyVertices(invocation.getArgument(0)))
      .when(resolver)
      .resolve(any());
    limited.processEstimatedVehicleJourney(journeyWithCode("b"));

    assertTrue(held("b"));
  }

  // --- RecordedAtTime ordering ---------------------------------------------------------------

  @Test
  void processEstimatedVehicleJourney_olderThanStored_isIgnored() {
    var stored = seedActiveTripRecordedAt(T);

    updater.processEstimatedVehicleJourney(
      recordedAt(journeyWithMovedDestination(), T.minusSeconds(1))
    );

    assertSame(stored, storedTrip());
    verify(resolver, times(1)).resolve(any());
  }

  @Test
  void processEstimatedVehicleJourney_newerThanStored_replaces() {
    var stored = seedActiveTripRecordedAt(T);

    updater.processEstimatedVehicleJourney(
      recordedAt(journeyWithMovedDestination(), T.plusSeconds(1))
    );

    assertNotSame(stored, storedTrip());
    assertEquals(T.plusSeconds(1), storedTrip().trip().recordedAt());
  }

  @Test
  void processEstimatedVehicleJourney_recordedAtSameTimeAsStored_replaces() {
    // A source may stamp a snapshot rather than each change: an equal stamp must not silence it.
    var stored = seedActiveTripRecordedAt(T);

    updater.processEstimatedVehicleJourney(recordedAt(journeyWithMovedDestination(), T));

    assertNotSame(stored, storedTrip());
  }

  @Test
  void processEstimatedVehicleJourney_withoutRecordedAt_replacesStampedStored() {
    var stored = seedActiveTripRecordedAt(T);

    updater.processEstimatedVehicleJourney(journeyWithMovedDestination());

    assertNotSame(stored, storedTrip());
    assertNull(storedTrip().trip().recordedAt());
  }

  @Test
  void processEstimatedVehicleJourney_stampedAfterUnstampedStored_replaces() {
    var stored = seedActiveTrip();

    updater.processEstimatedVehicleJourney(
      recordedAt(journeyWithMovedDestination(), T.minusDays(1))
    );

    assertNotSame(stored, storedTrip());
  }

  @Test
  void processEstimatedVehicleJourney_activeOlderThanCancellation_isIgnored() {
    // The case a multi-instance proxy produces: one instance delivers the cancellation, another
    // still holds the active version and delivers it afterwards.
    var tripId = seedActiveTripRecordedAt(T).trip().getId();
    updater.processEstimatedVehicleJourney(recordedAt(cancelledJourney(), T.plusSeconds(1)));
    assertFalse(tripIsInRepository(tripId));

    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), T));

    assertFalse(tripIsInRepository(tripId));
    verify(resolver, times(1)).resolve(any());
  }

  @Test
  void processEstimatedVehicleJourney_activeNewerThanUnresolvableRemoval_isAdded() {
    // The driver moves a waypoint somewhere no car can reach, then moves it back: the tombstone
    // the first move left must not block the second.
    var tripId = seedActiveTripRecordedAt(T).trip().getId();
    doReturn(null).when(resolver).resolve(any());
    updater.processEstimatedVehicleJourney(
      recordedAt(journeyWithMovedDestination(), T.plusSeconds(1))
    );
    assertFalse(tripIsInRepository(tripId));

    doAnswer(invocation -> RoutableCarpoolTripTestData.withDummyVertices(invocation.getArgument(0)))
      .when(resolver)
      .resolve(any());
    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), T.plusSeconds(2)));

    assertTrue(tripIsInRepository(tripId));
    assertEquals(T.plusSeconds(2), storedTrip().trip().recordedAt());
  }

  @Test
  void processEstimatedVehicleJourney_cancellationOfUnknownTrip_stillLeavesTombstone() {
    updater.processEstimatedVehicleJourney(recordedAt(cancelledJourney(), T.plusSeconds(1)));

    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), T));

    assertTrue(repository.getCarpoolTrips().isEmpty());
  }

  @Test
  void processEstimatedVehicleJourney_activeOlderThanUnstampedCancellation_isAdded() {
    // Without a stamp on the cancellation there is nothing to order against.
    var tripId = seedActiveTripRecordedAt(T).trip().getId();
    updater.processEstimatedVehicleJourney(cancelledJourney());

    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), T.minusDays(1)));

    assertTrue(tripIsInRepository(tripId));
  }

  @Test
  void processEstimatedVehicleJourney_activeOlderThanFewerThanTwoCallsRemoval_isIgnored() {
    var tripId = seedActiveTripRecordedAt(T).trip().getId();
    updater.processEstimatedVehicleJourney(
      recordedAt(journeyWithAllButOneCallCancelled(), T.plusSeconds(1))
    );
    assertFalse(tripIsInRepository(tripId));

    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), T));

    assertFalse(tripIsInRepository(tripId));
  }

  @Test
  void processEstimatedVehicleJourney_activeOlderThanUnresolvableRemoval_isIgnored() {
    var tripId = seedActiveTripRecordedAt(T).trip().getId();
    doReturn(null).when(resolver).resolve(any());
    updater.processEstimatedVehicleJourney(
      recordedAt(journeyWithMovedDestination(), T.plusSeconds(1))
    );
    assertFalse(tripIsInRepository(tripId));

    // The original geometry would resolve again; the tombstone must stop it before it is tried.
    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), T));

    assertFalse(tripIsInRepository(tripId));
    verify(resolver, times(2)).resolve(any());
  }

  @Test
  void removeExpiredTrips_sweepsTombstonesOnTripExpiry() {
    var tripId = seedActiveTripRecordedAt(T).trip().getId();
    updater.processEstimatedVehicleJourney(recordedAt(cancelledJourney(), T));
    var stale = recordedAt(minimalCompleteJourney(), T.minusSeconds(1));

    updater.removeExpiredTrips(
      T.plus(CarpoolingParameters.DEFAULT.tripExpiry()).minusSeconds(1).toInstant()
    );
    updater.processEstimatedVehicleJourney(stale);
    assertFalse(tripIsInRepository(tripId));

    updater.removeExpiredTrips(
      T.plus(CarpoolingParameters.DEFAULT.tripExpiry()).plusSeconds(1).toInstant()
    );
    updater.processEstimatedVehicleJourney(stale);
    assertTrue(tripIsInRepository(tripId));
  }

  @Test
  void processEstimatedTimetableDeliveries_journeyWithoutRecordedAt_takesTheFrames() {
    var stored = seedActiveTripRecordedAt(T);

    updater.processEstimatedTimetableDeliveries(
      List.of(deliveryWithFrame(T.minusSeconds(1), journeyWithMovedDestination()))
    );

    assertSame(stored, storedTrip());
  }

  @Test
  void processEstimatedTimetableDeliveries_journeyWithRecordedAt_keepsItsOwn() {
    var stored = seedActiveTripRecordedAt(T);

    updater.processEstimatedTimetableDeliveries(
      List.of(
        deliveryWithFrame(
          T.minusSeconds(1),
          recordedAt(journeyWithMovedDestination(), T.plusSeconds(1))
        )
      )
    );

    assertNotSame(stored, storedTrip());
    assertEquals(T.plusSeconds(1), storedTrip().trip().recordedAt());
  }

  private static EstimatedTimetableDeliveryStructure deliveryWithFrame(
    ZonedDateTime frameRecordedAt,
    EstimatedVehicleJourney... journeys
  ) {
    var frame = new EstimatedVersionFrameStructure();
    frame.setRecordedAtTime(frameRecordedAt);
    frame.getEstimatedVehicleJourneies().addAll(List.of(journeys));
    var delivery = new EstimatedTimetableDeliveryStructure();
    delivery.getEstimatedJourneyVersionFrames().add(frame);
    return delivery;
  }

  private RoutableCarpoolTrip seedActiveTripRecordedAt(ZonedDateTime recordedAt) {
    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), recordedAt));
    var stored = storedTrip();
    assertEquals(recordedAt, stored.trip().recordedAt());
    return stored;
  }

  /** The single trip in the repository. */
  private RoutableCarpoolTrip storedTrip() {
    var trips = repository.getCarpoolTrips();
    assertEquals(1, trips.size());
    return trips.iterator().next();
  }

  private FeedScopedId seedActiveTrip() {
    var journey = minimalCompleteJourney();
    updater.processEstimatedVehicleJourney(journey);
    var tripId = mapper.tripId(journey);
    assertTrue(tripIsInRepository(tripId));
    return tripId;
  }

  private CarpoolCorridor corridorOf(EstimatedVehicleJourney journey) {
    return repository.getCarpoolTrip(mapper.tripId(journey)).corridor();
  }

  private boolean tripIsInRepository(FeedScopedId id) {
    return repository
      .getCarpoolTrips()
      .stream()
      .anyMatch(t -> t.trip().getId().equals(id));
  }

  private SiriETCarpoolingUpdater updaterHoldingAtMost(int maxTrips) {
    return new SiriETCarpoolingUpdater(
      params("http://localhost/never-fetched", Duration.ofMinutes(1)),
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

  @Test
  void aPollReadsAtMostTheMaximumNumberOfPages(@TempDir Path feed) throws IOException {
    writeThreePagesWithMoreData(feed);

    updaterReadingAtMostTwoPages(feed, Duration.ofMinutes(1)).runPolling();

    assertEquals(1, unreadPages(feed));
  }

  @Test
  void anUpdaterThatPollsOnlyOnceReadsEveryPage(@TempDir Path feed) throws IOException {
    writeThreePagesWithMoreData(feed);

    updaterReadingAtMostTwoPages(feed, Duration.ZERO).runPolling();

    assertEquals(0, unreadPages(feed));
  }

  private static void writeThreePagesWithMoreData(Path feed) throws IOException {
    var page = PAGE_WITH_MORE_DATA.formatted(Instant.now());
    for (int i = 1; i <= 3; i++) {
      Files.writeString(feed.resolve("page-" + i + ".xml"), page);
    }
  }

  /** A non-positive frequency makes the updater poll only once. */
  private SiriETCarpoolingUpdater updaterReadingAtMostTwoPages(Path feed, Duration frequency) {
    return new SiriETCarpoolingUpdater(
      params(feed.toUri().toString(), frequency),
      repository,
      resolver,
      CarpoolingParametersTestData.withMaxPagesPerPoll(2)
    );
  }

  /** The pages not read yet: the file loader renames every page it reads. */
  private static long unreadPages(Path feed) throws IOException {
    try (var files = Files.list(feed)) {
      return files.filter(f -> f.toString().endsWith(".xml")).count();
    }
  }

  private static DefaultSiriETUpdaterParameters params(String url, Duration frequency) {
    return new DefaultSiriETUpdaterParameters(
      "carpool-test",
      FEED_ID,
      false,
      url,
      frequency,
      "test-requestor",
      Duration.ofSeconds(30),
      Duration.ofMinutes(15),
      false,
      HttpHeaders.empty(),
      false
    );
  }
}
