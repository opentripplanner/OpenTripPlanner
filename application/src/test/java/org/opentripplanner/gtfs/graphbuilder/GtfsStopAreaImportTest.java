package org.opentripplanner.gtfs.graphbuilder;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.GTFSToTransitDataImportMapper;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.site.FareArea;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * Reads a feed with {@code areas.txt} and {@code stop_areas.txt} through {@link GtfsBundle} and
 * the GTFS mapper with the {@link OTPFeature#FaresV2} feature off, and checks the areas of the
 * imported stops and stations.
 */
class GtfsStopAreaImportTest {

  private static final String FEED_ID = "F";

  private static GTFSToTransitDataImportMapper mapper;
  private static TransitDataImportBuilder builder;

  @BeforeAll
  static void importFeedWithoutFaresV2() {
    OTPFeature.FaresV2.testOff(() -> mapper = importFeed());
    builder = mapper.getBuilder();
  }

  @Test
  void station() {
    var station = builder.getStations().get(id("station"));

    assertEquals(List.of(id("station-area")), ids(station.getFareAreas()));
    assertEquals("Station Area", station.getFareAreas().getFirst().getName());
  }

  @Test
  void platformInheritsAreasOfParentStation() {
    var platform = builder.getStops().get(id("platform-inherit"));
    var station = builder.getStations().get(id("station"));

    assertEquals(List.of(id("station-area")), ids(platform.getFareAreas()));
    assertSame(station.getFareAreas().getFirst(), platform.getFareAreas().getFirst());
  }

  @Test
  void platformWithOwnAreas() {
    var platform = builder.getStops().get(id("platform-own"));

    assertEquals(List.of(id("platform-area"), id("unnamed-area")), ids(platform.getFareAreas()));
    assertEquals("Platform Area", platform.getFareAreas().getFirst().getName());
    assertNull(platform.getFareAreas().getLast().getName());
  }

  @Test
  void stopWithoutAreas() {
    assertThat(builder.getStops().get(id("stop-none")).getFareAreas()).isEmpty();
  }

  @Test
  void entranceRowDoesNotFailImport() {
    assertNull(builder.getStops().get(id("entrance")));
    assertEquals(1, builder.siteRepository().entrancesById().size());
  }

  @Test
  void fareServiceGetsNoStopAreasWithoutFaresV2() {
    assertThat(mapper.fareRulesData().stopAreas()).isEmpty();
  }

  @Test
  void fareServiceGetsStopAreasWithFaresV2() {
    OTPFeature.FaresV2.testOn(() ->
      assertThat(importFeed().fareRulesData().stopAreas().keySet()).containsExactly(
        id("station"),
        id("platform-own"),
        id("entrance")
      )
    );
  }

  private static GTFSToTransitDataImportMapper importFeed() {
    var bundle = GtfsBundleTestFactory.forTest(
      ResourceLoader.of(GtfsStopAreaImportTest.class).file("/gtfs/stop_areas"),
      FEED_ID
    );
    var result = new GTFSToTransitDataImportMapper(
      new TransitDataImportBuilder(new SiteRepository(), DataImportIssueStore.NOOP),
      FEED_ID,
      DataImportIssueStore.NOOP,
      false,
      StopTransferPriority.ALLOWED
    );
    try {
      result.mapStopTripAndRouteDataIntoBuilder(bundle.loadDao());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result;
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }

  private static List<FeedScopedId> ids(List<FareArea> areas) {
    return areas.stream().map(FareArea::getId).toList();
  }
}
