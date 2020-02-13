package org.opentripplanner.standalone;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opentripplanner.routing.impl.GraphLoader;
import org.opentripplanner.standalone.config.GraphConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphService {

  private static final Logger LOG = LoggerFactory.getLogger(GraphService.class);
  private final GraphConfig config;
  private Router currentRouter;
  private ScheduledExecutorService scanner = Executors.newSingleThreadScheduledExecutor();
  private WatchService watchService;

  public GraphService(GraphConfig config, boolean autoReload) {
    this.config = config;
    if (autoReload) {
      Path graphDirPath = config.getPath().toPath();
      try {
        watchService = graphDirPath.getFileSystem().newWatchService();
        graphDirPath.register(watchService, ENTRY_MODIFY);
        scanner.scheduleWithFixedDelay(() -> {
          this.scan();
        }, 60, 30, TimeUnit.SECONDS);
      } catch (IOException e) {
        LOG.info("Failed to register watch ", e);
      }
    }
  }

  private void scan() {
    WatchKey key = this.watchService.poll();
    if (key != null) {
      key.pollEvents().stream().filter(event -> event.context().toString().endsWith(".obj"))
          .findFirst().ifPresent(event -> {
        Router newRouter = this.load();
        if (newRouter != null) {
          LOG.info("Shutting down existing router and will switch to new router after done");
          currentRouter.shutdown();
          this.currentRouter = newRouter;
        } else {
          LOG.info("Failed to load new graph will try to reload in next round");
        }
      });
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
