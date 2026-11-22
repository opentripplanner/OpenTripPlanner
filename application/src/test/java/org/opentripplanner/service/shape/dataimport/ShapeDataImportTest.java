package org.opentripplanner.service.shape.dataimport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;

class ShapeDataImportTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final TripPattern PATTERN = TransitRepositoryForTest.tripPattern(
    "A",
    TransitRepositoryForTest.route("route").build()
  )
    .withStopPattern(
      TransitRepositoryForTest.stopPattern(
        TEST_MODEL.stop("A").build(),
        TEST_MODEL.stop("B").build()
      )
    )
    .build();
  private static final CompactLineStringSequence TRIP_PATTERN_SHAPE = ShapeTestFactory.straightLine(
    60.0
  );

  @Test
  void storesTripPatternShape() {
    var subject = new ShapeDataImport();
    assertNull(subject.getTripPatternShape(id("A")));

    subject.setTripPatternShape(PATTERN, TRIP_PATTERN_SHAPE);

    assertEquals(TRIP_PATTERN_SHAPE, subject.getTripPatternShape(id("A")));
  }

  @Test
  void convertsToScheduledData() {
    var subject = new ShapeDataImport();
    subject.setTripPatternShape(PATTERN, TRIP_PATTERN_SHAPE);

    var scheduled = subject.toScheduledShapeData();

    assertEquals(TRIP_PATTERN_SHAPE, scheduled.getTripPatternShape(id("A")));
  }

  @Test
  void modificationsAfterConversionFail() {
    var subject = new ShapeDataImport();
    subject.toScheduledShapeData();

    var exception = assertThrows(IllegalStateException.class, () ->
      subject.setTripPatternShape(PATTERN, TRIP_PATTERN_SHAPE)
    );
    assertThat(exception).hasMessageThat().contains("frozen");
  }

  @Test
  void rejectAMissingTripPatternShape() {
    var subject = new ShapeDataImport();

    var exception = assertThrows(IllegalArgumentException.class, () ->
      subject.setTripPatternShape(PATTERN, null)
    );
    assertThat(exception).hasMessageThat().contains("No shape for trip pattern");
    assertNull(subject.getTripPatternShape(id("A")));
  }

  @Test
  void rejectATripPatternShapeWithTheWrongNumberOfHops() {
    var subject = new ShapeDataImport();
    var twoHops = TripShapeFactory.fromLineStrings(
      List.of(makeLineString(10.0, 59.0, 10.1, 59.1), makeLineString(10.1, 59.1, 10.2, 59.0)),
      DeduplicatorService.NOOP
    );

    var exception = assertThrows(IllegalArgumentException.class, () ->
      subject.setTripPatternShape(PATTERN, twoHops)
    );
    assertThat(exception).hasMessageThat().contains("2 hops, expected 1");
    assertNull(subject.getTripPatternShape(id("A")));
  }
}
