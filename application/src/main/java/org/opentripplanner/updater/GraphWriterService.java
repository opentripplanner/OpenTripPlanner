package org.opentripplanner.updater;

import java.util.concurrent.Future;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialises all graph write operations by delegating to {@link UpdateManager}, which owns the
 * single-threaded executor. Each write task receives a freshly-constructed
 * {@link DefaultRealTimeUpdateContext} backed by the mutable timetable snapshot for that task.
 * <p>
 * This class will eventually be removed once all updaters submit directly to {@link UpdateManager}.
 */
public class GraphWriterService implements WriteToGraphCallback {

  private static final Logger LOG = LoggerFactory.getLogger(GraphWriterService.class);

  private final UpdateManager updateManager;
  private final RepositoryHandle<
    ReadOnlyTimetableSnapshot,
    MutableTimetableSnapshot
  > timetableHandle;
  private final Graph graph;
  private final TimetableRepository timetableRepository;

  public GraphWriterService(
    UpdateManager updateManager,
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableHandle,
    Graph graph,
    TimetableRepository timetableRepository
  ) {
    this.updateManager = updateManager;
    this.timetableHandle = timetableHandle;
    this.graph = graph;
    this.timetableRepository = timetableRepository;
  }

  @Override
  public Future<Void> execute(GraphWriterRunnable runnable) {
    return updateManager.submit(ctx -> {
      var mutableSnapshot = ctx.repository(timetableHandle);
      var context = new DefaultRealTimeUpdateContext(graph, timetableRepository, mutableSnapshot);
      try {
        runnable.run(context);
      } catch (Exception e) {
        LOG.error("Error while running graph writer {}:", runnable.getClass().getName(), e);
      }
    });
  }

  public void stop() {
    updateManager.shutdown();
  }
}
