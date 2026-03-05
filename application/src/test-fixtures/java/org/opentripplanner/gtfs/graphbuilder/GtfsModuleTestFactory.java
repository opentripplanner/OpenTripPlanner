package org.opentripplanner.gtfs.graphbuilder;

import java.util.List;
import org.opentripplanner.core.model.time.LocalDateInterval;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareServiceFactory;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.TimetableRepository;

public class GtfsModuleTestFactory {

  public static GtfsModule forTest(
    List<GtfsBundle> bundles,
    TimetableRepository timetableRepository,
    Graph graph,
    LocalDateInterval transitPeriodLimit
  ) {
    return new GtfsModule(
      bundles,
      timetableRepository,
      new DefaultStreetDetailsRepository(),
      graph,
      new Deduplicator(),
      DataImportIssueStore.NOOP,
      transitPeriodLimit,
      new DefaultFareServiceFactory(),
      150.0,
      120
    );
  }
}
