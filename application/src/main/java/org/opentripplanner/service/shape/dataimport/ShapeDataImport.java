package org.opentripplanner.service.shape.dataimport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

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
   * Set when the content has been converted to {@link ScheduledShapeData}. Not serialized: a
   * deserialized import has not been converted yet.
   */
  private transient boolean frozen = false;

  public ShapeDataImport() {}

  /**
   * Store the geometry of the given scheduled trip pattern. An existing geometry for the same
   * pattern is replaced.
   */
  public void setTripPatternGeometry(
    FeedScopedId tripPatternId,
    CompactLineStringSequence tripPatternGeometry
  ) {
    assertModificationsAllowed();
    geometriesByTripPattern.put(tripPatternId, tripPatternGeometry);
  }

  @Nullable
  public CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId) {
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
