package org.opentripplanner.ext.emission.internal.graphbuilder;

import java.util.HashMap;
import java.util.Map;
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
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows updating the graph with emission data from external emission data files.
 */
public class EmissionGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionGraphBuilder.class);

  private final EmissionParameters parameters;
  private final EmissionRepository emissionRepository;
  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources;
  private final Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionsDataSources;
  private final DataImportIssueStore issueStore;

  public EmissionGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionsDataSources,
    EmissionParameters parameters,
    EmissionRepository emissionRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.emissionsDataSources = emissionsDataSources;
    this.parameters = parameters;
    this.emissionRepository = emissionRepository;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (parameters != null) {
      LOG.info("Start emission building");
      var dataReader = new EmissionDataReader(issueStore);
      double carAvgCo2PerKm = parameters.car().avgCo2PerKm();
      double carAvgOccupancy = parameters.car().avgOccupancy();
      double carAvgEmissionsPerMeter = carAvgCo2PerKm / 1000 / carAvgOccupancy;
      Map<FeedScopedId, Double> emissionsData = new HashMap<>();

      for (var data : emissionsDataSources) {
        emissionsData.putAll(dataReader.read(data.dataSource(), data.config().feedId()));
      }
      for (var data : gtfsDataSources) {
        var resolvedFeedId = new GtfsBundle(data.dataSource(), data.config()).getFeedId();
        emissionsData.putAll(dataReader.read(data.dataSource(), resolvedFeedId));
      }
      this.emissionRepository.setCo2Emissions(emissionsData);
      this.emissionRepository.setCarAvgCo2PerMeter(carAvgEmissionsPerMeter);
      LOG.info(
        "Emissions building finished. Number of emission records saved: " + emissionsData.size()
      );
    }
  }
}
