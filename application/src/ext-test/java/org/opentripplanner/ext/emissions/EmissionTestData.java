package org.opentripplanner.ext.emissions;

import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.FeedScopedId;

interface EmissionTestData {
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

  default DataSource emissionFeed() {
    return resource().dataSource("em-feed.txt", FileType.EMMISION);
  }

  private ResourceLoader resource() {
    return ResourceLoader.of(EmissionTestData.class);
  }
}
