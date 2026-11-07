package org.opentripplanner.service.shape;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * A read-only, request-scoped view over a {@link ShapeRepositorySnapshot}: the scheduled geometries
 * imported at graph build time, overridden by the geometries of real-time patterns. It serves the
 * geometry of trip patterns: the geometry of each hop between two
 * consecutive stops and the cumulative distance along the pattern.
 * <p>
 * For patterns built without shape data (GTFS without {@code shapes.txt}, NeTEx without ServiceLink
 * projections, real-time added trips), the geometry consists of straight lines between the stops.
 * Every scheduled trip pattern and every real-time pattern that has been processed by a real-time
 * updater has a geometry. Only a new real-time pattern that is still being created inside an
 * update transaction has none; such a pattern is never visible here, because this service reads
 * committed snapshots only. Asking for the geometry of an unknown pattern is therefore a bug and
 * results in an {@link IllegalStateException}.
 */
public interface ShapeService {
  /**
   * The geometry of the trip pattern: one hop geometry for each pair of consecutive stops and the
   * cumulative distance table. The code that builds scheduled transit legs resolves the geometry
   * of the leg's pattern from this service and gives it to the leg.
   */
  CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId);
}
