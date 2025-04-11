package org.opentripplanner.ext.emission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.internal.csvdata.EmissionDataReader;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class EmissionDataReaderTest implements EmissionTestData {

  // We explicit set the feed-id in this test, we do NOT use the feed-id in the teed-info.txt.
  // This way, we test that the feed-id can be overridden.
  private static final String FEED_ID = "F";
  private static final FeedScopedId ROUTE_D_1001 = new FeedScopedId(
    FEED_ID,
    ROUTE_ID_GD_1001.getId()
  );
  private static final FeedScopedId ROUTE_F_R1 = new FeedScopedId(FEED_ID, ROUTE_ID_EM_R1.getId());

  private EmissionDataReader subject = new EmissionDataReader(DataImportIssueStore.NOOP);

  @Test
  void testCo2EmissionsFromGtfsDataSource() throws FileNotFoundException {
    var emission = subject.read(gtfsWithEmissionDir(), FEED_ID);
    assertEquals(0.006, emission.get(ROUTE_D_1001), 0.0001);
    assertEquals(6, emission.size());
  }

  @Test
  void testCo2EmissionsFromFeedDataSource() throws FileNotFoundException {
    var emission = subject.read(emissionFeed(), FEED_ID);
    assertEquals(0.006, emission.get(ROUTE_F_R1), 0.0001);
    assertEquals(2, emission.size());
  }
}
