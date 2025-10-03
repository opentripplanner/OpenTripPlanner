package org.opentripplanner.standalone.api;

import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.trias.parameters.TriasApiParameters;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;

/**
 * The purpose of this class is to give APIs (HTTP Resources) read-only access to the OTP internal
 * transit model. It allows individual API requests to use a limited number of methods and data
 * structures without direct access to the internals of the server components.
 *
 * By using an interface, and not injecting each service class we avoid giving the resources access
 * to the server implementation. The context is injected by Jersey. Instead of injecting this
 * context interface, it is conceivable to inject each of the individual items within this context.
 *
 * But there is not a "real" need for this. For example, we do not have unit tests on the
 * Resources. If we in the future would decide to write unit tests for the APIs, then we could
 * eliminate this interface and just inject the components. See the bind method in OTPServer.
 * <p>
 * The OTP Server and the implementation of this class is responsible to make the necessary
 * instances for request scoped objects. Hence; the user of this can assume that:
 * <ol>
 *   <li>Calling the methods of this interface will return the same object every time.</li>
 *   <li>
 *     If the returned component is mutable, then it will be a unique copy for each HTTP request.
 *   </li>
 * </ol>
 * <p>
 * This class is not THREAD-SAFE, each HTTP request gets its own copy, but if there are multiple
 * threads which accesses this context within the HTTP Request, then the caller is responsible
 * for the synchronization. Only request scoped components need to be synchronized - they are
 * potentially lazy initialized.
 */
@HttpRequestScoped
public interface OtpServerRequestContext {
  DebugUiConfig debugUiConfig();

  /**
   * A RouteRequest containing default parameters that will be cloned when handling each request.
   */
  @HttpRequestScoped
  RouteRequest defaultRouteRequest();

  RaptorConfig<TripSchedule> raptorConfig();

  Graph graph();

  @HttpRequestScoped
  TransitService transitService();

  /**
   * Get a request-scoped {@link RoutingService} valid for one HTTP request. It guarantees that
   * the data and services used are consistent and operate on the same transit snapshot. Any
   * realtime update that happens during the request will not affect the returned service and will
   * not be visible to the request.
   */
  @HttpRequestScoped
  RoutingService routingService();

  /**
   * Get information on geographical bounding box and center coordinates.
   */
  WorldEnvelopeService worldEnvelopeService();

  RealtimeVehicleService realtimeVehicleService();

  VehicleRentalService vehicleRentalService();

  VehicleParkingService vehicleParkingService();

  TransitTuningParameters transitTuningParameters();

  RaptorTuningParameters raptorTuningParameters();

  List<RideHailingService> rideHailingServices();

  StreetLimitationParametersService streetLimitationParametersService();

  MeterRegistry meterRegistry();

  /**
   * Callback which is injected into the {@code DirectStreetRouter}, used to visualize the
   * search.
   */
  @HttpRequestScoped
  TraverseVisitor<State, Edge> traverseVisitor();

  default GraphFinder graphFinder() {
    return GraphFinder.getInstance(
      graph(),
      vertexLinker(),
      transitService()::findRegularStopsByBoundingBox
    );
  }

  FlexParameters flexParameters();

  VectorTileConfig vectorTileConfig();

  ViaCoordinateTransferFactory viaTransferResolver();

  TriasApiParameters triasApiParameters();

  GtfsApiParameters gtfsApiParameters();

  TransmodelAPIParameters transmodelAPIParameters();

  /* Sandbox modules */

  @Nullable
  default List<ExtensionRequestContext> listExtensionRequestContexts(RouteRequest request) {
    var list = new ArrayList<ExtensionRequestContext>();
    if (OTPFeature.DataOverlay.isOn()) {
      list.add(
        new DataOverlayContext(
          graph().dataOverlayParameterBindings,
          request.preferences().system().dataOverlay()
        )
      );
    }
    return list;
  }

  @Nullable
  ItineraryDecorator emissionItineraryDecorator();

  @Nullable
  EmpiricalDelayService empiricalDelayService();

  @Nullable
  LuceneIndex lucenceIndex();

  @Nullable
  StopConsolidationService stopConsolidationService();

  @Nullable
  SorlandsbanenNorwayService sorlandsbanenService();

  @Nullable
  GraphQLSchema gtfsSchema();

  @Nullable
  GraphQLSchema transmodelSchema();

  FareService fareService();

  VertexLinker vertexLinker();
}
