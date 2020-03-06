package org.opentripplanner.standalone;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner.routing.impl.GraphLoader;
import org.opentripplanner.standalone.config.GraphConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphService {

  private static final Logger LOG = LoggerFactory.getLogger(GraphService.class);
  private final GraphConfig config;
  private Router currentRouter;
  private ScheduledExecutorService scanner = Executors.newSingleThreadScheduledExecutor();
  private AtomicLong lastModified = new AtomicLong();
  private File graphFile;

  public GraphService(GraphConfig config, boolean autoReload) {
    this.config = config;
    graphFile = new File(config.getPath(), "Graph.obj");
    if (autoReload) {
      lastModified.set(graphFile.lastModified());
      scanner.scheduleWithFixedDelay(() -> this.scan(), 60, 10, TimeUnit.SECONDS);
    }
  }

  private void scan() {
    long latestLastModified = graphFile.lastModified();
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
    currentRouter = GraphLoader.loadGraph(config);
    return currentRouter;
  }


}
