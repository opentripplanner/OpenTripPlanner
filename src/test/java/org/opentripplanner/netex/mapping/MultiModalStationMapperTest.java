package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.rutebanken.netex.model.StopPlace;

class MultiModalStationMapperTest {

  @Test
  void testMissingCoordinates() {
    DataImportIssueStore dataIssueStore = new DefaultDataImportIssueStore();
    FeedScopedIdFactory feedScopeIdFactory = new FeedScopedIdFactory(TransitModelForTest.FEED_ID);
    MultiModalStationMapper multiModalStationMapper = new MultiModalStationMapper(
      dataIssueStore,
      feedScopeIdFactory
    );
    StopPlace stopPlace = new StopPlace();
    stopPlace.setId(NetexTestDataSupport.STOP_PLACE_ID);
    assertNull(multiModalStationMapper.map(stopPlace, List.of()));
    assertEquals(1, dataIssueStore.listIssues().size());
    assertEquals(
      "MultiModalStationWithoutCoordinates",
      dataIssueStore.listIssues().getFirst().getType()
    );
  }
}
