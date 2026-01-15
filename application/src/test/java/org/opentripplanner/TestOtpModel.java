package org.opentripplanner;

import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareServiceFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public record TestOtpModel(
  Graph graph,
  TimetableRepository timetableRepository,
  TransferRepository transferRepository,
  FareServiceFactory fareServiceFactory
) {
  public TestOtpModel(
    Graph graph,
    TimetableRepository timetableRepository,
    TransferRepository transferRepository
  ) {
    this(graph, timetableRepository, transferRepository, new DefaultFareServiceFactory());
  }

  public TestOtpModel index() {
    timetableRepository.index();
    transferRepository.index();
    graph.index();
    return this;
  }
}
