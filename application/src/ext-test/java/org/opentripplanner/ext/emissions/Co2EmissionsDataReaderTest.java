package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.test.support.ResourceLoader;

public class Co2EmissionsDataReaderTest {

  private static final ResourceLoader RES = ResourceLoader.of(Co2EmissionsDataReaderTest.class);
  private static final CompositeDataSource CO2_GTFS = RES.catalogDataSource(
    "emissions-test-gtfs",
    FileType.GTFS
  );
  private static final CompositeDataSource INVALID_CO2_GTFS = RES.catalogDataSource(
    "emissions-invalid-test-gtfs",
    FileType.GTFS
  );
  public static final String FEED_ID = "em";

  private Co2EmissionsDataReader co2EmissionsDataReader = new Co2EmissionsDataReader(
    DataImportIssueStore.NOOP
  );

  @Test
  void testCo2EmissionsDataReading() throws FileNotFoundException {
    var emissions = co2EmissionsDataReader.read(CO2_GTFS, FEED_ID);
    assertEquals(6, emissions.size());
  }

  @Test
  void testInvalidCo2EmissionsDataReading() throws FileNotFoundException {
    var emissions = co2EmissionsDataReader.read(INVALID_CO2_GTFS, FEED_ID);
    assertEquals(0, emissions.size());
  }
}
