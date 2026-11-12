package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.netex.NetexTestDataSupport.createQuay;
import static org.opentripplanner.netex.NetexTestDataSupport.createStopPlace;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.entur.netex2.model.Quay;
import org.entur.netex2.model.StopPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;

class StationMapperTest {

  private DataImportIssueStore issueStore;
  private StationMapper stationMapper;

  @BeforeEach
  void setUp() {
    issueStore = new DefaultDataImportIssueStore();
    stationMapper = new StationMapper(
      issueStore,
      new FeedScopedIdFactory("FEED_ID"),
      ZoneId.of("UTC"),
      false,
      Set.of(),
      SiteRepository.of()
    );
  }

  @Test
  void testMappingWithStopPlaceCentroid() {
    StopPlace stopPlace = createStopPlace();
    Station station = stationMapper.map(stopPlace);
    assertEquals(NetexTestDataSupport.STOP_PLACE_LAT, station.getLat());
    assertEquals(NetexTestDataSupport.STOP_PLACE_LON, station.getLon());
    assertThat(issueTypes()).isEmpty();
  }

  @Test
  void testMappingWithQuayCentroid() {
    Quay quay = createQuay();
    StopPlace stopPlace = createStopPlace(quay);
    stopPlace.withCentroid(null);
    Station station = stationMapper.map(stopPlace);
    assertEquals(NetexTestDataSupport.QUAY_LAT, station.getLat());
    assertEquals(NetexTestDataSupport.QUAY_LON, station.getLon());
    assertThat(issueTypes()).containsExactly("StationWithoutCoordinates");
  }

  @Test
  void testStopPlaceWithoutCentroidAndWithoutQuaysIsSkipped() {
    StopPlace stopPlace = createStopPlace();
    stopPlace.withCentroid(null);
    assertNull(stationMapper.map(stopPlace));
    assertThat(issueTypes()).containsExactly(
      "StationWithoutCoordinates",
      "StationWithoutValidCoordinates"
    );
  }

  private List<String> issueTypes() {
    return issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
  }
}
