package org.opentripplanner.transit.model.site;

import java.util.Objects;
import javax.annotation.Nonnull;
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
  @Nonnull
  public PathwayNodeBuilder copy() {
    return new PathwayNodeBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull PathwayNode other) {
    return super.sameAs(other);
  }
}
