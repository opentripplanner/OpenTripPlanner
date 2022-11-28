package org.opentripplanner.graph_builder.module.interlining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.stopTime;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.StaySeatedNotAllowed;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.transfer.DefaultTransferService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

class InterlineProcessorTest implements PlanTestConstants {

  List<TripPattern> patterns = List.of(
    tripPattern("trip-1", "block-1"),
    tripPattern("trip-2", "block-1"),
    tripPattern("trip-2", "block-3")
  );

  @Test
  void run() {
    var transferService = new DefaultTransferService();
    var processor = new InterlineProcessor(
      transferService,
      List.of(),
      100,
      DataImportIssueStore.NOOP
    );

    var createdTransfers = processor.run(patterns);
    assertEquals(1, createdTransfers.size());

    assertEquals(transferService.listAll(), createdTransfers);

    createdTransfers.forEach(t -> assertTrue(t.getTransferConstraint().isStaySeated()));
  }

  @Test
  void staySeatedNotAllowed() {
    var transferService = new DefaultTransferService();

    var fromTrip = patterns.get(0).getTrip(0);
    var toTrip = patterns.get(1).getTrip(0);

    var notAllowed = new StaySeatedNotAllowed(fromTrip, toTrip);

    var processor = new InterlineProcessor(
      transferService,
      List.of(notAllowed),
      100,
      DataImportIssueStore.NOOP
    );

    var createdTransfers = processor.run(patterns);
    assertEquals(0, createdTransfers.size());

    assertEquals(transferService.listAll(), createdTransfers);
  }

  private static TripPattern tripPattern(String tripId, String blockId) {
    var trip = TransitModelForTest
      .trip(tripId)
      .withGtfsBlockId(blockId)
      .withServiceId(new FeedScopedId("1", "1"))
      .build();

    var stopTimes = List.of(stopTime(trip, 0), stopTime(trip, 1), stopTime(trip, 2));
    var stopPattern = new StopPattern(stopTimes);

    var tp = TripPattern
      .of(TransitModelForTest.id(tripId))
      .withRoute(trip.getRoute())
      .withStopPattern(stopPattern)
      .build();
    var tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
    tp.add(tripTimes);
    return tp;
  }
}
