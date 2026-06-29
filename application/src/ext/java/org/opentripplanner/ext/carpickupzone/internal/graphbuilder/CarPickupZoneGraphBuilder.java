package org.opentripplanner.ext.carpickupzone.internal.graphbuilder;

import java.io.IOException;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneRepository;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads car pickup zones from GTFS feeds whose filenames match the car pickup zone pattern
 * and stores them in the {@link CarPickupZoneRepository}.
 */
public class CarPickupZoneGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(CarPickupZoneGraphBuilder.class);

  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources;
  private final CarPickupZoneDataReader dataReader;

  public CarPickupZoneGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    CarPickupZoneRepository carPickupZoneRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.dataReader = new CarPickupZoneDataReader(carPickupZoneRepository, issueStore);
  }

  @Override
  public void buildGraph() {
    for (var data : gtfsDataSources) {
      var bundle = new GtfsBundle(data.dataSource(), data.config());
      try {
        dataReader.read(bundle);
      } catch (IOException e) {
        LOG.error("Failed to load car pickup zone feed {}: {}", bundle.feedInfo(), e.getMessage());
      }
    }
  }
}
