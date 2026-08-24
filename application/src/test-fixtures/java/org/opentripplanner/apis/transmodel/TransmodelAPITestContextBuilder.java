package org.opentripplanner.apis.transmodel;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.TransitTestEnvironment;

public class TransmodelAPITestContextBuilder {

  private final TransitTestEnvironment transitTestEnvironment;
  private Graph graph = null;
  private TransferRepository transferRepository = null;
  private RouteRequest defaultRequest = null;

  private TransmodelAPITestContextBuilder(TransitTestEnvironment transitTestEnvironment) {
    this.transitTestEnvironment = transitTestEnvironment;
  }

  public static TransmodelAPITestContextBuilder of(TransitTestEnvironment transitTestEnvironment) {
    return new TransmodelAPITestContextBuilder(transitTestEnvironment);
  }

  private Graph graph() {
    if (graph == null) {
      graph = new Graph();
    }
    return graph;
  }

  public TransmodelAPITestContextBuilder withGraph(Graph graph) {
    this.graph = graph;
    return this;
  }

  public TransmodelAPITestContextBuilder withTransferRepository(
    TransferRepository transferRepository
  ) {
    this.transferRepository = transferRepository;
    return this;
  }

  public TransferRepository transferRepository() {
    return transferRepository;
  }

  private RouteRequest defaultRequest() {
    if (defaultRequest == null) {
      defaultRequest = RouteRequest.of().buildDefault();
    }
    return defaultRequest;
  }

  public TransmodelAPITestContextBuilder withDefaultRequest(RouteRequest defaultRequest) {
    this.defaultRequest = defaultRequest;
    return this;
  }

  public TransmodelRequestContext build() {
    var transitService = transitTestEnvironment.transitService();
    var transferRepository = transferRepository();
    var graph = graph();

    var vertexLinker = VertexLinkerTestFactory.of(graph);

    return new TestTransmodelRequestContext(
      TestServerContext.createRoutingService(graph, transitService, transferRepository),
      transitService,
      null,
      null,
      defaultRequest(),
      TestServerContext.createVehicleRentalService(),
      TestServerContext.createVehicleParkingService(),
      graph,
      TransferServiceTestFactory.transferService(transferRepository),
      TestServerContext.createStreetDetailsService(),
      TestServerContext.createLinkingContextFactory(graph, vertexLinker, transitService),
      TestServerContext.createStreetLimitationParametersService()
    );
  }
}
