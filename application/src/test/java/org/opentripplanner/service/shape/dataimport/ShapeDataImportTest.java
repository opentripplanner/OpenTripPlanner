package org.opentripplanner.service.shape.dataimport;

import static com.google.common.truth.Truth.assertThat;
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
  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(60.0);

  @Test
  void storesGeometry() {
    var subject = new ShapeDataImport();
    assertThat(subject.getTripPatternShape(id("A"))).isNull();

    subject.setTripPatternShape(PATTERN, TRIP_PATTERN_GEOMETRY);

    assertThat(subject.getTripPatternShape(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void convertsToScheduledData() {
    var subject = new ShapeDataImport();
    subject.setTripPatternShape(PATTERN, TRIP_PATTERN_GEOMETRY);

    var scheduled = subject.toScheduledShapeData();

    assertThat(scheduled.getTripPatternShape(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void modificationsAfterConversionFail() {
    var subject = new ShapeDataImport();
    subject.toScheduledShapeData();

    var exception = assertThrows(IllegalStateException.class, () ->
      subject.setTripPatternShape(PATTERN, TRIP_PATTERN_GEOMETRY)
    );
    assertThat(exception).hasMessageThat().contains("frozen");
  }

  @Test
  void rejectAMissingShape() {
    var subject = new ShapeDataImport();

    var exception = assertThrows(IllegalArgumentException.class, () ->
      subject.setTripPatternShape(PATTERN, null)
    );
    assertThat(exception).hasMessageThat().contains("No shape for trip pattern");
    assertThat(subject.getTripPatternShape(id("A"))).isNull();
  }

  @Test
  void rejectAShapeWithTheWrongNumberOfHops() {
    var subject = new ShapeDataImport();
    var twoHops = TripShapeFactory.fromLineStrings(
      List.of(makeLineString(10.0, 59.0, 10.1, 59.1), makeLineString(10.1, 59.1, 10.2, 59.0)),
      DeduplicatorService.NOOP
    );

    var exception = assertThrows(IllegalArgumentException.class, () ->
      subject.setTripPatternShape(PATTERN, twoHops)
    );
    assertThat(exception).hasMessageThat().contains("2 hops, expected 1");
    assertThat(subject.getTripPatternShape(id("A"))).isNull();
  }
}
