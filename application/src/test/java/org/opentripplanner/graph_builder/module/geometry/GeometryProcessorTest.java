package org.opentripplanner.graph_builder.module.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.NOOP;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GeometryProcessorTest {

  public static final FeedScopedId SHAPE_ID = id("s1");

  @Test
  void test() {
    var testModel = TimetableRepositoryForTest.of();
    var stop1 = testModel.stop("1").withCoordinate(0, 0).build();
    var stop2 = testModel.stop("2").withCoordinate(0.1, 0.1).build();
    var stop3 = testModel.stop("3").withCoordinate(0.2, 0.2).build();
    var repo = testModel
      .siteRepositoryBuilder()
      .withRegularStops(List.of(stop1, stop2, stop3))
      .build();

    var builder = new OtpTransitServiceBuilder(repo, NOOP);

    var trip = TimetableRepositoryForTest.trip("t").withShapeId(SHAPE_ID).build();

    var stopTimes = List.of(
      testModel.stopTime(trip, 0, stop1),
      testModel.stopTime(trip, 1, stop2),
      testModel.stopTime(trip, 2, stop3)
    );
    builder.getStopTimesSortedByTrip().put(trip, stopTimes);

    builder.getShapePoints().putAll(SHAPE_ID, List.of());

    var processor = new GeometryProcessor(builder, 150, NOOP);
    var linestrings = processor.createHopGeometries(trip);

    assertEquals(
      linestrings.toString(),
      "[LINESTRING (0 0, 0.1 0.1), LINESTRING (0.1 0.1, 0.2 0.2)]"
    );
  }
}
