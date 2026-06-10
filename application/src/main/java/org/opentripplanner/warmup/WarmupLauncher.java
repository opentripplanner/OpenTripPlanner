package org.opentripplanner.warmup;

import javax.annotation.Nullable;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.warmup.api.WarmupParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches the application warmup background thread.
 * <p>
 * Injected dependencies come from the Dagger {@link
 * org.opentripplanner.warmup.configure.WarmupModule}. The launcher decides whether a warmup run
 * is applicable (parameters present, updaters present, selected API enabled) and, when it is,
 * starts a daemon thread running a {@link WarmupWorker}.
 */
public class WarmupLauncher {

  private static final Logger LOG = LoggerFactory.getLogger(WarmupLauncher.class);

  @Nullable
  private final WarmupParameters parameters;

  private final OtpServerRequestContext serverContext;
  private final TimetableRepository timetableRepository;

  public WarmupLauncher(
    @Nullable WarmupParameters parameters,
    OtpServerRequestContext serverContext,
    TimetableRepository timetableRepository
  ) {
    this.parameters = parameters;
    this.serverContext = serverContext;
    this.timetableRepository = timetableRepository;
  }

  /**
   * Start the application warmup thread if configured and applicable.
   * <p>
   * No warmup is started if parameters are null (warmup section absent in router-config.json),
   * if no updaters are configured (health probe would immediately return "UP"), or if the
   * selected API schema is not available.
   */
  public void start() {
    if (parameters == null) {
      return;
    }
    GraphUpdaterManager updaterManager = timetableRepository.getUpdaterManager();
    if (updaterManager == null) {
      LOG.info("Application warmup configured but no updaters found. Skipping warmup.");
      return;
    }
    var schema = switch (parameters.api()) {
      case TRANSMODEL -> serverContext.transmodelSchema();
      case GTFS -> serverContext.gtfsSchema();
    };
    if (schema == null) {
      LOG.warn(
        "Application warmup configured for {} API, but the schema is not available. " +
          "Is the corresponding API feature enabled?",
        parameters.api()
      );
      return;
    }
    var worker = new WarmupWorker(parameters, serverContext, () -> updaterManager);
    var thread = new Thread(worker, "app-warmup");
    thread.setDaemon(true);
    thread.start();
    LOG.info("Application warmup thread started.");
  }
}
