package org.opentripplanner;

import org.opentripplanner.ext.fares.service.gtfs.v1.GtfsFareServiceFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.service.TransitRepository;

public record TestOtpModel(
  Graph graph,
  TransitRepository transitRepository,
  TransferRepository transferRepository,
  FareServiceFactory fareServiceFactory
) {
  public TestOtpModel(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository
  ) {
    this(graph, transitRepository, transferRepository, new GtfsFareServiceFactory());
  }

  public TestOtpModel index() {
    transitRepository.index();
    transferRepository.index();
    graph.index();
    return this;
  }
}
