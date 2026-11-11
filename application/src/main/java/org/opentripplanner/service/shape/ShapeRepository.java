package org.opentripplanner.service.shape;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * The mutable repository of the shape data. It owns the {@link ScheduledShapeData} - the
 * shapes of the scheduled trip patterns - and holds the shapes of the trip patterns created
 * by real-time updates on top of it.
 * <p>
 * It is managed by the transaction framework: updaters obtain it through a {@code WriteContext} on
 * the single writer thread, and {@link #createSnapshot()} is called at commit time to publish a new
 * immutable {@link ShapeRepositorySnapshot}.
 */
public interface ShapeRepository extends ShapeRepositorySnapshot {
  /**
   * Store the shape of the given real-time trip pattern. An existing real-time shape for the
   * same pattern is replaced. The scheduled data is never modified.
   */
  void setTripPatternShape(FeedScopedId tripPatternId, CompactLineStringSequence tripPatternShape);

  /**
   * Remove the shapes of all real-time trip patterns of the given feed; they are stored per
   * feed. The scheduled data is never modified.
   */
  void clear(String feedId);

  /**
   * Produce an immutable snapshot of the current state of this repository.
   */
  ShapeRepositorySnapshot createSnapshot();
}
