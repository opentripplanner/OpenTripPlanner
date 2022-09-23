package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class PathwayNodeBuilder
  extends StationElementBuilder<PathwayNode, PathwayNodeBuilder> {

  PathwayNodeBuilder(FeedScopedId id) {
    super(id);
  }

  PathwayNodeBuilder(PathwayNode original) {
    super(original);
  }

  @Override
  PathwayNodeBuilder instance() {
    return this;
  }

  @Override
  protected PathwayNode buildFromValues() {
    return new PathwayNode(this);
  }
}
