package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.test.support.ResourceLoader;

public class Co2EmissionsDataReaderTest {

  private static final ResourceLoader RES = ResourceLoader.of(Co2EmissionsDataReaderTest.class);
  private static final String CO2_GTFS_ZIP_PATH = RES.file("emissions-test-gtfs.zip").getPath();
  private static final String CO2_GTFS_PATH = RES.file("emissions-test-gtfs/").getPath();
  private static final String INVALID_CO2_GTFS_PATH = RES
    .file("emissions-invalid-test-gtfs/")
    .getPath();

  private Co2EmissionsDataReader co2EmissionsDataReader = new Co2EmissionsDataReader(
    new DefaultDataImportIssueStore()
  );

  @Test
  void testCo2EmissionsZipDataReading() {
    var emissions = co2EmissionsDataReader.readGtfsZip(CO2_GTFS_ZIP_PATH);
    assertEquals(6, emissions.size());
  }

  @Test
  void testCo2EmissionsDataReading() {
    var emissions = co2EmissionsDataReader.readGtfs(CO2_GTFS_PATH);
    assertEquals(6, emissions.size());
  }

  @Test
  void testInvalidCo2EmissionsDataReading() {
    var emissions = co2EmissionsDataReader.readGtfs(INVALID_CO2_GTFS_PATH);
    assertEquals(0, emissions.size());
  }
}
