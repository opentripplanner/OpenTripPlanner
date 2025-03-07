package org.opentripplanner.gtfs.graphbuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.onebusaway.csv_entities.CsvInputSource;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsBundle {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsBundle.class);

  private final CompositeDataSource dataSource;

  private GtfsFeedId feedId;

  private CsvInputSource csvInputSource;

  public int subwayAccessTime;

  private double maxStopToShapeSnapDistance = 150;
  private final boolean removeRepeatedStops;

  private final StopTransferPriority stationTransferPreference;

  private final boolean discardMinTransferTimes;

  private final boolean blockBasedInterlining;

  private final int maxInterlineDistance;

  /** Used by unit tests */
  public GtfsBundle(File gtfsFile) {
    this(DataStoreModule.compositeSource(gtfsFile, FileType.GTFS));
  }

  private GtfsBundle(CompositeDataSource compositeDataSource) {
    this(
      new ConfiguredDataSource<>(
        compositeDataSource,
        new GtfsFeedParametersBuilder().withSource(compositeDataSource.uri()).build()
      )
    );
  }

  public GtfsBundle(ConfiguredDataSource<GtfsFeedParameters> configuredDataSource) {
    this.dataSource = (CompositeDataSource) configuredDataSource.dataSource();
    if (configuredDataSource.config().feedId() != null) {
      this.feedId = new GtfsFeedId.Builder().id(configuredDataSource.config().feedId()).build();
    }
    this.removeRepeatedStops = configuredDataSource.config().removeRepeatedStops();
    this.stationTransferPreference = configuredDataSource.config().stationTransferPreference();
    this.discardMinTransferTimes = configuredDataSource.config().discardMinTransferTimes();
    this.blockBasedInterlining = configuredDataSource.config().blockBasedInterlining();
    this.maxInterlineDistance = configuredDataSource.config().maxInterlineDistance();
  }

  public CsvInputSource getCsvInputSource() {
    if (csvInputSource == null) {
      csvInputSource = new CsvInputSource() {
        @Override
        public boolean hasResource(String s) {
          return dataSource.content().stream().anyMatch(it -> it.name().equals(s));
        }

        @Override
        public InputStream getResource(String s) {
          return dataSource.entry(s).asInputStream();
        }

        @Override
        public void close() {}
      };
    }
    return csvInputSource;
  }

  public void close() {
    try {
      dataSource.close();
    } catch (IOException e) {
      LOG.warn(
        "Failed to close datasource {}, details: {}",
        dataSource.path(),
        e.getLocalizedMessage(),
        e
      );
    }
  }

  public String toString() {
    String src = dataSource.path();
    if (feedId != null) {
      src += " (" + feedId.getId() + ")";
    }
    return "GTFS bundle at " + src;
  }

  /**
   * So that we can load multiple gtfs feeds into the same database.
   */
  public GtfsFeedId getFeedId() {
    if (feedId == null) {
      feedId = new GtfsFeedId.Builder().fromGtfsFeed(getCsvInputSource()).build();
    }
    return feedId;
  }

  public void setFeedId(GtfsFeedId feedId) {
    this.feedId = feedId;
  }

  public void checkInputs() {
    if (csvInputSource != null) {
      LOG.warn("unknown CSV source type; cannot check inputs");
      return;
    }
    if (!dataSource.exists()) {
      throw new RuntimeException(
        "GTFS Path " + dataSource.path() + " does not exist or " + "cannot be read."
      );
    }
  }

  public double getMaxStopToShapeSnapDistance() {
    return maxStopToShapeSnapDistance;
  }

  public void setMaxStopToShapeSnapDistance(double maxStopToShapeSnapDistance) {
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
  }

  public boolean removeRepeatedStops() {
    return removeRepeatedStops;
  }

  public StopTransferPriority stationTransferPreference() {
    return stationTransferPreference;
  }

  public boolean discardMinTransferTimes() {
    return discardMinTransferTimes;
  }

  public boolean blockBasedInterlining() {
    return blockBasedInterlining;
  }

  public int maxInterlineDistance() {
    return maxInterlineDistance;
  }
}
