package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripWithVerticesTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import uk.org.siri.siri21.EstimatedVehicleJourney;

class SiriETCarpoolingUpdaterLimitTest {

  private static final String FEED_ID = "EN";

  private DefaultCarpoolingRepository repository;
  private CarpoolTripVertexResolver resolver;
  private final CarpoolSiriMapper mapper = new CarpoolSiriMapper(FEED_ID);

  @BeforeEach
  void setUp() {
    repository = new DefaultCarpoolingRepository();
    resolver = mock(CarpoolTripVertexResolver.class);
    when(resolver.withCorridor(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(resolver.resolve(any())).thenAnswer(invocation ->
      CarpoolTripWithVerticesTestData.withDummyVertices(invocation.getArgument(0))
    );
  }

  private SiriETCarpoolingUpdater updater(Executor executor, int maxTrips) {
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
    return new SiriETCarpoolingUpdater(params, repository, resolver, executor, maxTrips);
  }

  private final Map<String, EstimatedVehicleJourney> journeys = new HashMap<>();

  private static EstimatedVehicleJourney freshJourney(String code) {
    var journey = minimalCompleteJourney();
    journey.setEstimatedVehicleJourneyCode(code);
    return journey;
  }

  /**
   * One journey object per code, so a re-delivery carries identical content (the fixture stamps
   * the current time on its calls, so two builds would differ by microseconds).
   */
  private EstimatedVehicleJourney journey(String code) {
    return journeys.computeIfAbsent(code, SiriETCarpoolingUpdaterLimitTest::freshJourney);
  }

  /** A changed version of the trip: its last call arrives later. */
  private static EstimatedVehicleJourney laterJourney(String code) {
    var journey = freshJourney(code);
    var last = journey.getEstimatedCalls().getEstimatedCalls().getLast();
    last.setAimedArrivalTime(last.getAimedArrivalTime().plusMinutes(30));
    return journey;
  }

  private static EstimatedVehicleJourney cancellation(String code) {
    var journey = freshJourney(code);
    journey.setCancellation(true);
    return journey;
  }

  private boolean held(String code) {
    return repository.getCarpoolTrip(mapper.tripId(journey(code))) != null;
  }

  @Test
  void newTripsBeyondTheLimitAreDroppedHeldOnesStayUpdatable() {
    var updater = updater(Runnable::run, 2);
    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    updater.processEstimatedVehicleJourney(journey("c"));

    assertTrue(held("a"));
    assertTrue(held("b"));
    assertFalse(held("c"), "the third trip does not fit");
    assertEquals(2, repository.getCarpoolTrips().size());

    // An update of a held trip is not a new trip and always goes through.
    updater.processEstimatedVehicleJourney(journey("a"));
    assertEquals(2, repository.getCarpoolTrips().size());
  }

  @Test
  void aCancellationFreesASlot() {
    var updater = updater(Runnable::run, 2);
    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    updater.processEstimatedVehicleJourney(cancellation("a"));
    updater.processEstimatedVehicleJourney(journey("c"));

    assertFalse(held("a"));
    assertTrue(held("b"));
    assertTrue(held("c"));
  }

  @Test
  void tripsStillQueuedForResolutionCountTowardsTheLimit() {
    var queued = new ArrayDeque<Runnable>();
    Executor deferring = queued::add;
    var updater = updater(deferring, 2);
    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    assertEquals(2, updater.pendingResolutions());
    assertEquals(0, repository.getCarpoolTrips().size());

    updater.processEstimatedVehicleJourney(journey("c"));
    runAll(queued);

    assertTrue(held("a"));
    assertTrue(held("b"));
    assertFalse(held("c"), "rejected while a and b were still queued");
  }

  @Test
  void anUnchangedRedeliveryIsNeitherQueuedNorCounted() {
    var queued = new ArrayDeque<Runnable>();
    var updater = updater(queued::add, 2);
    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    updater.processEstimatedVehicleJourney(journey("a"));
    assertEquals(2, updater.pendingResolutions(), "the re-delivery of a queued trip is dropped");

    runAll(queued);
    updater.processEstimatedVehicleJourney(journey("a"));
    assertEquals(0, updater.pendingResolutions(), "the re-delivery of a held trip is dropped");
    assertTrue(held("a"));
  }

  @Test
  void aChangedVersionOfAQueuedTripIsAcceptedWhenTheRepositoryIsFull() {
    var queued = new ArrayDeque<Runnable>();
    var updater = updater(queued::add, 2);
    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    var original = mapper.mapSiriToCarpoolTrip(journey("a"));
    updater.processEstimatedVehicleJourney(laterJourney("a"));
    updater.processEstimatedVehicleJourney(journey("c"));

    runAll(queued);
    assertTrue(held("a"));
    assertTrue(held("b"));
    assertFalse(held("c"), "still no room for a third trip");
    var storedA = repository.getCarpoolTrip(mapper.tripId(journey("a"))).trip();
    assertTrue(storedA.endTime().isAfter(original.endTime()), "the changed version was stored");
  }

  @Test
  void theLimitMustBePositive() {
    assertThrows(IllegalArgumentException.class, () -> updater(Runnable::run, 0));
  }

  private static void runAll(Deque<Runnable> tasks) {
    while (!tasks.isEmpty()) {
      tasks.poll().run();
    }
  }
}
