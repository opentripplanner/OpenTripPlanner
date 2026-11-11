package org.opentripplanner.updater.trip.shape;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;

/**
 * Makes sure that a trip pattern created by a real-time update has a shape in the
 * {@link ShapeRepository}. The shape itself is created by {@link RealTimeTripShapeFactory}.
 * <p>
 * The writer is idempotent: the {@link TripPatternCache} outlives the update task that created the
 * pattern, so a pattern may be seen again by a later task even though its shape has been removed in
 * the meantime, see {@link ShapeRepository#clear(String)}. A missing shape is created again. If
 * the original pattern is itself a real-time pattern whose shape has been removed, straight lines
 * are used.
 */
public class RealTimeTripShapeWriter {

  /**
   * Resolved lazily, so that tasks that do not touch any real-time pattern do not cause a new shape
   * snapshot to be published at commit.
   */
  private final Supplier<ShapeRepository> shapes;
  private final RealTimeTripShapeFactory factory;

  public RealTimeTripShapeWriter(
    Supplier<ShapeRepository> shapes,
    RealTimeTripShapeFactory factory
  ) {
    this.shapes = shapes;
    this.factory = factory;
  }

  /**
   * Write the shape of the given pattern, unless the pattern is not created in real-time or the
   * shape is already present.
   */
  public void ensureTripPatternShape(TripPattern pattern) {
    if (!pattern.isRealTimeTripPattern()) {
      return;
    }
    var repository = shapes.get();
    if (repository.getTripPatternShape(pattern.getId()) != null) {
      return;
    }
    repository.setTripPatternShape(
      pattern.getId(),
      factory.createTripPatternShape(pattern, getOriginalTripPatternShape(pattern, repository))
    );
  }

  @Nullable
  private static CompactLineStringSequence getOriginalTripPatternShape(
    TripPattern pattern,
    ShapeRepository repository
  ) {
    var original = pattern.getOriginalTripPattern();
    if (original == null) {
      return null;
    }
    var shape = repository.getTripPatternShape(original.getId());
    // A real-time original can lack a shape if it was cleared while the pattern lives on in the
    // cache. Only a scheduled original is expected to always have one.
    if (shape == null && !original.isRealTimeTripPattern()) {
      throw new IllegalStateException(
        "No shape found for the original trip pattern " + original.getId()
      );
    }
    return shape;
  }
}
