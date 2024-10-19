package org.opentripplanner.transit.model.site;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class PathwayNode extends StationElement<PathwayNode, PathwayNodeBuilder> {

  PathwayNode(PathwayNodeBuilder builder) {
    super(builder);
    // Verify coordinate is not null
    Objects.requireNonNull(getCoordinate());
  }

  public static PathwayNodeBuilder of(FeedScopedId id) {
    return new PathwayNodeBuilder(id);
  }

  @Override
  public PathwayNodeBuilder copy() {
    return new PathwayNodeBuilder(this);
  }

  @Override
  public boolean sameAs(PathwayNode other) {
    return super.sameAs(other);
  }
}
