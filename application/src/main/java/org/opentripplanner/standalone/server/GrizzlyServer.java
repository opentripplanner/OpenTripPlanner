package org.opentripplanner.standalone.server;

import static org.opentripplanner.framework.application.ApplicationShutdownSupport.addShutdownHook;
import static org.opentripplanner.framework.application.ApplicationShutdownSupport.removeShutdownHook;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.BindException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.server.ContainerFactory;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class GrizzlyServer {

  private static final Logger LOG = LoggerFactory.getLogger(GrizzlyServer.class);

  private static final int MIN_THREADS = 4;

  /**
   * Health probes and metrics scraping are infrequent and fast, a small pool is enough. It is
   * separate from the main pool, so they are answered even when the routing requests saturate it.
   */
  private static final int ACTUATOR_THREADS = 2;

  /** Where the debug client is on the classpath, it is put there by the Maven build. */
  private static final String DEBUG_CLIENT_INDEX = "client/index.html";

  /** The command line parameters, including things like port number and content directories. */
  private final CommandLineParameters params;
  private final Application app;

  @Nullable
  private final ActuatorEndpoint actuatorEndpoint;

  private final Duration httpTransactionTimeout;

  static {
    // Remove existing handlers attached to the j.u.l root logger (since SLF4J 1.6.5)
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    // Bridge j.u.l (used by Jersey) to the SLF4J root logger
    SLF4JBridgeHandler.install();
  }

  /**
   * Construct a Grizzly server with the given IoC injector and command line parameters.
   *
   * @param actuatorEndpoint the Actuator API served on its own port. It is {@code null} if the
   *                         Actuator API is served by the main {@code app}, or is disabled.
   */
  public GrizzlyServer(
    CommandLineParameters params,
    Application app,
    @Nullable ActuatorEndpoint actuatorEndpoint,
    Duration httpTransactionTimeout
  ) {
    this.params = params;
    this.app = app;
    this.actuatorEndpoint = actuatorEndpoint;
    this.httpTransactionTimeout = httpTransactionTimeout;
  }

  /**
   * Start the server, and the Actuator API server if it has its own port, and block until OTP is
   * shut down.
   */
  public void run() {
    LOG.info(
      "Starting OTP Grizzly server on port {} of interface {}",
      params.port,
      params.bindAddress
    );
    LOG.info("OTP server base directory is: {}", params.baseDirectory);
    HttpServer httpServer = createHttpServer();
    Optional<HttpServer> actuatorServer = createActuatorServer();
    List<HttpServer> servers = actuatorServer
      .map(it -> List.of(httpServer, it))
      .orElse(List.of(httpServer));

    // Add shutdown hook to gracefully shut down Grizzly. If no thread is returned then shutdown is already in progress.
    Optional<Thread> shutdownThread = addShutdownHook("grizzly-shutdown", () ->
      servers.forEach(HttpServer::shutdown)
    );
    if (!shutdownThread.isPresent()) {
      return;
    }

    /* RELINQUISH CONTROL TO THE SERVER THREAD */
    try {
      start(httpServer, params.port);
      if (actuatorServer.isPresent()) {
        LOG.info(
          "Starting Actuator API server on port {} of interface {}",
          actuatorEndpoint.port(),
          params.bindAddress
        );
        start(actuatorServer.get(), actuatorEndpoint.port());
      }
      LOG.info("Grizzly server running.");
      Thread.currentThread().join();
    } catch (BindException be) {
      LOG.error(be.getMessage());
    } catch (IOException ioe) {
      LOG.error("IO exception while starting server.");
    } catch (InterruptedException ie) {
      LOG.info("Interrupted, shutting down.");
    }

    shutdownThread.ifPresent(thread -> removeShutdownHook(thread));
    servers.forEach(HttpServer::shutdown);
  }

  /**
   * This function goes through roughly the same steps as Jersey's GrizzlyServerFactory, but we
   * instead construct an HttpServer and NetworkListener manually so we can set the number of
   * threads and other details.
   */
  private HttpServer createHttpServer() {
    HttpServer httpServer = new HttpServer();

    // Set up a pool of threads to handle incoming HTTP requests.
    // According to the Grizzly docs, setting the core and max pool size equal with no queue limit
    // will use a more efficient fixed-size thread pool implementation.
    // TODO we should probably use Grizzly async processing rather than tying up the HTTP handler threads.
    int nHandlerThreads = getMaxThreads();
    ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
      .setPoolName("grizzly")
      .setThreadFactory(Thread.ofPlatform().name("grizzly-", 0).factory())
      .setCorePoolSize(nHandlerThreads)
      .setMaxPoolSize(nHandlerThreads)
      .setQueueLimit(-1);

    if (OTPFeature.HttpResponseTimeMetrics.isOn()) {
      threadPoolConfig.getInitialMonitoringConfig().addProbes(GrizzlyQueueWaitProbe.INSTANCE);
    }

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
      var classLoader = GrizzlyServer.class.getClassLoader();
      if (!hasDebugClient(classLoader)) {
        LOG.warn(
          "The debug client is not on the classpath, so there is no debug UI at '/'. Maven builds " +
            "it in the package phase, not when the code is compiled. Run " +
            "'mvn package -DskipTests -pl client -am' to build it, or turn the DebugUi feature off " +
            "to silence this warning."
        );
      }
      CLStaticHttpHandler staticHandler = new CLStaticHttpHandler(classLoader, "/client/");
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

    return httpServer;
  }

  /**
   * A separate server is needed for the Actuator API, because Grizzly registers the handlers per
   * server, not per listener. It has its own small thread pool, so it is isolated from the routing
   * load, and serves nothing but the Actuator API.
   */
  private Optional<HttpServer> createActuatorServer() {
    if (actuatorEndpoint == null) {
      return Optional.empty();
    }
    HttpServer httpServer = new HttpServer();

    ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
      .setPoolName("grizzly-actuator")
      .setThreadFactory(Thread.ofPlatform().name("grizzly-actuator-", 0).factory())
      .setCorePoolSize(ACTUATOR_THREADS)
      .setMaxPoolSize(ACTUATOR_THREADS)
      .setQueueLimit(-1);

    NetworkListener httpListener = new NetworkListener(
      "otp_actuator",
      params.bindAddress,
      actuatorEndpoint.port()
    );
    httpListener.setSecure(false);
    httpListener.getTransport().setSelectorRunnersCount(1);
    httpListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
    httpListener.setTransactionTimeout((int) httpTransactionTimeout.toSeconds());
    httpServer.addListener(httpListener);

    HttpHandler actuatorHandler = ContainerFactory.createContainer(
      HttpHandler.class,
      actuatorEndpoint.app()
    );
    httpServer.getServerConfiguration().addHttpHandler(actuatorHandler, "/otp/");
    return Optional.of(httpServer);
  }

  private static void start(HttpServer server, int port) throws IOException {
    try {
      server.start();
    } catch (BindException e) {
      var bindException = new BindException(
        "Cannot bind to port " + port + ". Is it already in use? " + e.getMessage()
      );
      bindException.initCause(e);
      throw bindException;
    }
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
    if (params.maxThreads != null && params.maxThreads > 0) {
      maxThreads = params.maxThreads;
      LOG.info("Based on configuration, forced max thread pool size to {} threads.", maxThreads);
    } else if (maxThreads < MIN_THREADS) {
      // Some machines apparently report 1 processor even when they have 8.
      maxThreads = MIN_THREADS;
    }
    LOG.info("Maximum HTTP handler thread pool size will be {} threads.", maxThreads);
    return maxThreads;
  }

  /**
   * The Actuator API application and the port it is served on, when the Actuator API has its own
   * port, separate from the public APIs.
   */
  public record ActuatorEndpoint(Application app, int port) {}

  /**
   * The debug client is not checked in, it is built by Maven. When OTP is started from an IDE
   * without a package build it is missing, and requests for it get a 404 without any explanation.
   */
  static boolean hasDebugClient(ClassLoader classLoader) {
    return classLoader.getResource(DEBUG_CLIENT_INDEX) != null;
  }
}
