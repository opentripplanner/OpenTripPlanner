package org.opentripplanner.service.shape;

import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * The geometries of the scheduled (non-real-time) trip patterns.
 * <p>
 * Immutable: it holds an immutable copy of the data it is created from and never changes, so it
 * can be shared freely, for example between threads.
 */
public final class ScheduledShapeData {

  private static final ScheduledShapeData EMPTY = new ScheduledShapeData(Map.of());

  private final Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern;

  private ScheduledShapeData(Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern) {
    this.geometriesByTripPattern = geometriesByTripPattern;
  }

  /**
   * Create the immutable scheduled data. The given map is copied, so later changes to it are not
   * reflected. The geometry instances are shared, not copied.
   */
  public static ScheduledShapeData of(
    Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern
  ) {
    return new ScheduledShapeData(Map.copyOf(geometriesByTripPattern));
  }

  public static ScheduledShapeData empty() {
    return EMPTY;
  }

  @Nullable
  public CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId) {
    return geometriesByTripPattern.get(tripPatternId);
  }
}
