package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

class TransitModelTest {

  @Test
  void validateTimeZones() {
    // First GTFS bundle should be added successfully
    var model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);
    TransitModel transitModel = model.transitModel;
    TransitModelIndex transitModelIndex = transitModel.getTransitModelIndex();

    // Then time zone should match the one provided in the feed
    assertEquals("America/New_York", transitModel.getTimeZone().getId());

    // Then trip times should be same as in input data
    Trip trip = transitModelIndex.getTripForId().get(new FeedScopedId("1", "1.2"));
    Timetable timetable = transitModelIndex.getPatternForTrip().get(trip).getScheduledTimetable();
    assertEquals(20 * 60, timetable.getTripTimes(trip).getDepartureTime(0));

    // Should throw on second bundle, with different agency time zone
    assertThrows(
      IllegalStateException.class,
      () ->
        ConstantsForTests.addGtfsToGraph(
          model.graph,
          transitModel,
          ConstantsForTests.KCM_GTFS,
          new DefaultFareServiceFactory(),
          null
        ),
      "The graph contains agencies with different time zones. Please configure the one to be used in the build-config.json"
    );
  }

  @Test
  void validateTimeZonesWithExplicitTimeZone() {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(stopModel, deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);

    // Whit explicit time zone
    transitModel.initTimeZone(ZoneId.of("America/Chicago"));

    // First GTFS bundle should be added successfully
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitModel,
      ConstantsForTests.FAKE_GTFS,
      new DefaultFareServiceFactory(),
      null
    );

    // Should load second bundle, with different agency time zone
    ConstantsForTests.addGtfsToGraph(
      graph,
      transitModel,
      ConstantsForTests.KCM_GTFS,
      new DefaultFareServiceFactory(),
      null
    );

    new TimeZoneAdjusterModule().buildGraph(graph, transitModel, null);

    TransitModelIndex transitModelIndex = transitModel.getTransitModelIndex();

    // Then time zone should match the one provided in the feed
    assertEquals("America/Chicago", transitModel.getTimeZone().getId());

    // Then trip times should be on hour less than in input data
    Trip trip = transitModelIndex.getTripForId().get(new FeedScopedId("1", "1.2"));
    Timetable timetable = transitModelIndex.getPatternForTrip().get(trip).getScheduledTimetable();
    assertEquals(20 * 60 - 60 * 60, timetable.getTripTimes(trip).getDepartureTime(0));
  }
}
