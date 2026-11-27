package org.opentripplanner.updater.elevator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.updater.elevator.model.ElevatorStatus.inService;
import static org.opentripplanner.updater.elevator.model.ElevatorStatus.outOfService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.framework.transaction.configure.TransactionFactory;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.event.EventHandler;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.core.model.transaction.UpdateManager;
import org.opentripplanner.street.elevator.ElevatorEdgeIndex;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.updater.DefaultTransitRealTimeUpdateContext;
import org.opentripplanner.updater.elevator.model.ElevatorStatus;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

class ElevatorStatusUpdaterTest {

  private static final String A = "Lutz:1";
  private static final String B = "Koch:2";

  private final Graph graph = new Graph();
  private final RepositoryRegistry registry = TransactionFactory.createRepositoryRegistry();
  private final RepositoryHandle<
    List<ElevatorStatusChanged>,
    List<ElevatorStatusChanged>
  > eventLog = registry.registerRepository(new ArrayList<>(), new EventLogLifecycle());
  private final UpdateManager transitUpdateManager =
    TransactionFactory.createUpdateManagerWithAtomicCommits(
      "test",
      registry,
      Executors.defaultThreadFactory()
    );
  /** Whether elevator A was out of service on the street when each event was handled. */
  private final List<Boolean> aOutOfServiceOnTheStreetAtEvent = new ArrayList<>();
  private int failuresLeft = 0;
  private final FakeDataSource source = new FakeDataSource();
  private final ElevatorStatusUpdater subject;

  ElevatorStatusUpdaterTest() {
    elevator("A", A);
    elevator("B", B);
    transitUpdateManager.register(new RecordingHandler(), eventLog);
    subject = new ElevatorStatusUpdater(new Parameters(), source, ElevatorEdgeIndex.of(graph));
    // Like the writer of the transit domain, a failed task is logged and its writes are committed
    subject.setup(task ->
      transitUpdateManager.submit(context -> {
        try {
          task.run(new DefaultTransitRealTimeUpdateContext(null, null, () -> null, context));
        } catch (IllegalStateException e) {
          // The failure of the test handler
        }
      })
    );
  }

  @AfterEach
  void tearDown() {
    transitUpdateManager.shutdown();
  }

  @Test
  void setsTheElevatorsOutOfServiceOnTheStreetAndPublishesTheEvent() throws Exception {
    poll(outOfService(A), inService(B));

    assertTrue(isOutOfService(A));
    assertFalse(isOutOfService(B));
    assertEquals(List.of(new ElevatorStatusChanged(Set.of(A))), events());
  }

  @Test
  void anElevatorNotInTheSnapshotIsInServiceAgain() throws Exception {
    poll(outOfService(A));
    poll(outOfService(B));

    assertFalse(isOutOfService(A));
    assertTrue(isOutOfService(B));
    assertEquals(
      List.of(new ElevatorStatusChanged(Set.of(A)), new ElevatorStatusChanged(Set.of(B))),
      events()
    );
  }

  @Test
  void noEventIfTheElevatorsOutOfServiceDidNotChange() throws Exception {
    poll(outOfService(A), inService(B));
    poll(inService(B), outOfService(A));
    poll(outOfService(A));

    assertEquals(List.of(new ElevatorStatusChanged(Set.of(A))), events());
  }

  @Test
  void anElevatorIsOutOfServiceIfOneOfItsStatusesIs() throws Exception {
    poll(inService(A), outOfService(A), inService(B), inService(B));

    assertTrue(isOutOfService(A));
    assertFalse(isOutOfService(B));
    assertEquals(List.of(new ElevatorStatusChanged(Set.of(A))), events());
  }

  @Test
  void theStreetIsUpdatedBeforeTheEventIsHandled() throws Exception {
    poll(outOfService(A));
    poll(inService(A));

    // When the transfers react to the event, the street already has the new status
    assertEquals(List.of(true, false), aOutOfServiceOnTheStreetAtEvent);
  }

  @Test
  void aFailedUpdateIsRetriedOnTheNextPoll() throws Exception {
    failuresLeft = 1;
    poll(outOfService(A));

    assertEquals(List.of(), events());

    poll(outOfService(A));

    assertEquals(List.of(new ElevatorStatusChanged(Set.of(A))), events());
  }

  @Test
  void anElevatorNotInTheGraphIsInTheEvent() throws Exception {
    poll(outOfService("unknown"));

    assertEquals(List.of(new ElevatorStatusChanged(Set.of("unknown"))), events());
  }

  private void poll(ElevatorStatus... statuses) throws Exception {
    source.statuses = List.of(statuses);
    subject.run();
    // The writer runs one task at a time, so this waits for the task of the poll
    transitUpdateManager.submit(context -> {}).get();
  }

  /** The events handled in committed transactions. */
  private List<ElevatorStatusChanged> events() {
    return eventLog.repositorySnapshot(registry.scope());
  }

  private boolean isOutOfService(String id) {
    for (var edge : graph.findEdges(ElevatorHopEdge.class)) {
      if (edge.id().filter(id::equals).isPresent()) {
        return edge.isOutOfService();
      }
    }
    throw new IllegalArgumentException(id);
  }

  private void elevator(String label, String id) {
    var from = intersectionVertex(label + "0", 0, 0);
    var to = intersectionVertex(label + "1", 0, 0);
    graph.addVertex(from);
    graph.addVertex(to);
    ElevatorHopEdge.bidirectional(
      from,
      to,
      StreetTraversalPermission.ALL,
      Accessibility.POSSIBLE,
      1,
      -1,
      id
    );
  }

  /**
   * Records the handled events in a repository, so only the events of committed transactions are
   * kept.
   */
  private class RecordingHandler
    implements EventHandler<ElevatorStatusChanged, List<ElevatorStatusChanged>>
  {

    @Override
    public Class<ElevatorStatusChanged> eventType() {
      return ElevatorStatusChanged.class;
    }

    @Override
    public void handle(ElevatorStatusChanged event, List<ElevatorStatusChanged> events) {
      if (failuresLeft > 0) {
        failuresLeft--;
        throw new IllegalStateException("Handling the event failed");
      }
      events.add(event);
      aOutOfServiceOnTheStreetAtEvent.add(isOutOfService(A));
    }
  }

  private static class EventLogLifecycle
    implements RepositoryLifecycle<List<ElevatorStatusChanged>, List<ElevatorStatusChanged>>
  {

    @Override
    public List<ElevatorStatusChanged> copyOnWrite(List<ElevatorStatusChanged> snapshot) {
      return new ArrayList<>(snapshot);
    }

    @Override
    public List<ElevatorStatusChanged> freeze(List<ElevatorStatusChanged> repository) {
      return List.copyOf(repository);
    }
  }

  private static class FakeDataSource implements DataSource<ElevatorStatus> {

    private List<ElevatorStatus> statuses = List.of();

    @Override
    public boolean update() {
      return true;
    }

    @Override
    public List<ElevatorStatus> getUpdates() {
      return statuses;
    }
  }

  private record Parameters() implements PollingGraphUpdaterParameters {
    @Override
    public Duration frequency() {
      return Duration.ZERO;
    }

    @Override
    public String configRef() {
      return "elevator-test";
    }
  }
}
