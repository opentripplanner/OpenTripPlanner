package org.opentripplanner.ext.taxizone.internal.graphbuilder;

import java.io.IOException;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads taxi zones from GTFS feeds whose filenames match the taxi zone pattern
 * and stores them in the {@link TaxiZoneRepository}.
 */
public class TaxiZoneGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TaxiZoneGraphBuilder.class);

  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources;
  private final TaxiZoneDataReader dataReader;

  public TaxiZoneGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    TaxiZoneRepository taxiZoneRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.dataReader = new TaxiZoneDataReader(taxiZoneRepository, issueStore);
  }

  @Override
  public void buildGraph() {
    for (var data : gtfsDataSources) {
      var bundle = new GtfsBundle(data.dataSource(), data.config());
      try {
        dataReader.read(bundle);
      } catch (IOException e) {
        LOG.error("Failed to load taxi zone feed {}: {}", bundle.feedInfo(), e.getMessage());
      }
    }
  }
}
