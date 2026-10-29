package org.opentripplanner.service.shape;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * The mutable repository of the shape data. It owns the {@link ScheduledShapeData} - the
 * geometries of the scheduled trip patterns - and holds the geometries of the trip patterns created
 * by real-time updates on top of it.
 * <p>
 * It is managed by the transaction framework: updaters obtain it through a {@code WriteContext} on
 * the single writer thread, and {@link #createSnapshot()} is called at commit time to publish a new
 * immutable {@link ShapeRepositorySnapshot}.
 */
public interface ShapeRepository extends ShapeRepositorySnapshot {
  /**
   * Store the geometry of the given real-time trip pattern. An existing real-time geometry for the
   * same pattern is replaced. The scheduled data is never modified.
   */
  void setTripPatternGeometry(
    FeedScopedId tripPatternId,
    CompactLineStringSequence tripPatternGeometry
  );

  /**
   * Produce an immutable snapshot of the current state of this repository.
   */
  ShapeRepositorySnapshot createSnapshot();
}
