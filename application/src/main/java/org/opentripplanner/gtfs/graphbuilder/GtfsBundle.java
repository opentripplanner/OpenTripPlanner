package org.opentripplanner.gtfs.graphbuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.onebusaway.csv_entities.CsvInputSource;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.gtfs.config.GtfsDefaultParameters;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsBundle {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsBundle.class);

  private final CompositeDataSource dataSource;

  // The feedId is resolved lazy to make any exceptions in the caller when parsing the
  // gfts files, and not in the instrumentation of the bundle.
  @Nullable
  private String feedId;

  private CsvInputSource csvInputSource;

  private final GtfsFeedParameters parameters;

  public GtfsBundle(CompositeDataSource dataSource, GtfsFeedParameters parameters) {
    this.dataSource = dataSource;
    this.parameters = parameters;
    // Override feed id, if set in config
    this.feedId = parameters.feedId();
  }

  /** Used by unit tests */
  public static GtfsBundle forTest(File gtfsFile, @Nullable String feedId) {
    var dataSource = DataStoreModule.compositeSource(gtfsFile, FileType.GTFS);
    var parameters = GtfsDefaultParameters.DEFAULT.withFeedInfo()
      .withSource(dataSource.uri())
      .withFeedId(feedId)
      .build();
    return new GtfsBundle(dataSource, parameters);
  }

  /** Used by unit tests */
  public static GtfsBundle forTest(File gtfsFile) {
    return forTest(gtfsFile, null);
  }

  /**
   * So that we can load multiple gtfs feeds into the same database.
   */
  public String getFeedId() {
    if (feedId == null) {
      feedId = GtfsFeedIdResolver.fromGtfsFeed(getCsvInputSource());
    }
    return feedId;
  }

  public GtfsFeedParameters parameters() {
    return parameters;
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

  public String feedInfo() {
    return "GTFS bundle at " + dataSource.path() + " (" + getFeedId() + ")";
  }
}
