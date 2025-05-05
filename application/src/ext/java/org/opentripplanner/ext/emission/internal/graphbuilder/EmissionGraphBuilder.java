package org.opentripplanner.ext.emission.internal.graphbuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.csvdata.EmissionDataReader;
import org.opentripplanner.ext.emission.internal.csvdata.trip.TripHopMapper;
import org.opentripplanner.ext.emission.parameters.EmissionFeedParameters;
import org.opentripplanner.ext.emission.parameters.EmissionParameters;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.ConfiguredDataSource;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows updating the graph with emissions data from external emissions data files.
 */
public class EmissionGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionGraphBuilder.class);

  private final EmissionParameters parameters;
  private final EmissionRepository emissionRepository;
  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources;
  private final Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionDataSources;
  private final TimetableRepository timetableRepository;

  private final DataImportIssueStore issueStore;

  public EmissionGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionDataSources,
    EmissionParameters parameters,
    EmissionRepository emissionRepository,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.emissionDataSources = emissionDataSources;
    this.parameters = parameters;
    this.emissionRepository = emissionRepository;
    this.timetableRepository = timetableRepository;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (parameters == null) {
      return;
    }
    var tripHopMapper = new TripHopMapper(createStopsByTripIdMap(), issueStore);
    var co2EmissionsDataReader = new EmissionDataReader(
      emissionRepository,
      tripHopMapper,
      issueStore
    );

    // Read Transit pasenger emission data from configured emission feeds
    for (var data : emissionDataSources) {
      co2EmissionsDataReader.read(data.dataSource(), data.config().feedId());
    }
    // Read Transit pasenger emission inside gtfs feeds
    for (var data : gtfsDataSources) {
      var resolvedFeedId = new GtfsBundle(data.dataSource(), data.config()).getFeedId();
      co2EmissionsDataReader.read(data.dataSource(), resolvedFeedId);
    }

    logEmissionSummary();
  }

  private Map<FeedScopedId, List<StopLocation>> createStopsByTripIdMap() {
    var map = new HashMap<FeedScopedId, List<StopLocation>>();
    for (TripPattern pattern : timetableRepository.getAllTripPatterns()) {
      pattern.scheduledTripsAsStream().forEach(it -> map.put(it.getId(), pattern.getStops()));
    }
    return map;
  }

  private void logEmissionSummary() {
    LOG.info(emissionRepository.summary().toString());
  }
}
