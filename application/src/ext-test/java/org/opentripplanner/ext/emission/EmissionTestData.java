package org.opentripplanner.ext.emission;

import java.io.File;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.configure.DataStoreModule;
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
    return DataStoreModule.dataSource(
      "em-on-routes.txt",
      FileType.EMISSION,
      """
      route_id,avg_co2_per_vehicle_per_km,avg_passenger_count
      R1,12.0,2.0
      R2,123,3
      """
    );
  }

  default DataSource emissionOnTripHops() {
    return DataStoreModule.dataSource(
      "em-on-trip-hops.txt",
      FileType.EMISSION,
      """
      trip_id,from_stop_id,from_stop_sequence,ignore_this_value,co2
      T1,A,1,ignore,5.0
      T1,B,2,999,7.0
      T2,A,1,xyz,15.0
      T2,B,2,xyz,17.0
      E1,A,-1,xyz,25.0
      E2,B,1,xyz,-1000001
      E3,B,1,xyz,1000000001
      """
    );
  }

  /**
   * The DataSource framwork should prevent this from happening, but we add it here
   * as a test-case so we can see that the parsers handle it gracefully.
   */
  default DataSource emissionMissingFile() {
    return new FileDataSource(new File("file-does-not-exist.txt"), FileType.EMISSION);
  }

  default CompositeDataSource gtfsDirectoryDataSourceWithoutEmissions() {
    return DataStoreModule.compositeSource(ConstantsForTests.SIMPLE_GTFS, FileType.GTFS);
  }

  default CompositeDataSource gtfsZipDataSourceWithoutEmissions() {
    return DataStoreModule.compositeSource(ConstantsForTests.CALTRAIN_GTFS, FileType.GTFS);
  }

  private ResourceLoader resource() {
    return ResourceLoader.of(EmissionTestData.class);
  }
}
