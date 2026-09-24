package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTestCoordinates;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;

class CarpoolTripResolutionQueueTest {

  private final DefaultCarpoolingRepository repository = new DefaultCarpoolingRepository(
    CarpoolingParameters.DEFAULT.expirySweepInterval()
  );
  /** The background thread's work, run when a test calls {@link #runQueued()}. */
  private final Deque<Runnable> queued = new ArrayDeque<>();
  private final CarpoolTrip trip = newTrip();

  @Test
  void aTripIsStoredOnceResolved() {
    var queue = queue(RoutableCarpoolTripTestData::withDummyVertices);

    queue.submit(trip);
    assertNull(repository.getCarpoolTrip(trip.getId()));

    runQueued();
    assertNotNull(repository.getCarpoolTrip(trip.getId()));
  }

  @Test
  void changesAreAppliedInOrder() {
    var queue = queue(RoutableCarpoolTripTestData::withDummyVertices);

    queue.submit(trip);
    queue.remove(trip.getId());
    runQueued();
    assertNull(repository.getCarpoolTrip(trip.getId()));

    queue.remove(trip.getId());
    queue.submit(trip);
    runQueued();
    assertNotNull(repository.getCarpoolTrip(trip.getId()));
  }

  @Test
  void onlyATripsLatestWaitingChangeIsResolved() {
    var resolved = new ArrayList<CarpoolTrip>();
    var queue = queue(t -> {
      resolved.add(t);
      return RoutableCarpoolTripTestData.withDummyVertices(t);
    });
    var changed = moreSeats(trip);

    queue.submit(trip);
    queue.submit(changed);
    assertEquals(1, queued.size());

    runQueued();
    assertEquals(List.of(changed), resolved);
  }

  @Test
  void aChangeQueuedWhileItsTripResolvesIsAppliedAfterwards() {
    var changed = moreSeats(trip);
    var queue = new CarpoolTripResolutionQueue[1];
    queue[0] = queue(t -> {
      if (t == trip) {
        queue[0].submit(changed);
      }
      return RoutableCarpoolTripTestData.withDummyVertices(t);
    });

    queue[0].submit(trip);
    runQueued();

    assertSame(changed, repository.getCarpoolTrip(trip.getId()).trip());
  }

  @Test
  void aTripThatCannotBeResolvedIsRemoved() {
    repository.upsertCarpoolTrip(RoutableCarpoolTripTestData.withDummyVertices(trip));

    queue(t -> null).submit(trip);
    runQueued();

    assertNull(repository.getCarpoolTrip(trip.getId()));
  }

  @Test
  void aTripThatFailsToResolveIsRemovedAndTheQueueGoesOn() {
    repository.upsertCarpoolTrip(RoutableCarpoolTripTestData.withDummyVertices(trip));
    var other = newTrip();
    var queue = queue(t -> {
      if (t == trip) {
        throw new IllegalStateException("boom");
      }
      return RoutableCarpoolTripTestData.withDummyVertices(t);
    });

    queue.submit(trip);
    queue.submit(other);
    runQueued();

    assertNull(repository.getCarpoolTrip(trip.getId()));
    assertNotNull(repository.getCarpoolTrip(other.getId()));
  }

  private CarpoolTripResolutionQueue queue(Function<CarpoolTrip, RoutableCarpoolTrip> resolver) {
    return new CarpoolTripResolutionQueue(queued::add, resolver, repository);
  }

  private void runQueued() {
    while (!queued.isEmpty()) {
      queued.poll().run();
    }
  }

  /** Another version of the trip: a new object with the same id. */
  private static CarpoolTrip moreSeats(CarpoolTrip trip) {
    return new CarpoolTripBuilder(trip.getId())
      .withStops(trip.stops())
      .withStartTime(trip.startTime())
      .withEndTime(trip.endTime())
      .withTotalCapacity(trip.totalCapacity() + 1)
      .build();
  }

  private static CarpoolTrip newTrip() {
    return CarpoolTripTestData.createSimpleTrip(
      CarpoolTestCoordinates.OSLO_CENTER,
      CarpoolTestCoordinates.OSLO_EAST
    );
  }
}
