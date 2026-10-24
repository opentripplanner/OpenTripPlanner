package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;

class MultiModalStationMapperTest {

  @Test
  void testMissingCoordinates() {
    DataImportIssueStore dataIssueStore = new DefaultDataImportIssueStore();
    FeedScopedIdFactory feedScopeIdFactory = new FeedScopedIdFactory(
      TransitRepositoryForTest.FEED_ID
    );
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

  @Test
  void testInvalidCoordinates() {
    DataImportIssueStore dataIssueStore = new DefaultDataImportIssueStore();
    MultiModalStationMapper multiModalStationMapper = new MultiModalStationMapper(
      dataIssueStore,
      new FeedScopedIdFactory(TransitRepositoryForTest.FEED_ID)
    );
    StopPlace stopPlace = new StopPlace()
      .withId(NetexTestDataSupport.STOP_PLACE_ID)
      .withCentroid(
        new SimplePoint_VersionStructure().withLocation(
          new LocationStructure()
            .withLatitude(BigDecimal.valueOf(56162332.0))
            .withLongitude(BigDecimal.valueOf(15.579309))
        )
      );
    assertNull(multiModalStationMapper.map(stopPlace, List.of()));
    assertEquals(1, dataIssueStore.listIssues().size());
    assertEquals(
      "MultiModalStationInvalidCoordinates",
      dataIssueStore.listIssues().getFirst().getType()
    );
  }
}
