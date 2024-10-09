package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.test.support.ResourceLoader;

public class Co2EmissionsDataReaderTest {

  private static final ResourceLoader RES = ResourceLoader.of(Co2EmissionsDataReaderTest.class);
  private static final File CO2_GTFS_ZIP = RES.file("emissions-test-gtfs.zip");
  private static final File CO2_GTFS = RES.file("emissions-test-gtfs/");
  private static final File INVALID_CO2_GTFS = RES.file("emissions-invalid-test-gtfs/");
  private static final File CO2_MISSING_GTFS_ZIP = RES.file("emissions-missing-test-gtfs.zip");
  private static final File CO2_MISSING_GTFS = RES.file("emissions-missing-test-gtfs");

  private Co2EmissionsDataReader co2EmissionsDataReader = new Co2EmissionsDataReader(
    new DefaultDataImportIssueStore()
  );

  @Test
  void testCo2EmissionsZipDataReading() {
    var emissions = co2EmissionsDataReader.readGtfsZip(CO2_GTFS_ZIP);
    assertEquals(6, emissions.size());
  }

  @Test
  void testCo2EmissionsDataReading() {
    var emissions = co2EmissionsDataReader.readGtfs(CO2_GTFS);
    assertEquals(6, emissions.size());
  }

  @Test
  void testInvalidCo2EmissionsDataReading() {
    var emissions = co2EmissionsDataReader.readGtfs(INVALID_CO2_GTFS);
    assertEquals(0, emissions.size());
  }

  @Test
  void testMissingCo2EmissionsZipDataReading() {
    var emissions = co2EmissionsDataReader.readGtfsZip(CO2_MISSING_GTFS_ZIP);
    assertTrue(emissions.isEmpty());
  }

  @Test
  void testMissingCo2EmissionsDataReading() {
    var emissions = co2EmissionsDataReader.readGtfs(CO2_MISSING_GTFS);
    assertTrue(emissions.isEmpty());
  }
}
