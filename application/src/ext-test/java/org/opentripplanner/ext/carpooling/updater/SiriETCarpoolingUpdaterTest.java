package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.recordedAt;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolTripWithVerticesTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.EstimatedVersionFrameStructure;

class SiriETCarpoolingUpdaterTest {

  private static final String FEED_ID = "EN";
  private static final ZonedDateTime T = ZonedDateTime.parse("2026-10-23T09:39:45+02:00");

  private DefaultCarpoolingRepository repository;
  private SiriETCarpoolingUpdater updater;
  private CarpoolTripVertexResolver resolver;
  private final CarpoolSiriMapper mapper = new CarpoolSiriMapper(FEED_ID);

  @BeforeEach
  void setUp() {
    repository = new DefaultCarpoolingRepository();
    resolver = mock(CarpoolTripVertexResolver.class);
    when(resolver.resolve(any())).thenAnswer(invocation ->
      CarpoolTripWithVerticesTestData.withDummyVertices(invocation.getArgument(0))
    );
    var params = new DefaultSiriETUpdaterParameters(
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
    updater = new SiriETCarpoolingUpdater(params, repository, resolver);
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

    doAnswer(invocation ->
      CarpoolTripWithVerticesTestData.withDummyVertices(invocation.getArgument(0))
    )
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

    doAnswer(invocation ->
      CarpoolTripWithVerticesTestData.withDummyVertices(invocation.getArgument(0))
    )
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

    updater.removeExpiredTrips(T.plusDays(2).minusSeconds(1).toInstant());
    updater.processEstimatedVehicleJourney(stale);
    assertFalse(tripIsInRepository(tripId));

    updater.removeExpiredTrips(T.plusDays(2).plusSeconds(1).toInstant());
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

  private CarpoolTripWithVertices seedActiveTripRecordedAt(ZonedDateTime recordedAt) {
    updater.processEstimatedVehicleJourney(recordedAt(minimalCompleteJourney(), recordedAt));
    var stored = storedTrip();
    assertEquals(recordedAt, stored.trip().recordedAt());
    return stored;
  }

  /** The single trip in the repository. */
  private CarpoolTripWithVertices storedTrip() {
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

  private boolean tripIsInRepository(FeedScopedId id) {
    return repository
      .getCarpoolTrips()
      .stream()
      .anyMatch(t -> t.trip().getId().equals(id));
  }
}
