package org.opentripplanner.service.shape.dataimport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;

class ShapeDataImportTest {

  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY =
    TripShapeFactory.fromLineStrings(
      List.of(makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0)),
      DeduplicatorService.NOOP
    );

  @Test
  void storesGeometry() {
    var subject = new ShapeDataImport();
    assertThat(subject.getTripPatternShape(id("A"))).isNull();

    subject.setTripPatternShape(id("A"), TRIP_PATTERN_GEOMETRY);

    assertThat(subject.getTripPatternShape(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void convertsToScheduledData() {
    var subject = new ShapeDataImport();
    subject.setTripPatternShape(id("A"), TRIP_PATTERN_GEOMETRY);

    var scheduled = subject.toScheduledShapeData();

    assertThat(scheduled.getTripPatternShape(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void modificationsAfterConversionFail() {
    var subject = new ShapeDataImport();
    subject.toScheduledShapeData();

    assertThrows(IllegalStateException.class, () ->
      subject.setTripPatternShape(id("A"), TRIP_PATTERN_GEOMETRY)
    );
  }
}
