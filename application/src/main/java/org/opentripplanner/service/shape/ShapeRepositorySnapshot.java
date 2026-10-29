package org.opentripplanner.service.shape;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * An immutable, read-only snapshot of the shape data: the geometries of the scheduled trip patterns
 * and of the patterns created by real-time updates. A new snapshot is published each time a
 * transaction that touched the {@link ShapeRepository} commits. Request threads read a snapshot
 * resolved at the start of the request, usually through the request-scoped {@link ShapeService}.
 * <p>
 * The mutable {@link ShapeRepository} extends this interface, so that the writer thread can read
 * its own in-progress additions in the same way.
 */
public interface ShapeRepositorySnapshot {
  /**
   * The geometry of the trip pattern with the given id. Geometries stored by real-time updates
   * take precedence over the scheduled ones. Returns {@code null} only for a real-time pattern
   * whose geometry has not been written yet, see {@link ShapeService}.
   */
  @Nullable
  CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId);
}
