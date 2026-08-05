package org.opentripplanner.updater;

import java.util.concurrent.Future;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialises all graph write operations by delegating to {@link UpdateManager}, which owns the
 * single-threaded executor. Each write task receives a freshly-constructed
 * {@link DefaultRealTimeUpdateContext} backed by the mutable realtime-timetable repository for
 * that task.
 * <p>
 * This class will eventually be removed once all updaters submit directly to {@link UpdateManager}.
 */
public class GraphWriterService implements WriteToGraphCallback {

  private static final Logger LOG = LoggerFactory.getLogger(GraphWriterService.class);

  private final UpdateManager updateManager;
  private final RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableHandle;
  private final Graph graph;
  private final TransitRepository transitRepository;

  public GraphWriterService(
    UpdateManager updateManager,
    RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableHandle,
    Graph graph,
    TransitRepository transitRepository
  ) {
    this.updateManager = updateManager;
    this.timetableHandle = timetableHandle;
    this.graph = graph;
    this.transitRepository = transitRepository;
  }

  @Override
  public Future<Void> execute(GraphWriterRunnable runnable) {
    return updateManager.submit(ctx -> {
      var repository = ctx.repository(timetableHandle);
      var context = new DefaultRealTimeUpdateContext(graph, transitRepository, repository);
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
