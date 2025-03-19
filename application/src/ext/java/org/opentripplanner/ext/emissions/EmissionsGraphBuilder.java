package org.opentripplanner.ext.emissions;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.emissions.config.EmissionsConfig;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
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

  private final EmissionsConfig config;
  private final EmissionsRepository emissionsRepository;
  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> dataSources;
  private final DataImportIssueStore issueStore;

  public EmissionsGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> dataSources,
    EmissionsConfig config,
    EmissionsRepository emissionsRepository,
    DataImportIssueStore issueStore
  ) {
    this.dataSources = dataSources;
    this.config = config;
    this.emissionsRepository = emissionsRepository;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (config != null) {
      LOG.info("Start emissions building");
      var co2EmissionsDataReader = new Co2EmissionsDataReader(issueStore);
      double carAvgCo2PerKm = config.getCarAvgCo2PerKm();
      double carAvgOccupancy = config.getCarAvgOccupancy();
      double carAvgEmissionsPerMeter = carAvgCo2PerKm / 1000 / carAvgOccupancy;
      Map<FeedScopedId, Double> emissionsData = new HashMap<>();

      for (var gtfsData : dataSources) {
        var resolvedFeedId = new GtfsBundle(gtfsData.dataSource(), gtfsData.config()).getFeedId();
        emissionsData.putAll(co2EmissionsDataReader.read(gtfsData.dataSource(), resolvedFeedId));
      }
      this.emissionsRepository.setCo2Emissions(emissionsData);
      this.emissionsRepository.setCarAvgCo2PerMeter(carAvgEmissionsPerMeter);
      LOG.info(
        "Emissions building finished. Number of CO2 emission records saved: " + emissionsData.size()
      );
    }
  }
}
