package org.opentripplanner.ext.legacygraphqlapi;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.inspector.raster.TileRendererManager;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;

public class TestServerRequestContext implements OtpServerRequestContext {

  private final DefaultTransitService transitService;
  private final Graph graph = new Graph();

  public TestServerRequestContext() {
    graph
      .getVehicleParkingService()
      .updateVehicleParking(
        List.of(
          VehicleParking
            .builder()
            .id(TransitModelForTest.id("parking-1"))
            .name(NonLocalizedString.ofNullable("parking"))
            .build()
        ),
        List.of()
      );
    var transitModel = new TransitModel();
    transitModel.index();
    transitModel.getTransitModelIndex().addRoutes(TransitModelForTest.route("123").build());
    transitService = new DefaultTransitService(transitModel);
  }

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
    return RaptorConfig.defaultConfigForTest();
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Override
  public RoutingService routingService() {
    return new RoutingService(this);
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
    return FlexConfig.DEFAULT;
  }

  @Override
  public VectorTilesResource.LayersParameters<VectorTilesResource.LayerType> vectorTileLayers() {
    return null;
  }
}
