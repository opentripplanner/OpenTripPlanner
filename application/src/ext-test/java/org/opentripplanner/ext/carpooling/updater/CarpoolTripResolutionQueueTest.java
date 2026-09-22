package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTestCoordinates;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolTripWithVerticesTestData;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

class CarpoolTripResolutionQueueTest {

  /** Runs nothing until told to, like a busy background thread. */
  private static final class ManualExecutor implements Executor {

    private final Deque<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable task) {
      tasks.add(task);
    }

    void runAll() {
      while (!tasks.isEmpty()) {
        tasks.poll().run();
      }
    }
  }

  private final DefaultCarpoolingRepository repository = new DefaultCarpoolingRepository();
  private final ManualExecutor executor = new ManualExecutor();
  private final AtomicInteger resolutions = new AtomicInteger();
  private final CarpoolTrip tripA = newTrip();
  private final CarpoolTrip tripB = newTrip();

  private static CarpoolTrip newTrip() {
    return CarpoolTripTestData.createSimpleTrip(
      CarpoolTestCoordinates.OSLO_CENTER,
      CarpoolTestCoordinates.OSLO_EAST
    );
  }

  private CarpoolTripResolutionQueue queue() {
    return new CarpoolTripResolutionQueue(
      executor,
      trip -> {
        resolutions.incrementAndGet();
        return CarpoolTripWithVerticesTestData.withDummyVertices(trip);
      },
      repository
    );
  }

  @Test
  void aSubmittedTripIsStoredOnceResolvedAndInvisibleBefore() {
    var queue = queue();

    queue.submit(tripA);
    assertEquals(1, queue.pending());
    assertNull(repository.getCarpoolTrip(tripA.getId()), "not routable until resolved");

    executor.runAll();
    assertNotNull(repository.getCarpoolTrip(tripA.getId()));
    assertEquals(0, queue.pending());
  }

  @Test
  void aNewerVersionSupersedesAQueuedOne() {
    var queue = queue();
    queue.submit(tripA);
    queue.submit(tripA);

    executor.runAll();

    assertEquals(1, resolutions.get(), "the superseded version is never resolved");
    assertNotNull(repository.getCarpoolTrip(tripA.getId()));
    assertEquals(0, queue.pending());
  }

  @Test
  void aCancellationWhileQueuedKeepsTheTripOut() {
    var queue = queue();
    queue.submit(tripA);
    queue.cancel(tripA.getId());
    repository.removeCarpoolTrip(tripA.getId());

    executor.runAll();

    assertEquals(0, resolutions.get());
    assertNull(repository.getCarpoolTrip(tripA.getId()));
    assertEquals(0, queue.pending());
  }

  @Test
  void countsQueuedTripsNotHeldYetAndRemembersTheLatestVersion() {
    repository.upsertCarpoolTrip(CarpoolTripWithVerticesTestData.withDummyVertices(tripA));
    var queue = queue();

    queue.submit(tripA);
    queue.submit(tripB);
    assertEquals(2, queue.pending(), "both tasks are queued");
    assertEquals(1, queue.pendingNew(), "only the trip the repository does not hold takes a slot");
    assertEquals(tripB, queue.pendingVersion(tripB.getId()));

    executor.runAll();
    assertEquals(0, queue.pendingNew());
    assertNull(queue.pendingVersion(tripB.getId()));

    queue.submit(tripB);
    queue.cancel(tripB.getId());
    assertEquals(0, queue.pendingNew());
    assertNull(queue.pendingVersion(tripB.getId()));
    executor.runAll();
  }

  @Test
  void anUnresolvableTripIsRemovedAndAFailingResolverIsSurvived() {
    repository.upsertCarpoolTrip(CarpoolTripWithVerticesTestData.withDummyVertices(tripA));
    var unresolvable = new CarpoolTripResolutionQueue(executor, trip -> null, repository);
    unresolvable.submit(tripA);
    executor.runAll();
    assertNull(repository.getCarpoolTrip(tripA.getId()), "an unresolvable update removes the trip");

    var failing = new CarpoolTripResolutionQueue(
      executor,
      trip -> {
        throw new IllegalStateException("boom");
      },
      repository
    );
    failing.submit(tripB);
    executor.runAll();
    assertEquals(0, failing.pending());
    assertNull(repository.getCarpoolTrip(tripB.getId()));
  }
}
