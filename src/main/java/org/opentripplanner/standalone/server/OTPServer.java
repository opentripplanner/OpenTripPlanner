package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerContext;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This replaces a Spring application context, which OTP originally used. It contains a field
 * referencing each top-level component of an OTP server. This means that supplying a single
 * instance of this object allows accessing any of the other OTP components.
 * TODO OTP2 refactor: rename to OTPContext or OTPComponents and use to draft injection approach
 */
public class OTPServer implements OtpServerContext {

  private static final Logger LOG = LoggerFactory.getLogger(OTPServer.class);

  public final CommandLineParameters params;

  private final Router router;

  public OTPServer(CommandLineParameters params, Router router) {
    LOG.info("Wiring up and configuring server.");
    this.params = params;
    this.router = router;
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
  }

  /**
   * This method is used to create a {@link RoutingService} valid for one request. It grantees that
   * the data and services used are consistent and operate on the same transit snapshot. Any
   * realtime update that happens during the request will not affect the returned service and will
   * not be visible to the request.
   */
  public RoutingService createRoutingRequestService() {
    return new RoutingService(router.graph(), router.transitModel());
  }

  public TransitService createTransitRequestService() {
    return new DefaultTransitService(router.transitModel());
  }

  @Override
  public RoutingRequest copyDefaultRoutingRequest() {
    return router.copyDefaultRoutingRequest();
  }

  @Override
  public Locale getDefaultLocale() {
    return router.getDefaultLocale();
  }

  @Override
  public double streetRoutingTimeoutSeconds() {
    return router.streetRoutingTimeoutSeconds();
  }

  @Override
  public Graph graph() {
    return router.graph();
  }

  @Override
  public TransitModel transitModel() {
    return router.transitModel();
  }

  @Override
  public RouterConfig routerConfig() {
    return router.routerConfig();
  }

  @Override
  public MeterRegistry meterRegistry() {
    return router.meterRegistry();
  }

  @Override
  public RaptorConfig<TripSchedule> raptorConfig() {
    return router.raptorConfig();
  }

  @Override
  public Logger requestLogger() {
    return router.requestLogger();
  }

  @Override
  public TileRendererManager tileRendererManager() {
    return router.tileRendererManager();
  }

  @Override
  public GraphVisualizer graphVisualizer() {
    return router.graphVisualizer();
  }

  /**
   * Return an HK2 Binder that injects this specific OTPServer instance into Jersey web resources.
   * This should be registered in the ResourceConfig (Jersey) or Application (JAX-RS) as a
   * singleton. Jersey forces us to use injection to get application context into HTTP method
   * handlers, but in OTP we always just inject this OTPServer instance and grab anything else we
   * need (routers, graphs, application components) from this single object.
   * <p>
   * More on custom injection in Jersey 2: http://jersey.576304.n2.nabble.com/Custom-providers-in-Jersey-2-tp7580699p7580715.html
   */
  AbstractBinder makeBinder() {
    return new AbstractBinder() {
      @Override
      protected void configure() {
        bind(OTPServer.this).to(OtpServerContext.class);
      }
    };
  }

  /**
   * Hook to cleanup various stuff of some used libraries (org.geotools), which depend on the
   * external client to call them for cleaning-up.
   */
  private static void cleanupWebapp() {
    LOG.info("Web application shutdown: cleaning various stuff");
    WeakCollectionCleaner.DEFAULT.exit();
    DeferredAuthorityFactory.exit();
  }

  private void shutdownHook() {
    router.shutdown();
    cleanupWebapp();
  }
}
