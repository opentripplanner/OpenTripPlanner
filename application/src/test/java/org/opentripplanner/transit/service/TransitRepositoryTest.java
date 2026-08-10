package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.route;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.stopPattern;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.tripPattern;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.service.gtfs.v1.GtfsFareServiceFactory;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;

class TransitRepositoryTest {

  public static final String FAKE_FEED_ID = "FAKE";
  public static final FeedScopedId SAMPLE_TRIP_ID = new FeedScopedId(FAKE_FEED_ID, "1.2");
  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(
    TransitRepositoryTest.class
  );

  @Test
  void validateTimeZones() {
    // First GTFS bundle should be added successfully
    var siteRepository = new SiteRepository();
    var graph = new Graph();
    var transitRepository = new TransitRepository(siteRepository);
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitRepository,
      ConstantsForTests.SIMPLE_GTFS,
      new GtfsFareServiceFactory(),
      FAKE_FEED_ID
    );

    // Then time zone should match the one provided in the feed
    assertEquals("America/New_York", transitRepository.getTimeZone().getId());

    // Then trip times should be same as in input data
    TransitRepositoryIndex transitRepositoryIndex = transitRepository.getTransitRepositoryIndex();
    Trip trip = transitRepositoryIndex.getTripForId(SAMPLE_TRIP_ID);
    Timetable timetable = transitRepositoryIndex.getPatternForTrip(trip).getScheduledTimetable();
    assertEquals(20 * 60, timetable.getTripTimes(trip).getDepartureTime(0));

    // Should throw on second bundle, with different agency time zone
    assertThrows(
      IllegalStateException.class,
      () ->
        ConstantsForTests.addGtfsToGraph(
          graph,
          transitRepository,
          RESOURCE_LOADER.file("kcm_gtfs.zip"),
          new GtfsFareServiceFactory(),
          null
        ),
      ("The graph contains agencies with different time zones. " +
        "Please configure the one to be used in the " +
        BUILD_CONFIG_FILENAME)
    );
  }

  @Test
  void validateTimeZonesWithExplicitTimeZone() {
    var siteRepository = new SiteRepository();
    var graph = new Graph();
    var transitRepository = new TransitRepository(siteRepository);

    // Whit explicit time zone
    transitRepository.initTimeZone(ZoneIds.CHICAGO);

    // First GTFS bundle should be added successfully
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitRepository,
      ConstantsForTests.SIMPLE_GTFS,
      new GtfsFareServiceFactory(),
      FAKE_FEED_ID
    );

    // Should load second bundle, with different agency time zone
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitRepository,
      RESOURCE_LOADER.file("kcm_gtfs.zip"),
      new GtfsFareServiceFactory(),
      null
    );

    new TimeZoneAdjusterModule(transitRepository).buildGraph();

    TransitRepositoryIndex transitRepositoryIndex = transitRepository.getTransitRepositoryIndex();

    // Then time zone should match the one provided in the feed
    assertEquals("America/Chicago", transitRepository.getTimeZone().getId());

    // Then trip times should be on hour less than in input data
    Trip trip = transitRepositoryIndex.getTripForId(SAMPLE_TRIP_ID);
    Timetable timetable = transitRepositoryIndex.getPatternForTrip(trip).getScheduledTimetable();
    assertEquals(20 * 60 - 60 * 60, timetable.getTripTimes(trip).getDepartureTime(0));
  }

  @Test
  void scheduledStopPoints() {
    var repo = new TransitRepository();
    var sspId = id("ssp-1");
    var stop = TransitRepositoryForTest.of().stop("stop-1").build();
    repo.addScheduledStopPointMapping(Map.of(sspId, stop));
    assertEquals(stop, repo.findStopByScheduledStopPoint(sspId).get());
  }

  @Test
  void testGetStopLocationsUsedForBikesAllowedTrips() {
    var repo = new TransitRepository();
    var S11 = TransitRepositoryForTest.of().stop("S11").build();
    var S12 = TransitRepositoryForTest.of().stop("S12").build();
    var S13 = TransitRepositoryForTest.of().stop("S13").build();
    var S21 = TransitRepositoryForTest.of().stop("S21").build();
    var S22 = TransitRepositoryForTest.of().stop("S22").build();
    var S23 = TransitRepositoryForTest.of().stop("S23").build();
    var R1 = route("R1").withMode(TransitMode.BUS).build();
    var R2 = route("R2").withMode(TransitMode.BUS).build();
    var TP1 = tripPattern("TP1", R1)
      .withStopPattern(stopPattern(S11, S12, S13))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of()
            .withTrip(TransitRepositoryForTest.trip("T1").build())
            .withDepartureTimes("00:00 01:00 02:00")
            .build()
        )
      )
      .build();
    var TP2 = tripPattern("TP2", R2)
      .withStopPattern(stopPattern(S21, S22, S23))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of()
            .withTrip(
              TransitRepositoryForTest.trip("T2").withBikesAllowed(BikeAccess.ALLOWED).build()
            )
            .withDepartureTimes("00:00 01:00 02:00")
            .build()
        )
      )
      .build();
    repo.addTripPattern(id("TP1"), TP1);
    repo.addTripPattern(id("TP2"), TP2);
    assertEquals(Set.of(S21, S22, S23), repo.getStopLocationsUsedForBikesAllowedTrips());
  }

  @Test
  void testGetStopLocationsUsedForCarsAllowedTrips() {
    var repo = new TransitRepository();
    var S11 = TransitRepositoryForTest.of().stop("S11").build();
    var S12 = TransitRepositoryForTest.of().stop("S12").build();
    var S13 = TransitRepositoryForTest.of().stop("S13").build();
    var S21 = TransitRepositoryForTest.of().stop("S21").build();
    var S22 = TransitRepositoryForTest.of().stop("S22").build();
    var S23 = TransitRepositoryForTest.of().stop("S23").build();
    var R1 = route("R1").withMode(TransitMode.RAIL).build();
    var R2 = route("R2").withMode(TransitMode.RAIL).build();
    var TP1 = tripPattern("TP1", R1)
      .withStopPattern(stopPattern(S11, S12, S13))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of()
            .withTrip(TransitRepositoryForTest.trip("T1").build())
            .withDepartureTimes("00:00 01:00 02:00")
            .build()
        )
      )
      .build();
    var TP2 = tripPattern("TP2", R2)
      .withStopPattern(stopPattern(S21, S22, S23))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of()
            .withTrip(
              TransitRepositoryForTest.trip("T2").withCarsAllowed(CarAccess.ALLOWED).build()
            )
            .withDepartureTimes("00:00 01:00 02:00")
            .build()
        )
      )
      .build();
    repo.addTripPattern(id("TP1"), TP1);
    repo.addTripPattern(id("TP2"), TP2);
    assertEquals(Set.of(S21, S22, S23), repo.getStopLocationsUsedForCarsAllowedTrips());
  }
}
