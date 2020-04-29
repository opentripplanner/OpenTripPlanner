package org.opentripplanner.standalone;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner.annotation.ComponentAnnotationConfigurator;
import org.opentripplanner.routing.impl.GraphLoader;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphService {

  private static final Logger LOG = LoggerFactory.getLogger(GraphService.class);
  private final OTPAppConstruction app;
  private Router currentRouter;
  private ScheduledExecutorService scanner = Executors.newSingleThreadScheduledExecutor();
  private AtomicLong lastModified = new AtomicLong();

  public GraphService(OTPAppConstruction app) {
    this.app = app;
    ComponentAnnotationConfigurator.getInstance().fromConfig(app.config().buildConfig().getRawJson());
    if (app.config().getCli().autoReload) {
      lastModified.set(app.store().getGraph().lastModified());
      scanner.scheduleWithFixedDelay(() -> this.scan(), 60, 10, TimeUnit.SECONDS);
    }
  }

  private void scan() {
    long latestLastModified = app.store().getGraph().lastModified();
    if (lastModified.getAndSet(latestLastModified) != latestLastModified) {
      Router newRouter = this.load();
      if (newRouter != null) {
        LOG.info("Shutting down existing router and will switch to new router after done");
        currentRouter.shutdown();
        this.currentRouter = newRouter;
        LOG.info("New Graph loaded");
      } else {
        LOG.info("Failed to load new graph will try to reload in next round");
      }
    }
  }

  public Router getRouter() {
    return currentRouter;
  }

  public Router load() {
    currentRouter = GraphLoader.loadGraph(app);
    return currentRouter;
  }
}
