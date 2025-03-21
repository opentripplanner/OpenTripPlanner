package org.opentripplanner.ext.emissions;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.emissions.model.EmissionParameters;
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

  private final EmissionParameters parameters;
  private final EmissionsRepository emissionsRepository;
  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> dataSources;
  private final DataImportIssueStore issueStore;

  public EmissionsGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> dataSources,
    EmissionParameters parameters,
    EmissionsRepository emissionsRepository,
    DataImportIssueStore issueStore
  ) {
    this.dataSources = dataSources;
    this.parameters = parameters;
    this.emissionsRepository = emissionsRepository;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (parameters != null) {
      LOG.info("Start emissions building");
      var co2EmissionsDataReader = new Co2EmissionsDataReader(issueStore);
      double carAvgCo2PerKm = parameters.car().avgCo2PerKm();
      double carAvgOccupancy = parameters.car().avgOccupancy();
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
