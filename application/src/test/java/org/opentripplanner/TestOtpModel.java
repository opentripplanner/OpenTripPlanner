package org.opentripplanner;

import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.service.TimetableRepository;

public record TestOtpModel(
  Graph graph,
  StreetLimitationParameters streetLimitationParameters,
  TimetableRepository timetableRepository,
  FareServiceFactory fareServiceFactory
) {
  public TestOtpModel(Graph graph, TimetableRepository timetableRepository) {
    this(
      graph,
      new StreetLimitationParameters(),
      timetableRepository,
      new DefaultFareServiceFactory()
    );
  }

  public TestOtpModel(
    Graph graph,
    StreetLimitationParameters streetLimitationParameters,
    TimetableRepository timetableRepository
  ) {
    this(graph, streetLimitationParameters, timetableRepository, new DefaultFareServiceFactory());
  }

  public TestOtpModel(
    Graph graph,
    TimetableRepository timetableRepository,
    FareServiceFactory fareServiceFactory
  ) {
    this(graph, new StreetLimitationParameters(), timetableRepository, fareServiceFactory);
  }

  public TestOtpModel index() {
    timetableRepository.index();
    graph.index();
    return this;
  }
}
