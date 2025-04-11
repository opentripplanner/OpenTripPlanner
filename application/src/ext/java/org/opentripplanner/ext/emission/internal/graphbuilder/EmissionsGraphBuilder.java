package org.opentripplanner.ext.emission.internal.graphbuilder;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.emission.EmissionsRepository;
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
 * This class allows updating the graph with emissions data from external emissions data files.
 */
public class EmissionsGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionsGraphBuilder.class);

  private final EmissionParameters parameters;
  private final EmissionsRepository emissionsRepository;
  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources;
  private final Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionsDataSources;
  private final DataImportIssueStore issueStore;

  public EmissionsGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    Iterable<ConfiguredDataSource<EmissionFeedParameters>> emissionsDataSources,
    EmissionParameters parameters,
    EmissionsRepository emissionsRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.emissionsDataSources = emissionsDataSources;
    this.parameters = parameters;
    this.emissionsRepository = emissionsRepository;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (parameters != null) {
      LOG.info("Start emissions building");
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
      this.emissionsRepository.setCo2Emissions(emissionsData);
      this.emissionsRepository.setCarAvgCo2PerMeter(carAvgEmissionsPerMeter);
      LOG.info(
        "Emissions building finished. Number of CO2 emission records saved: " + emissionsData.size()
      );
    }
  }
}
