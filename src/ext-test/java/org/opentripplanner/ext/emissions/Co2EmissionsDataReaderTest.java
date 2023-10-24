package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class Co2EmissionsDataReaderTest {

  private static final String CO2_GTFS_ZIP_PATH = "src/test/resources/gtfs/emissions-test-gtfs.zip";
  private static final String CO2_GTFS_PATH = "src/test/resources/gtfs/emissions-test-gtfs";
  private static final String INVALID_CO2_GTFS_PATH =
    "src/test/resources/gtfs/emissions-invalid-test-gtfs";

  private Co2EmissionsDataReader co2EmissionsDataReader = new Co2EmissionsDataReader();
  private Map<FeedScopedId, Double> emissions;

  @BeforeEach
  void SetUp() {
    this.emissions = new HashMap<>();
  }

  @Test
  void testCo2EmissionsZipDataReading() {
    this.emissions = co2EmissionsDataReader.readGtfsZip(CO2_GTFS_ZIP_PATH);
    assertEquals(6, emissions.size());
  }

  @Test
  void testCo2EmissionsDataReading() {
    this.emissions = co2EmissionsDataReader.readGtfs(CO2_GTFS_PATH);
    assertEquals(6, emissions.size());
  }

  @Test
  void testInvalidCo2EmissionsDataReading() {
    this.emissions = co2EmissionsDataReader.readGtfs(INVALID_CO2_GTFS_PATH);
    assertEquals(0, emissions.size());
  }
}
