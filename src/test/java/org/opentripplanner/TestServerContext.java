package org.opentripplanner;

import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.inspector.raster.TileRendererManager;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.RoutingService;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;

public class TestServerContext {

  private TestServerContext() {}

  /** Create a context for unit testing, using the default RouteRequest. */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TransitModel transitModel
  ) {
    transitModel.index();
    final RouterConfig routerConfig = RouterConfig.DEFAULT;
    DefaultServerRequestContext context = DefaultServerRequestContext.create(
      routerConfig.transitTuningConfig(),
      routerConfig.routingRequestDefaults(),
      routerConfig.streetRoutingTimeout(),
      new RaptorConfig<>(routerConfig.transitTuningConfig()),
      graph,
      new DefaultTransitService(transitModel),
      Metrics.globalRegistry,
      routerConfig.vectorTileLayers(),
      createWorldEnvelopeService(),
      routerConfig.flexConfig(),
      null,
      routerConfig.requestLogFile()
    );
    creatTransitLayerForRaptor(transitModel, routerConfig.transitTuningConfig());
    return context;
  }

  /** Static factory method to create a service for test purposes. */
  public static WorldEnvelopeService createWorldEnvelopeService() {
    return new DefaultWorldEnvelopeService(new DefaultWorldEnvelopeRepository());
  }

  public static Builder builder(Graph graph, TransitModel transitModel) {
    return new Builder(graph, transitModel);
  }

  public static class Builder {

    private final Graph graph;
    private final TransitModel transitModel;
    private RoutingResponse routingResult = null;
    private final Instant instant = OffsetDateTime.parse("2023-01-27T21:08:35+01:00").toInstant();

    public Builder(Graph graph, TransitModel transitModel) {
      this.graph = graph;
      this.transitModel = transitModel;
    }

    public Builder withRoutingResponse(List<Itinerary> routingResult) {
      this.routingResult =
        new RoutingResponse(
          new TripPlan(PlanTestConstants.A, PlanTestConstants.B, instant, routingResult),
          null,
          null,
          null,
          List.of(),
          new DebugTimingAggregator()
        );
      return this;
    }

    public OtpServerRequestContext build() {
      return new OtpServerRequestContext() {
        @Override
        public RouteRequest defaultRouteRequest() {
          return new RouteRequest();
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
          return new RoutingService() {
            @Override
            public RoutingResponse route(RouteRequest request) {
              return routingResult;
            }

            @Override
            public ViaRoutingResponse route(RouteViaRequest request) {
              return null;
            }
          };
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
          return null;
        }

        @Override
        public MeterRegistry meterRegistry() {
          return null;
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
          return null;
        }

        @Override
        public VectorTilesResource.LayersParameters<VectorTilesResource.LayerType> vectorTileLayers() {
          return null;
        }
      };
    }
  }
}
