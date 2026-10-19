package org.opentripplanner;

import org.opentripplanner.ext.fares.service.gtfs.v1.GtfsFareServiceFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.repository.ScheduledTimetableData;
import org.opentripplanner.transit.service.TransitRepository;

public record TestOtpModel(
  Graph graph,
  TransitRepository transitRepository,
  TimetableDataImport timetableDataImport,
  TransferRepository transferRepository,
  FareServiceFactory fareServiceFactory
) {
  public TestOtpModel(
    Graph graph,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport,
    TransferRepository transferRepository
  ) {
    this(
      graph,
      transitRepository,
      timetableDataImport,
      transferRepository,
      new GtfsFareServiceFactory()
    );
  }

  /** The scheduled data built so far, converted as the server would do it on startup. */
  public ScheduledTimetableData scheduledTimetableData() {
    return timetableDataImport.toScheduledTimetableData();
  }

  public TestOtpModel index() {
    transitRepository.index();
    transferRepository.index();
    graph.index();
    return this;
  }
}
