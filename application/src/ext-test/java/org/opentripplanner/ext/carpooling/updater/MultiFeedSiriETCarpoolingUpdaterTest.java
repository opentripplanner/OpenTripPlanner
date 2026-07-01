package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.cancelledJourney;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;

/**
 * Verifies that two {@link SiriETCarpoolingUpdater} instances can share a single
 * {@link DefaultCarpoolingRepository} without colliding when both receive a SIRI journey
 * with the same {@code EstimatedVehicleJourneyCode}. The two updaters represent two
 * independent SIRI-ET feeds; their feedIds are the only thing that should keep their
 * trips apart in the shared repository.
 *
 * <p>The {@code processEstimatedTimetableDeliveries} pipeline is called once per updater
 * per polling cycle and receives only the deliveries fetched from that updater's source
 * URL — there is no per-call feed multiplexing inside a single delivery list, so the
 * tests below invoke each updater explicitly.
 */
class MultiFeedSiriETCarpoolingUpdaterTest {

  private static final String FEED_A = "EN";
  private static final String FEED_B = "AT";

  private DefaultCarpoolingRepository repository;
  private SiriETCarpoolingUpdater updaterA;
  private SiriETCarpoolingUpdater updaterB;
  private final CarpoolSiriMapper mapperA = new CarpoolSiriMapper(FEED_A);
  private final CarpoolSiriMapper mapperB = new CarpoolSiriMapper(FEED_B);

  @BeforeEach
  void setUp() {
    repository = new DefaultCarpoolingRepository();
    updaterA = new SiriETCarpoolingUpdater(paramsFor(FEED_A), repository);
    updaterB = new SiriETCarpoolingUpdater(paramsFor(FEED_B), repository);
  }

  @Test
  void sameJourneyCode_acrossFeeds_storedAsDistinctTrips() {
    var journey = minimalCompleteJourney();

    updaterA.processEstimatedVehicleJourney(journey);
    updaterB.processEstimatedVehicleJourney(journey);

    var idA = mapperA.tripId(journey);
    var idB = mapperB.tripId(journey);

    // Same local id, but different feed prefixes
    assertEquals(idA.getId(), idB.getId());
    assertEquals(FEED_A, idA.getFeedId());
    assertEquals(FEED_B, idB.getFeedId());

    Set<FeedScopedId> ids = repository
      .getCarpoolTrips()
      .stream()
      .map(CarpoolTrip::getId)
      .collect(Collectors.toSet());

    assertEquals(Set.of(idA, idB), ids);
  }

  @Test
  void cancellationOnOneFeed_doesNotRemoveTripOnOtherFeed() {
    var journey = minimalCompleteJourney();
    updaterA.processEstimatedVehicleJourney(journey);
    updaterB.processEstimatedVehicleJourney(journey);

    var idA = mapperA.tripId(journey);
    var idB = mapperB.tripId(journey);
    assertTrue(tripIsInRepository(idA));
    assertTrue(tripIsInRepository(idB));

    updaterA.processEstimatedVehicleJourney(cancelledJourney());

    assertFalse(tripIsInRepository(idA));
    assertTrue(tripIsInRepository(idB));
  }

  @Test
  void everyTripCarriesItsOwnFeedPrefix() {
    var journey = minimalCompleteJourney();
    updaterA.processEstimatedVehicleJourney(journey);
    updaterB.processEstimatedVehicleJourney(journey);

    var feedPrefixes = repository
      .getCarpoolTrips()
      .stream()
      .map(t -> t.getId().getFeedId())
      .collect(Collectors.toSet());

    assertEquals(Set.of(FEED_A, FEED_B), feedPrefixes);

    // Every stop on every trip must inherit the trip's feed prefix — guards against
    // a half-finished refactor where the trip id is parameterised but stop ids are not.
    for (var trip : repository.getCarpoolTrips()) {
      var expectedFeed = trip.getId().getFeedId();
      for (var stop : trip.stops()) {
        assertEquals(
          expectedFeed,
          stop.getId().getFeedId(),
          "Stop %s on trip %s should carry feed prefix %s".formatted(
            stop.getId(),
            trip.getId(),
            expectedFeed
          )
        );
      }
    }
  }

  private static DefaultSiriETUpdaterParameters paramsFor(String feedId) {
    return new DefaultSiriETUpdaterParameters(
      "carpool-test-" + feedId,
      feedId,
      false,
      "http://localhost/never-fetched-" + feedId,
      Duration.ofMinutes(1),
      "test-requestor",
      Duration.ofSeconds(30),
      Duration.ofMinutes(15),
      false,
      HttpHeaders.empty(),
      false
    );
  }

  private boolean tripIsInRepository(FeedScopedId id) {
    return repository
      .getCarpoolTrips()
      .stream()
      .anyMatch(t -> t.getId().equals(id));
  }
}
