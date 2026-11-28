package org.opentripplanner.service.shape.internal;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;
import org.opentripplanner.service.shape.ShapeService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * A service over the given {@link ShapeRepositorySnapshot}. What it sees depends on what it is
 * created over:
 * <ul>
 *   <li>In request scope, create one per request over the snapshot of the request's transaction
 *   scope, so that the whole request sees one consistent view.</li>
 *   <li>On the writer thread, it can be created over the mutable {@code ShapeRepository}, which also
 *   implements the snapshot interface, to see the shapes added by the ongoing update. Such an
 *   instance must never be given to request threads.</li>
 * </ul>
 */
public class DefaultShapeService implements ShapeService {

  private final ShapeRepositorySnapshot shapes;

  public DefaultShapeService(ShapeRepositorySnapshot shapes) {
    this.shapes = shapes;
  }

  @Override
  public CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId) {
    var tripPatternShape = shapes.getTripPatternShape(tripPatternId);
    if (tripPatternShape == null) {
      throw new IllegalArgumentException("No shape found for trip pattern " + tripPatternId);
    }
    return tripPatternShape;
  }
}
