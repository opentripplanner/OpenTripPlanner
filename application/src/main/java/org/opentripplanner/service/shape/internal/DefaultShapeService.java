package org.opentripplanner.service.shape.internal;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;
import org.opentripplanner.service.shape.ShapeService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * In request scope a new instance should be created for each request with a snapshot resolved from
 * the same transaction scope as the rest of the request, so that the whole request sees one
 * consistent view. The mutable {@code ShapeRepository} also implements the snapshot interface, so a
 * service over it can be used on the writer thread to see in-progress additions. Such an instance
 * must never be given to request threads.
 */
public class DefaultShapeService implements ShapeService {

  private final ShapeRepositorySnapshot shapes;

  public DefaultShapeService(ShapeRepositorySnapshot shapes) {
    this.shapes = shapes;
  }

  @Override
  public CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId) {
    var tripPatternGeometry = shapes.getTripPatternGeometry(tripPatternId);
    if (tripPatternGeometry == null) {
      throw new IllegalStateException("No geometry found for trip pattern " + tripPatternId);
    }
    return tripPatternGeometry;
  }
}
