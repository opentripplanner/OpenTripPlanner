package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.inspector.raster.TileRendererManager;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;

public class TestServerRequestContext implements OtpServerRequestContext {

  private final Graph graph;
  private final TransitModel transitModel;

  private final RouterConfig routerConfig = RouterConfig.DEFAULT;

  private final TestRoutingService routingService = new TestRoutingService();

  public TestServerRequestContext(Graph graph, TransitModel transitModel) {
    this.graph = graph;
    this.transitModel = transitModel;
  }

  public void setRoutingResult(List<Itinerary> i1) {
    routingService.setRoutingResponse(i1);
  }

  @Override
  public RouteRequest defaultRouteRequest() {
    return routerConfig.routingRequestDefaults();
  }

  @Override
  public Locale defaultLocale() {
    return Locale.ENGLISH;
  }

  @Override
  public RaptorConfig<TripSchedule> raptorConfig() {
    return null;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public TransitService transitService() {
    return new DefaultTransitService(transitModel);
  }

  @Override
  public RoutingService routingService() {
    return routingService;
  }

  @Override
  public WorldEnvelopeService worldEnvelopeService() {
    return null;
  }

  @Override
  public TransitTuningParameters transitTuningParameters() {
    return null;
  }

  @Override
  public RaptorTuningParameters raptorTuningParameters() {
    return null;
  }

  @Override
  public Duration streetRoutingTimeout() {
    return routerConfig.streetRoutingTimeout();
  }

  @Override
  public MeterRegistry meterRegistry() {
    return Metrics.globalRegistry;
  }

  @Override
  public Logger requestLogger() {
    return null;
  }

  @Override
  public TileRendererManager tileRendererManager() {
    return null;
  }

  @Override
  public TraverseVisitor<State, Edge> traverseVisitor() {
    return null;
  }

  @Override
  public FlexConfig flexConfig() {
    return routerConfig.flexConfig();
  }

  @Override
  public VectorTilesResource.LayersParameters<VectorTilesResource.LayerType> vectorTileLayers() {
    return null;
  }
}
