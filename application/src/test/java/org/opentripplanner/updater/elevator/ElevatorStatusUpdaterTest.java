package org.opentripplanner.updater.elevator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.inService;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.outOfService;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

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
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.core.model.transaction.UpdateManager;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusChanged;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepositorySnapshot;
import org.opentripplanner.service.elevatorstatus.configure.ElevatorStatusRepositoryModule;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;
import org.opentripplanner.street.elevator.ElevatorEdgeIndex;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.updater.DefaultTransitRealTimeUpdateContext;
import org.opentripplanner.updater.GraphWriterService;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

class ElevatorStatusUpdaterTest {

  private static final String A = "Lutz:1";
  private static final String B = "Koch:2";

  private final Graph graph = new Graph();
  private final RepositoryRegistry registry = TransactionFactory.createRepositoryRegistry();
  private final RepositoryHandle<
    ElevatorStatusRepositorySnapshot,
    ElevatorStatusRepository
  > handle = ElevatorStatusRepositoryModule.elevatorStatusRepositoryHandle(registry);
  private final UpdateManager transitUpdateManager =
    TransactionFactory.createUpdateManagerWithAtomicCommits(
      "test",
      registry,
      Executors.defaultThreadFactory()
    );
  private final UpdateManager streetUpdateManager =
    TransactionFactory.createUpdateManagerWithAtomicCommits(
      "test-street",
      TransactionFactory.createRepositoryRegistry(),
      Executors.defaultThreadFactory()
    );
  private final List<ElevatorStatusChanged> events = new ArrayList<>();
  private int failuresLeft = 0;
  private final FakeDataSource source = new FakeDataSource();
  private final ElevatorStatusUpdater subject;

  ElevatorStatusUpdaterTest() {
    elevator("A", A);
    elevator("B", B);
    transitUpdateManager.register(new RecordingHandler(), handle);
    subject = new ElevatorStatusUpdater(
      new Parameters(),
      source,
      ElevatorEdgeIndex.of(graph),
      GraphWriterService.forStreetDomain(streetUpdateManager, graph)
    );
    // Like the writer of the transit domain, a failed task is logged and its writes are committed
    subject.setup(task ->
      transitUpdateManager.submit(context -> {
        try {
          task.run(
            new DefaultTransitRealTimeUpdateContext(
              null,
              null,
              () -> null,
              () -> context.repository(handle),
              context
            )
          );
        } catch (IllegalStateException e) {
          // The failure of the test handler
        }
      })
    );
  }

  @AfterEach
  void tearDown() {
    transitUpdateManager.shutdown();
    streetUpdateManager.shutdown();
  }

  @Test
  void setsTheElevatorsOutOfServiceOnTheStreetAndInTheRepository() throws Exception {
    poll(outOfService(A), inService(B));

    assertTrue(isOutOfService(A));
    assertFalse(isOutOfService(B));
    assertEquals(Set.of(A), snapshot().outOfServiceIds());
    assertEquals(List.of(new ElevatorStatusChanged(Set.of(A))), events);
  }

  @Test
  void anElevatorNotInTheSnapshotIsInServiceAgain() throws Exception {
    poll(outOfService(A));
    poll(outOfService(B));

    assertFalse(isOutOfService(A));
    assertTrue(isOutOfService(B));
    assertEquals(Set.of(B), snapshot().outOfServiceIds());
    assertEquals(
      List.of(new ElevatorStatusChanged(Set.of(A)), new ElevatorStatusChanged(Set.of(B))),
      events
    );
  }

  @Test
  void noEventIfTheElevatorsOutOfServiceDidNotChange() throws Exception {
    poll(outOfService(A), inService(B));
    poll(outOfService(A));

    assertEquals(1, events.size());
    // The statuses changed, so they are written even if the elevators out of service did not
    assertEquals(List.of(outOfService(A)), List.copyOf(snapshot().listStatuses()));
  }

  @Test
  void noNewSnapshotIfTheStatusesDidNotChange() throws Exception {
    poll(outOfService(A), inService(B));
    var first = snapshot();

    poll(inService(B), outOfService(A));

    assertSame(first, snapshot());
  }

  @Test
  void aFailedUpdateIsRetriedOnTheNextPoll() throws Exception {
    failuresLeft = 1;
    poll(outOfService(A));

    assertEquals(List.of(), events);

    poll(outOfService(A));

    assertEquals(List.of(new ElevatorStatusChanged(Set.of(A))), events);
    assertEquals(Set.of(A), snapshot().outOfServiceIds());
  }

  @Test
  void anElevatorNotInTheGraphIsKeptInTheRepository() throws Exception {
    poll(outOfService("unknown"));

    assertEquals(Set.of("unknown"), snapshot().outOfServiceIds());
  }

  @Test
  void mergeDuplicates() {
    assertEquals(
      List.of(outOfService(A), inService(B)),
      ElevatorStatusUpdater.mergeDuplicates(
        List.of(inService(A), outOfService(A), inService(B), inService(B), inService(A))
      )
    );
  }

  private void poll(ElevatorStatus... statuses) throws Exception {
    source.statuses = List.of(statuses);
    subject.run();
    // Each writer runs one task at a time, so this waits for the tasks of the poll
    transitUpdateManager.submit(context -> {}).get();
    streetUpdateManager.submit(context -> {}).get();
  }

  private ElevatorStatusRepositorySnapshot snapshot() {
    return handle.repositorySnapshot(registry.scope());
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

  private class RecordingHandler
    implements EventHandler<ElevatorStatusChanged, ElevatorStatusRepository>
  {

    @Override
    public Class<ElevatorStatusChanged> eventType() {
      return ElevatorStatusChanged.class;
    }

    @Override
    public void handle(ElevatorStatusChanged event, ElevatorStatusRepository repository) {
      if (failuresLeft > 0) {
        failuresLeft--;
        throw new IllegalStateException("Handling the event failed");
      }
      events.add(event);
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
