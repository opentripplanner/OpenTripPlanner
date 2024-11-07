package org.opentripplanner;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TimetableRepository;

public record TestOtpModel(Graph graph, TimetableRepository timetableRepository) {
  public TestOtpModel index() {
    timetableRepository.index();
    graph.index(timetableRepository.getSiteRepository());
    return this;
  }
}
