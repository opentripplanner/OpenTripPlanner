package org.opentripplanner.netex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.netex.configure.NetexConfigure;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Load a small NeTEx data set with car pooling, a commuter offering a ride on two days. This is a
 * smoke test of the parts of the import working together, each part has its own test.
 */
class NetexCarPoolingBundleSmokeTest {

  private static final File NETEX_DIR = new File("src/test/resources/netex/carpooling");

  @Test
  void carPoolingJourneysAreImportedAsCarpoolTrips() throws IOException {
    var transitBuilder = new TransitDataImportBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );
    try (NetexBundle netexBundle = netexBundle(transitBuilder)) {
      netexBundle.loadBundle(new Deduplicator(), DataImportIssueStore.NOOP);
    }
    var transitData = transitBuilder.build();

    assertEquals(1, transitData.getTripPatterns().size());
    TripPattern tripPattern = transitData.getTripPatterns().iterator().next();
    assertTripPattern(tripPattern);
    assertRoute(tripPattern.getRoute());
    assertTrips(
      transitData.getAllTrips().stream().toList(),
      transitBuilder.buildCalendarServiceData()
    );
    assertTripTimes(tripPattern.getScheduledTimetable().getTripTimes().getFirst());
  }

  private static void assertTripPattern(TripPattern tripPattern) {
    assertEquals("RID:SingleJourneyPath:1", tripPattern.getId().getId());
    assertEquals("Hill road to Central station", tripPattern.getName());
    assertEquals(TransitMode.CARPOOL, tripPattern.getMode());
    // The two first meeting points are stops of their own, the last is assigned to a quay
    assertEquals(
      "[RegularStop{NETEX:RID:VehicleMeetingPoint:home Hill road 12}, " +
        "RegularStop{NETEX:RID:VehicleMeetingPoint:school School car park}, " +
        "RegularStop{NETEX:RID:Quay:1 Central station}]",
      tripPattern.getStops().toString()
    );
  }

  private static void assertRoute(Route route) {
    assertEquals("RID:CarPoolingService:1", route.getId().getId());
    assertEquals("Ride Along commuter car pool", route.getName());
    assertEquals(TransitMode.CARPOOL, route.getMode());
    assertEquals("Ride Along", route.getOperator().getName());
    assertEquals("Ride Along", route.getAgency().getName());
  }

  private static void assertTrips(List<Trip> allTrips, CalendarServiceData calendar) {
    var trips = allTrips
      .stream()
      .sorted(Comparator.comparing(trip -> trip.getId().getId()))
      .toList();

    assertEquals(
      "[Trip{NETEX:RID:SingleJourney:2026-12-01 Ride Along commuter car pool}, " +
        "Trip{NETEX:RID:SingleJourney:2026-12-03 Ride Along commuter car pool}]",
      trips.toString()
    );
    assertEquals("Central station", trips.getFirst().getHeadsign().toString());
    assertEquals(
      List.of(LocalDate.of(2026, 12, 1)),
      calendar.getServiceDatesForServiceId(trips.getFirst().getServiceId())
    );
    assertEquals(
      List.of(LocalDate.of(2026, 12, 3)),
      calendar.getServiceDatesForServiceId(trips.getLast().getServiceId())
    );
  }

  private static void assertTripTimes(TripTimes<?> tripTimes) {
    assertEquals(TimeUtils.time("08:00"), tripTimes.getDepartureTime(0));
    assertEquals(TimeUtils.time("08:04"), tripTimes.getArrivalTime(1));
    assertEquals(TimeUtils.time("08:05"), tripTimes.getDepartureTime(1));
    assertEquals(TimeUtils.time("08:15"), tripTimes.getArrivalTime(2));
  }

  private static NetexBundle netexBundle(TransitDataImportBuilder transitBuilder) {
    var dataSource = new DirectoryDataSource(NETEX_DIR, FileType.NETEX);
    var configuredDataSource = new ConfiguredCompositeDataSource<>(
      dataSource,
      NetexFeedParameters.DEFAULT
    );
    return new NetexConfigure(BuildConfig.DEFAULT).netexBundle(
      transitBuilder,
      configuredDataSource
    );
  }
}
