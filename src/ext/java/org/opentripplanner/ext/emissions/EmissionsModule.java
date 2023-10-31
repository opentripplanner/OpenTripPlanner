package org.opentripplanner.ext.emissions;

import dagger.Module;
import jakarta.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows updating the graph with emissions data from external emissions data files.
 */
@Module
public class EmissionsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionsModule.class);
  private BuildConfig config;
  private EmissionsDataModel emissionsDataModel;
  private GraphBuilderDataSources dataSources;
  private Map<FeedScopedId, Double> emissionsData = new HashMap<>();
  private final DataImportIssueStore issueStore;

  @Inject
  public EmissionsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    EmissionsDataModel emissionsDataModel,
    DataImportIssueStore issueStore
  ) {
    this.dataSources = dataSources;
    this.config = config;
    this.emissionsDataModel = emissionsDataModel;
    this.issueStore = issueStore;
  }

  public void buildGraph() {
    if (config.emissions != null) {
      LOG.info("Start emissions building");
      Co2EmissionsDataReader co2EmissionsDataReader = new Co2EmissionsDataReader(issueStore);
      double carAvgCo2PerKm = config.emissions.getCarAvgCo2PerKm();
      double carAvgOccupancy = config.emissions.getCarAvgOccupancy();
      double carAvgEmissionsPerMeter = carAvgCo2PerKm / 1000 / carAvgOccupancy;

      for (ConfiguredDataSource<GtfsFeedParameters> gtfsData : dataSources.getGtfsConfiguredDatasource()) {
        if (gtfsData.dataSource().name().contains(".zip")) {
          emissionsData = co2EmissionsDataReader.readGtfsZip(new File(gtfsData.dataSource().uri()));
        } else {
          emissionsData = co2EmissionsDataReader.readGtfs(new File(gtfsData.dataSource().uri()));
        }
      }
      this.emissionsDataModel.setCo2Emissions(this.emissionsData);
      this.emissionsDataModel.setCarAvgCo2PerMeter(carAvgEmissionsPerMeter);
    }
  }
}
