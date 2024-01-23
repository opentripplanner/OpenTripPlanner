package org.opentripplanner;

import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;

import io.micrometer.core.instrument.Metrics;
import java.util.List;
import org.opentripplanner.ext.emissions.DefaultEmissionsService;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.framework.lang.ObjectUtils;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

public class TestServerContextBuilder {

  private Graph graph;
  private TransitModel transitModel;
  private TransitService transitService;

  private TestServerContextBuilder() {}

  public static TestServerContextBuilder of() {
    return new TestServerContextBuilder();
  }

  public TestServerContextBuilder withGraph(Graph graph) {
    this.graph = ObjectUtils.requireNotInitialized(this.graph, graph);
    return this;
  }

  public Graph graph() {
    if (graph == null) {
      this.graph = new Graph();
    }
    return graph;
  }

  public TestServerContextBuilder withTransitModel(TransitModel transitModel) {
    this.transitModel = ObjectUtils.requireNotInitialized(this.transitModel, transitModel);
    return this;
  }

  private TransitModel transitModel() {
    if (transitModel == null) {
      this.transitModel = new TransitModel();
    }

    return transitModel;
  }

  public TransitService transitService() {
    if (transitService == null) {
      this.transitService = new DefaultTransitService(transitModel());
    }
    return transitService;
  }

  /** Create a context for unit testing, using the default RouteRequest. */
  public OtpServerRequestContext serverContext() {
    transitModel().index();
    final RouterConfig routerConfig = RouterConfig.DEFAULT;
    DefaultServerRequestContext context = DefaultServerRequestContext.create(
      routerConfig.transitTuningConfig(),
      routerConfig.routingRequestDefaults(),
      new RaptorConfig<>(routerConfig.transitTuningConfig()),
      graph(),
      new DefaultTransitService(transitModel()),
      Metrics.globalRegistry,
      routerConfig.vectorTileLayers(),
      createWorldEnvelopeService(),
      createRealtimeVehicleService(),
      createVehicleRentalService(),
      createEmissionsService(),
      routerConfig.flexConfig(),
      List.of(),
      null,
      null
    );
    creatTransitLayerForRaptor(transitModel(), routerConfig.transitTuningConfig());
    return context;
  }

  /** Static factory method to create a service for test purposes. */
  public WorldEnvelopeService createWorldEnvelopeService() {
    return new DefaultWorldEnvelopeService(new DefaultWorldEnvelopeRepository());
  }

  public RealtimeVehicleService createRealtimeVehicleService() {
    return new DefaultRealtimeVehicleService(transitService());
  }

  public VehicleRentalService createVehicleRentalService() {
    return new DefaultVehicleRentalService();
  }

  public static EmissionsService createEmissionsService() {
    return new DefaultEmissionsService(new EmissionsDataModel());
  }
}
