package org.opentripplanner.service.shape.dataimport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.TripPatternShapeValidator;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * The mutable shape data collected while the graph is built (GTFS and NeTEx import), and
 * serialized with the graph.
 * <p>
 * The content is converted into the immutable {@link ScheduledShapeData} with
 * {@link #toScheduledShapeData()}. The dependency goes from this data-import class to the shape
 * domain, not the other way around.
 * <p>
 * Converting the content freezes the data import: later modifications would not be reflected in the
 * converted data, so they fail.
 */
public class ShapeDataImport implements Serializable {

  private final Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern =
    new HashMap<>();

  /**
   * Set when the content is converted to {@link ScheduledShapeData}. Not serialized, so a
   * deserialized data import can be modified.
   */
  private transient boolean frozen = false;

  public ShapeDataImport() {}

  /**
   * Store the shape of the given scheduled trip pattern. An existing shape for the same
   * pattern is replaced.
   *
   * @throws IllegalArgumentException if the shape is missing or does not have one hop per pair of
   *                                  stops of the pattern, see {@link TripPatternShapeValidator}
   * @throws IllegalStateException    if the data import is already frozen
   */
  public void setTripPatternShape(
    TripPattern tripPattern,
    CompactLineStringSequence tripPatternShape
  ) {
    assertModificationsAllowed();
    TripPatternShapeValidator.validate(tripPattern, tripPatternShape);
    geometriesByTripPattern.put(tripPattern.getId(), tripPatternShape);
  }

  @Nullable
  public CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId) {
    return geometriesByTripPattern.get(tripPatternId);
  }

  /**
   * Convert the content to the immutable scheduled shape data. The data import is frozen: any later
   * modification fails.
   */
  public ScheduledShapeData toScheduledShapeData() {
    frozen = true;
    return ScheduledShapeData.of(geometriesByTripPattern);
  }

  private void assertModificationsAllowed() {
    if (frozen) {
      throw new IllegalStateException(
        "The shape data import is frozen, it has already been converted into the scheduled " +
          "shape data."
      );
    }
  }
}
