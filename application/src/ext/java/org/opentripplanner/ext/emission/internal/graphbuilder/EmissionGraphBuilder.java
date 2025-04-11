package org.opentripplanner.ext.emission.internal.graphbuilder;

import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.csvdata.EmissionDataReader;
import org.opentripplanner.ext.emission.parameters.EmissionFeedParameters;
import org.opentripplanner.ext.emission.parameters.EmissionParameters;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.ConfiguredDataSource;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.utils.logging.ProgressTracker;
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

  private final DataImportIssueStore issueStore;

  public EmissionGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionDataSources,
    EmissionParameters parameters,
    EmissionRepository emissionRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.emissionDataSources = emissionDataSources;
    this.parameters = parameters;
    this.emissionRepository = emissionRepository;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (parameters == null) {
      return;
    }
    var progress = ProgressTracker.track("Read emission data", 100, -1);
    var co2EmissionsDataReader = new EmissionDataReader(emissionRepository, issueStore);

    // Init car passenger emission data
    {
      double carAvgCo2PerKm = parameters.car().avgCo2PerKm();
      double carAvgOccupancy = parameters.car().avgOccupancy();
      double carAvgEmissionsPerMeter = carAvgCo2PerKm / 1000 / carAvgOccupancy;
      this.emissionRepository.setCarAvgCo2PerMeter(carAvgEmissionsPerMeter);
    }
    // Read Transit pasenger emission data from configured emission feeds
    for (var data : emissionDataSources) {
      co2EmissionsDataReader.read(data.dataSource(), data.config().feedId());
      progress.step(m -> LOG.info(m));
    }
    // Read Transit pasenger emission inside gtfs feeds
    for (var data : gtfsDataSources) {
      var resolvedFeedId = new GtfsBundle(data.dataSource(), data.config()).getFeedId();
      co2EmissionsDataReader.read(data.dataSource(), resolvedFeedId);
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
  }
}
