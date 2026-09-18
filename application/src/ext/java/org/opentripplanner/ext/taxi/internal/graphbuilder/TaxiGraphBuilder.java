package org.opentripplanner.ext.taxi.internal.graphbuilder;

import java.io.IOException;
import org.opentripplanner.ext.taxi.TaxiRepository;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads taxi zones from GTFS feeds explicitly flagged with
 * {@code transitFeeds[].taxiProvider: true} and stores them in the
 * {@link TaxiRepository}.
 */
public class TaxiGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TaxiGraphBuilder.class);

  private final Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources;
  private final TaxiDataReader dataReader;

  public TaxiGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> gtfsDataSources,
    TaxiRepository taxiRepository,
    DataImportIssueStore issueStore
  ) {
    this.gtfsDataSources = gtfsDataSources;
    this.dataReader = new TaxiDataReader(taxiRepository, issueStore);
  }

  @Override
  public void buildGraph() {
    for (var data : gtfsDataSources) {
      var bundle = new GtfsBundle(data.dataSource(), data.config());
      try {
        dataReader.read(bundle);
      } catch (IOException e) {
        LOG.error("Failed to load taxi provider feed {}: {}", bundle.feedInfo(), e.getMessage());
      }
    }
  }
}
