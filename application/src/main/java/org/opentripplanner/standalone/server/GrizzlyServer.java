package org.opentripplanner.standalone.server;

import static org.opentripplanner.framework.application.ApplicationShutdownSupport.addShutdownHook;
import static org.opentripplanner.framework.application.ApplicationShutdownSupport.removeShutdownHook;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.BindException;
import java.time.Duration;
import java.util.Optional;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.server.ContainerFactory;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class GrizzlyServer {

  private static final Logger LOG = LoggerFactory.getLogger(GrizzlyServer.class);

  private static final int MIN_THREADS = 4;
  /** The command line parameters, including things like port number and content directories. */
  private final CommandLineParameters params;
  private final Application app;
  private final Duration httpTransactionTimeout;

  static {
    // Remove existing handlers attached to the j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)
    // Bridge j.u.l (used by Jersey) to the SLF4J root logger
    SLF4JBridgeHandler.install();
  }

  /** Construct a Grizzly server with the given IoC injector and command line parameters. */
  public GrizzlyServer(
    CommandLineParameters params,
    Application app,
    Duration httpTransactionTimeout
  ) {
    this.params = params;
    this.app = app;
    this.httpTransactionTimeout = httpTransactionTimeout;
  }

  /**
   * This function goes through roughly the same steps as Jersey's GrizzlyServerFactory, but we
   * instead construct an HttpServer and NetworkListener manually so we can set the number of
   * threads and other details.
   */
  public void run() {
    LOG.info(
      "Starting OTP Grizzly server on port {} of interface {}",
      params.port,
      params.bindAddress
    );
    LOG.info("OTP server base directory is: {}", params.baseDirectory);
    HttpServer httpServer = new HttpServer();

    // Set up a pool of threads to handle incoming HTTP requests.
    // According to the Grizzly docs, setting the core and max pool size equal with no queue limit
    // will use a more efficient fixed-size thread pool implementation.
    // TODO we should probably use Grizzly async processing rather than tying up the HTTP handler threads.
    int nHandlerThreads = getMaxThreads();
    ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
      .setPoolName("grizzly")
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("grizzly-%d").build())
      .setCorePoolSize(nHandlerThreads)
      .setMaxPoolSize(nHandlerThreads)
      .setQueueLimit(-1);

    /* HTTP (non-encrypted) listener */
    NetworkListener httpListener = new NetworkListener(
      "otp_insecure",
      params.bindAddress,
      params.port
    );
    httpListener.setSecure(false);

    // For the HTTP listener: enable gzip compression, set thread pool, add listener to httpServer.
    CompressionConfig cc = httpListener.getCompressionConfig();
    cc.setCompressionMode(CompressionConfig.CompressionMode.ON);
    // the min number of bytes to compress
    cc.setCompressionMinSize(50000);
    // the mime types to compress
    cc.setCompressibleMimeTypes(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
    httpListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
    httpListener.setTransactionTimeout((int) httpTransactionTimeout.toSeconds());
    httpServer.addListener(httpListener);

    /* Add a few handlers (~= servlets) to the Grizzly server. */

    /* 1. A Grizzly wrapper around the Jersey Application. */
    HttpHandler dynamicHandler = ContainerFactory.createContainer(HttpHandler.class, app);
    httpServer.getServerConfiguration().addHttpHandler(dynamicHandler, "/otp/");

    /* 2. A static content handler to serve the client JS apps etc. from the classpath. */
    if (OTPFeature.DebugUi.isOn()) {
      CLStaticHttpHandler staticHandler = new CLStaticHttpHandler(
        GrizzlyServer.class.getClassLoader(),
        "/client/"
      );
      if (params.disableFileCache) {
        LOG.info("Disabling HTTP server static file cache.");
        staticHandler.setFileCacheEnabled(false);
      }
      httpServer.getServerConfiguration().addHttpHandler(staticHandler, "/");
    }

    /* 3. A static content handler to serve local files from the filesystem, under the "local" path. */
    if (params.clientDirectory != null) {
      StaticHttpHandler localHandler = new StaticHttpHandler(
        params.clientDirectory.getAbsolutePath()
      );
      localHandler.setFileCacheEnabled(false);
      httpServer.getServerConfiguration().addHttpHandler(localHandler, "/local");
    }

    /* 3. Test alternate HTTP handling without Jersey. */
    // As in servlets, * is needed in base path to identify the "rest" of the path.
    // GraphService gs = (GraphService) iocFactory.getComponentProvider(GraphService.class).getInstance();
    // Graph graph = gs.getGraph();
    // httpServer.getServerConfiguration().addHttpHandler(new OTPHttpHandler(graph), "/test/*");

    // Add shutdown hook to gracefully shut down Grizzly. If no thread is returned then shutdown is already in progress.
    Optional<Thread> shutdownThread = addShutdownHook("grizzly-shutdown", httpServer::shutdown);
    if (!shutdownThread.isPresent()) {
      return;
    }

    /* RELINQUISH CONTROL TO THE SERVER THREAD */
    try {
      httpServer.start();
      LOG.info("Grizzly server running.");
      Thread.currentThread().join();
    } catch (BindException be) {
      LOG.error("Cannot bind to port {}. Is it already in use?", params.port);
    } catch (IOException ioe) {
      LOG.error("IO exception while starting server.");
    } catch (InterruptedException ie) {
      LOG.info("Interrupted, shutting down.");
    }

    shutdownThread.ifPresent(thread -> removeShutdownHook(thread));
    httpServer.shutdown();
  }

  /**
   * OTP is CPU-bound, so we want roughly as many worker threads as we have cores, subject to some
   * constraints.
   */
  private int getMaxThreads() {
    int maxThreads = Runtime.getRuntime().availableProcessors();
    LOG.info("Java reports that this machine has {} available processors.", maxThreads);
    // Testing shows increased throughput up to 1.25x as many threads as cores
    maxThreads *= 1.25;
    if (params.maxThreads != null) {
      maxThreads = params.maxThreads;
      LOG.info("Based on configuration, forced max thread pool size to {} threads.", maxThreads);
    }
    if (maxThreads < MIN_THREADS) {
      // Some machines apparently report 1 processor even when they have 8.
      maxThreads = MIN_THREADS;
    }
    LOG.info("Maximum HTTP handler thread pool size will be {} threads.", maxThreads);
    return maxThreads;
  }
}
