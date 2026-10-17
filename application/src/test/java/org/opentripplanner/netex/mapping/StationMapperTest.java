package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.netex.NetexTestDataSupport.createQuay;
import static org.opentripplanner.netex.NetexTestDataSupport.createStopPlace;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

class StationMapperTest {

  /** A latitude which lost its decimal point, as seen in real data. */
  private static final double INVALID_LAT = 56162332.0;

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
  void testInvalidStopPlaceCentroidFallsBackToQuayCentroid() {
    Quay quay = createQuay();
    StopPlace stopPlace = createStopPlace(quay);
    stopPlace.getCentroid().getLocation().setLatitude(BigDecimal.valueOf(INVALID_LAT));
    Station station = stationMapper.map(stopPlace);
    assertEquals(NetexTestDataSupport.QUAY_LAT, station.getLat());
    assertEquals(NetexTestDataSupport.QUAY_LON, station.getLon());
    assertThat(issueTypes()).containsExactly("StationInvalidCoordinates");
  }

  @Test
  void testInvalidQuayCentroidIsIgnoredWhenComputingStationCentroid() {
    Quay invalidQuay = createQuay("Q:invalid", "Invalid", "1", INVALID_LAT, 10.0, "1");
    StopPlace stopPlace = createStopPlace(invalidQuay);
    stopPlace.getQuays().withQuayRefOrQuay(new ObjectFactory().createQuay(createQuay()));
    stopPlace.withCentroid(null);
    Station station = stationMapper.map(stopPlace);
    assertEquals(NetexTestDataSupport.QUAY_LAT, station.getLat());
    assertEquals(NetexTestDataSupport.QUAY_LON, station.getLon());
  }

  @Test
  void testStopPlaceWithoutAnyValidCoordinatesIsSkipped() {
    Quay invalidQuay = createQuay("Q:invalid", "Invalid", "1", INVALID_LAT, 10.0, "1");
    StopPlace stopPlace = createStopPlace(invalidQuay);
    stopPlace.getCentroid().getLocation().setLatitude(BigDecimal.valueOf(INVALID_LAT));
    assertNull(stationMapper.map(stopPlace));
    assertThat(issueTypes()).containsExactly(
      "StationInvalidCoordinates",
      "StationWithoutValidCoordinates"
    );
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
