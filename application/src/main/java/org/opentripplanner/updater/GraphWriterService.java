package org.opentripplanner.updater;

import java.util.concurrent.Future;
import java.util.function.Function;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.WriteContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialises the graph write operations of one write domain by delegating to that domain's
 * {@link UpdateManager}, which owns the single-threaded executor. Each write task receives the
 * update context of its domain: transit tasks get access to the mutable realtime-timetable
 * repository, street tasks get access to the street model.
 * <p>
 * This class will eventually be removed once all updaters submit directly to {@link UpdateManager}.
 *
 * @param <C> the update context of this domain, see {@link GraphWriterRunnable}
 */
public class GraphWriterService<C> implements WriteToGraphCallback<C> {

  private static final Logger LOG = LoggerFactory.getLogger(GraphWriterService.class);

  private final UpdateManager updateManager;
  private final Function<WriteContext, C> contextFactory;

  private GraphWriterService(
    UpdateManager updateManager,
    Function<WriteContext, C> contextFactory
  ) {
    this.updateManager = updateManager;
    this.contextFactory = contextFactory;
  }

  /**
   * Create the bridge for the transit write domain. Each task checks out the mutable
   * realtime-timetable repository for the current transaction.
   */
  public static GraphWriterService<TransitRealTimeUpdateContext> forTransitDomain(
    UpdateManager updateManager,
    RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableHandle,
    TransitRepository transitRepository
  ) {
    return new GraphWriterService<>(updateManager, ctx ->
      new DefaultTransitRealTimeUpdateContext(transitRepository, ctx.repository(timetableHandle))
    );
  }

  /**
   * Create the bridge for the street write domain.
   */
  public static GraphWriterService<StreetRealTimeUpdateContext> forStreetDomain(
    UpdateManager updateManager,
    Graph graph
  ) {
    var context = new DefaultStreetRealTimeUpdateContext(graph);
    return new GraphWriterService<>(updateManager, ctx -> context);
  }

  @Override
  public Future<Void> execute(GraphWriterRunnable<C> runnable) {
    return updateManager.submit(ctx -> {
      var context = contextFactory.apply(ctx);
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
