package org.opentripplanner;

import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareServiceFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TimetableRepository;

public record TestOtpModel(
  Graph graph,
  TimetableRepository timetableRepository,
  FareServiceFactory fareServiceFactory
) {
  public TestOtpModel(Graph graph, TimetableRepository timetableRepository) {
    this(graph, timetableRepository, new DefaultFareServiceFactory());
  }

  public TestOtpModel index() {
    timetableRepository.index();
    graph.index();
    return this;
  }
}
