package org.opentripplanner.graph_builder.module.geometry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.NOOP;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;
import static org.opentripplanner.street.geometry.SphericalDistanceLibrary.distance;
import static org.opentripplanner.street.geometry.SphericalDistanceLibrary.moveMeters;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.LineString;
import org.opentest4j.AssertionFailedError;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

class GeometryProcessorTest {

  public static final FeedScopedId SHAPE_ID = id("s1");

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final RegularStop STOP_A = TEST_MODEL.stop("A").withCoordinate(0, 0).build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").withCoordinate(0.1, 0).build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C").withCoordinate(0.2, 0).build();
  private static final RegularStop STOP_D = TEST_MODEL.stop("D").withCoordinate(0.2, 0.1).build();
  private static final RegularStop STOP_E = TEST_MODEL.stop("E").withCoordinate(0.2, 0.2).build();
  private static final RegularStop STOP_F = TEST_MODEL.stop("F").withCoordinate(0.1, 0.2).build();
  private static final RegularStop STOP_G = TEST_MODEL.stop("G").withCoordinate(0, 0.2).build();
  private static final RegularStop STOP_H = TEST_MODEL.stop("H").withCoordinate(0, 0.1).build();
  private static final RegularStop STOP_BL = TEST_MODEL.stop("BL")
    .withCoordinate(moveMeters(new WgsCoordinate(0.1, 0), 0, -1))
    .build();
  private static final RegularStop STOP_BR = TEST_MODEL.stop("BR")
    .withCoordinate(moveMeters(new WgsCoordinate(0.1, 0), 0, 1))
    .build();
  private static final RegularStop STOP_X = TEST_MODEL.stop("X").withCoordinate(0, -0.1).build();
  private static final RegularStop STOP_Y = TEST_MODEL.stop("Y").withCoordinate(0, 0.3).build();
  private static final SiteRepository REPO = TEST_MODEL.siteRepositoryBuilder()
    .withRegularStops(
      List.of(
        STOP_A,
        STOP_B,
        STOP_C,
        STOP_D,
        STOP_E,
        STOP_F,
        STOP_G,
        STOP_H,
        STOP_BL,
        STOP_BR,
        STOP_X,
        STOP_Y
      )
    )
    .build();

  private static final double TOLERANCE = 1.0;

  private static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.argumentSet(
        "empty shape",
        List.of(STOP_A, STOP_B, STOP_C),
        List.of(),
        List.of(makeLineString(0, 0, 0, 0.1), makeLineString(0, 0.1, 0, 0.2))
      ),
      Arguments.argumentSet(
        "shape point exactly at stops",
        List.of(STOP_A, STOP_B, STOP_C),
        List.of(new ShapePoint(0, 0, 0, null), new ShapePoint(1, 0.2, 0, null)),
        List.of(makeLineString(0, 0, 0, 0.1), makeLineString(0, 0.1, 0, 0.2))
      ),
      Arguments.argumentSet(
        "intermediate stop in the middle of a segment",
        List.of(STOP_A, STOP_B, STOP_C),
        List.of(new ShapePoint(0, 0, 0, null), new ShapePoint(1, 0.3, 0, null)),
        List.of(makeLineString(0, 0, 0, 0.1), makeLineString(0, 0.1, 0, 0.2))
      ),
      Arguments.argumentSet(
        "zigzag shape",
        List.of(STOP_A, STOP_B, STOP_C),
        List.of(
          new ShapePoint(0, 0, 0, null),
          new ShapePoint(1, 0, 0.1, null),
          new ShapePoint(2, 0.1, 0.1, null),
          new ShapePoint(3, 0.1, -0.1, null),
          new ShapePoint(4, 0.2, -0.1, null),
          new ShapePoint(5, 0.2, 0, null)
        ),
        List.of(
          makeLineString(0, 0, 0.1, 0, 0.1, 0.1, 0, 0.1),
          makeLineString(0, 0.1, -0.1, 0.1, -0.1, 0.2, 0, 0.2)
        )
      ),
      Arguments.argumentSet(
        "simple double-back",
        List.of(STOP_A, STOP_B, STOP_C, STOP_B, STOP_A),
        List.of(
          new ShapePoint(0, 0, 0, null),
          new ShapePoint(1, 0.2, 0, null),
          new ShapePoint(2, 0, 0, null)
        ),
        List.of(
          makeLineString(0, 0, 0, 0.1),
          makeLineString(0, 0.1, 0, 0.2),
          makeLineString(0, 0.2, 0, 0.1),
          makeLineString(0, 0.1, 0, 0)
        )
      ),
      Arguments.argumentSet(
        "double-back with stops on both sides of the road",
        List.of(STOP_A, STOP_BL, STOP_C, STOP_BR, STOP_A),
        List.of(
          new ShapePoint(0, 0, 0, null),
          new ShapePoint(1, 0.2, 0, null),
          new ShapePoint(2, 0, 0, null)
        ),
        List.of(
          makeLineString(0, 0, 0, 0.1),
          makeLineString(0, 0.1, 0, 0.2),
          makeLineString(0, 0.2, 0, 0.1),
          makeLineString(0, 0.1, 0, 0)
        )
      ),
      Arguments.argumentSet(
        "double-back at a turning circle further than the stop",
        List.of(STOP_A, STOP_C, STOP_A),
        List.of(
          new ShapePoint(0, 0, 0, null),
          new ShapePoint(1, 0.3, 0, null),
          new ShapePoint(2, 0, 0, null)
        ),
        List.of(
          makeLineString(0, 0, 0, 0.2),
          // assume that the bus call at the first instance passing through the stop
          makeLineString(0, 0.2, 0, 0.3, 0, 0)
        )
      ),
      Arguments.argumentSet(
        "double-back at a turning circle further than the stop with two stops",
        List.of(STOP_A, STOP_C, STOP_B, STOP_A),
        List.of(
          new ShapePoint(0, 0, 0, null),
          new ShapePoint(1, 0.3, 0, null),
          new ShapePoint(2, 0, 0, null)
        ),
        List.of(
          makeLineString(0, 0, 0, 0.2),
          makeLineString(0, 0.2, 0, 0.3, 0, 0.1),
          // the bus has to call at the return
          makeLineString(0, 0.1, 0, 0)
        )
      ),
      Arguments.argumentSet(
        "calling on the other side of the road after turning back",
        List.of(STOP_A, STOP_BR, STOP_A),
        // driving on the left, carriages approx. 2 m apart
        List.of(
          new ShapePoint(0, 0, -9e-6, null),
          new ShapePoint(1, 0.3, -9e-6, null),
          new ShapePoint(2, 0.3, 9e-6, null),
          new ShapePoint(3, 0, 9e-6, null)
        ),
        List.of(
          // the bus has to call at the return
          makeLineString(-9e-6, 0, -9e-6, 0.3, 9e-6, 0.3, 9e-6, 0.1),
          makeLineString(9e-6, 0.1, 9e-6, 0)
        )
      ),
      Arguments.argumentSet(
        "simple loop",
        List.of(STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H, STOP_A),
        List.of(
          new ShapePoint(0, 0, 0, null),
          new ShapePoint(1, 0.2, 0, null),
          new ShapePoint(2, 0.2, 0.2, null),
          new ShapePoint(3, 0, 0.2, null),
          new ShapePoint(4, 0, 0, null)
        ),
        List.of(
          makeLineString(0, 0, 0, 0.1),
          makeLineString(0, 0.1, 0, 0.2),
          makeLineString(0, 0.2, 0.1, 0.2),
          makeLineString(0.1, 0.2, 0.2, 0.2),
          makeLineString(0.2, 0.2, 0.2, 0.1),
          makeLineString(0.2, 0.1, 0.2, 0),
          makeLineString(0.2, 0, 0.1, 0),
          makeLineString(0.1, 0, 0, 0)
        )
      ),
      Arguments.argumentSet(
        "loop and continue, passing some stops non-stop",
        List.of(STOP_Y, STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H, STOP_X),
        List.of(
          new ShapePoint(0, 0, 0.3, null),
          new ShapePoint(1, 0, 0, null),
          new ShapePoint(2, 0.2, 0, null),
          new ShapePoint(3, 0.2, 0.2, null),
          new ShapePoint(4, 0, 0.2, null),
          new ShapePoint(5, 0, -0.1, null)
        ),
        List.of(
          makeLineString(0.3, 0, 0, 0),
          makeLineString(0, 0, 0, 0.1),
          makeLineString(0, 0.1, 0, 0.2),
          makeLineString(0, 0.2, 0.1, 0.2),
          makeLineString(0.1, 0.2, 0.2, 0.2),
          makeLineString(0.2, 0.2, 0.2, 0.1),
          makeLineString(0.2, 0.1, 0.2, 0),
          makeLineString(0.2, 0, 0.1, 0),
          makeLineString(0.1, 0, -0.1, 0)
        )
      ),
      Arguments.argumentSet(
        "loop and continue with double calling",
        List.of(
          STOP_Y,
          STOP_G,
          STOP_H,
          STOP_A,
          STOP_B,
          STOP_C,
          STOP_D,
          STOP_E,
          STOP_F,
          STOP_G,
          STOP_H,
          STOP_A,
          STOP_X
        ),
        List.of(
          new ShapePoint(0, 0, 0.3, null),
          new ShapePoint(1, 0, 0, null),
          new ShapePoint(2, 0.2, 0, null),
          new ShapePoint(3, 0.2, 0.2, null),
          new ShapePoint(4, 0, 0.2, null),
          new ShapePoint(5, 0, -0.1, null)
        ),
        List.of(
          makeLineString(0.3, 0, 0.2, 0),
          makeLineString(0.2, 0, 0.1, 0),
          makeLineString(0.1, 0, 0, 0),
          makeLineString(0, 0, 0, 0.1),
          makeLineString(0, 0.1, 0, 0.2),
          makeLineString(0, 0.2, 0.1, 0.2),
          makeLineString(0.1, 0.2, 0.2, 0.2),
          makeLineString(0.2, 0.2, 0.2, 0.1),
          makeLineString(0.2, 0.1, 0.2, 0),
          makeLineString(0.2, 0, 0.1, 0),
          makeLineString(0.1, 0, 0, 0),
          makeLineString(0.0, 0, -0.1, 0)
        )
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void test(List<RegularStop> stops, List<ShapePoint> shapePoints, List<LineString> expected) {
    var builder = new TransitDataImportBuilder(REPO, NOOP);

    var trip = TimetableRepositoryForTest.trip("t").withShapeId(SHAPE_ID).build();

    var stopTimes = IntStream.range(0, stops.size())
      .mapToObj(index -> TEST_MODEL.stopTime(trip, index, stops.get(index)))
      .toList();
    builder.getStopTimesSortedByTrip().put(trip, stopTimes);

    builder.getShapePoints().put(SHAPE_ID, shapePoints);

    var processor = new GeometryProcessor(builder, 150, NOOP);
    var linestrings = processor.createHopGeometries(trip);

    assertLineStringWithinTolerance(expected, linestrings);
  }

  @Test
  void testShapeDistance() {
    var builder = new TransitDataImportBuilder(REPO, NOOP);

    var trip = TimetableRepositoryForTest.trip("t").withShapeId(SHAPE_ID).build();

    var stopTimes = List.of(
      TEST_MODEL.stopTime(trip, 0, STOP_A),
      TEST_MODEL.stopTime(trip, 1, STOP_B),
      TEST_MODEL.stopTime(trip, 2, STOP_A)
    );
    stopTimes.get(0).setShapeDistTraveled(0);
    stopTimes.get(1).setShapeDistTraveled(3);
    stopTimes.get(2).setShapeDistTraveled(4);
    builder.getStopTimesSortedByTrip().put(trip, stopTimes);

    builder
      .getShapePoints()
      .put(
        SHAPE_ID,
        List.of(
          new ShapePoint(0, 0, 0, 0.0),
          new ShapePoint(1, 0.2, 0, 2.0),
          new ShapePoint(2, 0, 0, 4.0)
        )
      );

    var processor = new GeometryProcessor(builder, 150, NOOP);
    var linestrings = processor.createHopGeometries(trip);
    var expected = List.of(
      // the bus has to call at the return because of the shape distance traveled
      makeLineString(0, 0, 0, 0.2, 0, 0.1),
      makeLineString(0, 0.1, 0, 0)
    );

    assertLineStringWithinTolerance(expected, linestrings);
  }

  @Test
  void ignoreInvalidReference() {
    var issueStore = new DefaultDataImportIssueStore();
    var builder = new TransitDataImportBuilder(REPO, issueStore);
    builder
      .getShapePoints()
      .put(SHAPE_ID, List.of(new ShapePoint(0, 0, 0, 0.0), new ShapePoint(1, 1, 1, 1.0)));

    var invalidRef = id("unknown");
    var trip = TimetableRepositoryForTest.trip("t").withShapeId(invalidRef).build();

    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "8:00");
    builder.getStopTimesSortedByTrip().put(trip, stopTimes);

    var processor = new GeometryProcessor(builder, 150, issueStore);
    var linestrings = processor.createHopGeometries(trip);

    assertThat(linestrings).hasSize(2);

    assertEquals(
      "[Issue{type: 'InvalidShapeReference', message: 'Trip 'F:t' refers to unknown shape geometry 'F:unknown''}]",
      issueStore.listIssues().toString()
    );
  }

  private static void assertLineStringWithinTolerance(
    List<LineString> expected,
    List<LineString> actual
  ) {
    if (expected.size() != actual.size()) {
      throw new AssertionFailedError(null, expected, actual);
    }
    for (int i = 0; i < expected.size(); i++) {
      var expectedLineString = expected.get(i);
      var actualLineString = actual.get(i);
      if (expectedLineString.getNumPoints() != actualLineString.getNumPoints()) {
        throw new AssertionFailedError(null, expected, actual);
      }
      for (int j = 0; j < expectedLineString.getNumPoints(); j++) {
        if (
          distance(expectedLineString.getCoordinateN(j), actualLineString.getCoordinateN(j)) >
          TOLERANCE
        ) {
          throw new AssertionFailedError(null, expected, actual);
        }
      }
    }
  }
}
