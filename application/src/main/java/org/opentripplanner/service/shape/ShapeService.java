package org.opentripplanner.service.shape;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * A read-only, request-scoped view over a {@link ShapeRepositorySnapshot}: the scheduled shapes
 * imported at graph build time, overridden by the shapes of real-time patterns. It serves the
 * trip pattern shape: the geometry of each hop between two consecutive stops and the cumulative
 * distance along the pattern.
 * <p>
 * For patterns built without shape data (GTFS without {@code shapes.txt}, NeTEx without ServiceLink
 * projections, real-time added trips), the shape consists of straight lines between the stops.
 * Every scheduled trip pattern and every real-time pattern that has been processed by a real-time
 * updater has a shape. Only a new real-time pattern that is still being created inside an
 * update transaction has none; such a pattern is never visible here, because this service reads
 * committed snapshots only. Asking for the shape of an unknown pattern is therefore a bug.
 */
public interface ShapeService {
  /**
   * The shape of the trip pattern: one hop geometry for each pair of consecutive stops and the
   * cumulative distance table. The code that builds scheduled transit legs resolves the shape of
   * the leg's pattern from this service and gives it to the leg.
   *
   * @throws IllegalArgumentException if the committed snapshot has no scheduled or real-time shape
   *                                  for the trip pattern id
   */
  CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId);
}
