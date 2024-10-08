package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

class TransitModelTest {

  public static final String FAKE_FEED_ID = "FAKE";
  public static final FeedScopedId SAMPLE_TRIP_ID = new FeedScopedId(FAKE_FEED_ID, "1.2");
  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(TransitModelTest.class);

  @Test
  void validateTimeZones() {
    // First GTFS bundle should be added successfully
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitModel,
      ConstantsForTests.SIMPLE_GTFS,
      new DefaultFareServiceFactory(),
      FAKE_FEED_ID
    );

    // Then time zone should match the one provided in the feed
    assertEquals("America/New_York", transitModel.getTimeZone().getId());

    // Then trip times should be same as in input data
    TransitModelIndex transitModelIndex = transitModel.getTransitModelIndex();
    Trip trip = transitModelIndex.getTripForId().get(SAMPLE_TRIP_ID);
    Timetable timetable = transitModelIndex.getPatternForTrip().get(trip).getScheduledTimetable();
    assertEquals(20 * 60, timetable.getTripTimes(trip).getDepartureTime(0));

    // Should throw on second bundle, with different agency time zone
    assertThrows(
      IllegalStateException.class,
      () ->
        ConstantsForTests.addGtfsToGraph(
          graph,
          transitModel,
          RESOURCE_LOADER.file("kcm_gtfs.zip"),
          new DefaultFareServiceFactory(),
          null
        ),
      (
        "The graph contains agencies with different time zones. " +
        "Please configure the one to be used in the " +
        BUILD_CONFIG_FILENAME
      )
    );
  }

  @Test
  void validateTimeZonesWithExplicitTimeZone() {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);

    // Whit explicit time zone
    transitModel.initTimeZone(ZoneIds.CHICAGO);

    // First GTFS bundle should be added successfully
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitModel,
      ConstantsForTests.SIMPLE_GTFS,
      new DefaultFareServiceFactory(),
      FAKE_FEED_ID
    );

    // Should load second bundle, with different agency time zone
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitModel,
      RESOURCE_LOADER.file("kcm_gtfs.zip"),
      new DefaultFareServiceFactory(),
      null
    );

    new TimeZoneAdjusterModule(transitModel).buildGraph();

    TransitModelIndex transitModelIndex = transitModel.getTransitModelIndex();

    // Then time zone should match the one provided in the feed
    assertEquals("America/Chicago", transitModel.getTimeZone().getId());

    // Then trip times should be on hour less than in input data
    Trip trip = transitModelIndex.getTripForId().get(SAMPLE_TRIP_ID);
    Timetable timetable = transitModelIndex.getPatternForTrip().get(trip).getScheduledTimetable();
    assertEquals(20 * 60 - 60 * 60, timetable.getTripTimes(trip).getDepartureTime(0));
  }
}
