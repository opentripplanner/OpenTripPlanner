package org.opentripplanner.ext.emission;

import java.io.File;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.ext.emission.internal.csvdata.EmissionDataReader;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public interface EmissionTestData {
  String FEED_FEED_ID = "em";
  String GTFS_DIR_FEED_ID = "gd";
  String GTFS_ZIP_FEED_ID = "gz";

  FeedScopedId ROUTE_ID_EM_R1 = new FeedScopedId(FEED_FEED_ID, "R1");
  FeedScopedId ROUTE_ID_GD_1001 = new FeedScopedId(GTFS_DIR_FEED_ID, "1001");
  FeedScopedId ROUTE_ID_GZ_1002 = new FeedScopedId(GTFS_ZIP_FEED_ID, "1002");

  default CompositeDataSource gtfsWithEmissionZip() {
    return resource().catalogDataSource("gz-gtfs.zip", FileType.GTFS);
  }

  default CompositeDataSource gtfsWithEmissionDir() {
    return resource().catalogDataSource("gd-gtfs/", FileType.GTFS);
  }

  default DataSource gtfsWithEmissionFile() {
    return gtfsWithEmissionDir().entry(EmissionDataReader.EMISSION_FILE_NAME);
  }

  default DataSource emissionOnRoutes() {
    return resource().dataSource("em-on-routes.txt", FileType.EMISSION);
  }

  default DataSource emissionOnTripLegs() {
    return resource().dataSource("em-on-trip-legs.txt", FileType.EMISSION);
  }

  /**
   * The DataSource framwork should prevent this from happening, but we add it here
   * as a test-case so we can see that the parsers handle it gracefully.
   */
  default DataSource emissionMissingFile() {
    return new FileDataSource(new File("file-does-not-exist.txt"), FileType.EMISSION);
  }

  private ResourceLoader resource() {
    return ResourceLoader.of(EmissionTestData.class);
  }
}
