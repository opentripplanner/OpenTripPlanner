package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTripResolver;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import uk.org.siri.siri21.EstimatedVehicleJourney;

class SiriETCarpoolingUpdaterLimitTest {

  private static final String FEED_ID = "EN";

  private final DefaultCarpoolingRepository repository = new DefaultCarpoolingRepository();
  private final CarpoolSiriMapper mapper = new CarpoolSiriMapper(FEED_ID);
  /** The resolution queue's work, run when a test calls {@link #runQueued()}. */
  private final Deque<Runnable> queued = new ArrayDeque<>();

  @Test
  void newTripsBeyondTheLimitAreDropped() {
    var updater = updater(2);

    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    // a and b are still queued for resolution: they count already.
    updater.processEstimatedVehicleJourney(journey("c"));
    runQueued();

    assertTrue(held("a"));
    assertTrue(held("b"));
    assertFalse(held("c"));
  }

  @Test
  void aHeldTripCanStillChangeWhenTheFeedIsFull() {
    var updater = updater(2);
    var changedA = laterJourney("a");

    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    updater.processEstimatedVehicleJourney(changedA);
    runQueued();

    var stored = repository.getCarpoolTrip(mapper.tripId(changedA)).trip();
    assertEquals(mapper.mapSiriToCarpoolTrip(changedA).endTime(), stored.endTime());
  }

  @Test
  void aCancellationFreesASlot() {
    var updater = updater(2);

    updater.processEstimatedVehicleJourney(journey("a"));
    updater.processEstimatedVehicleJourney(journey("b"));
    updater.processEstimatedVehicleJourney(cancellation("a"));
    updater.processEstimatedVehicleJourney(journey("c"));
    runQueued();

    assertFalse(held("a"));
    assertTrue(held("b"));
    assertTrue(held("c"));
  }

  private SiriETCarpoolingUpdater updater(int maxTrips) {
    var resolver = mock(RoutableCarpoolTripResolver.class);
    when(resolver.resolve(any())).thenAnswer(invocation ->
      RoutableCarpoolTripTestData.withDummyVertices(invocation.getArgument(0))
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
    return new SiriETCarpoolingUpdater(params, repository, resolver, queued::add, maxTrips);
  }

  private void runQueued() {
    while (!queued.isEmpty()) {
      queued.poll().run();
    }
  }

  private boolean held(String code) {
    return repository.getCarpoolTrip(mapper.tripId(journey(code))) != null;
  }

  private static EstimatedVehicleJourney journey(String code) {
    var journey = minimalCompleteJourney();
    journey.setEstimatedVehicleJourneyCode(code);
    return journey;
  }

  /** A changed version of the trip: it arrives 30 minutes later. */
  private static EstimatedVehicleJourney laterJourney(String code) {
    var journey = journey(code);
    var last = journey.getEstimatedCalls().getEstimatedCalls().getLast();
    last.setAimedArrivalTime(last.getAimedArrivalTime().plusMinutes(30));
    return journey;
  }

  private static EstimatedVehicleJourney cancellation(String code) {
    var journey = journey(code);
    journey.setCancellation(true);
    return journey;
  }
}
