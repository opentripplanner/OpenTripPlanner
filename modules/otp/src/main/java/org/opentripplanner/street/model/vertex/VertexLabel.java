package org.opentripplanner.street.model.vertex;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A label for a vertex so it can be identified when finding vertices in the graph.
 * <p>
 * This is not just a string in order to conserve memory because many vertices have IDs that
 * have the same prefix.
 */
public sealed interface VertexLabel {
  static VertexLabel string(String label) {
    return new StringLabel(label);
  }

  static VertexLabel osm(long nodeId, String level) {
    return new OsmNodeOnLevelLabel(nodeId, level);
  }

  static VertexLabel osm(long nodeId) {
    return new OsmNodeLabel(nodeId);
  }

  static VertexLabel feedScopedId(FeedScopedId id) {
    return new FeedScopedIdLabel(id);
  }

  /**
   * A vertex label that is based on a string.
   */
  record StringLabel(String value) implements VertexLabel {
    @Override
    public String toString() {
      return value;
    }
  }

  /**
   * A vertex label for an OSM node id.
   */
  record OsmNodeLabel(long nodeId) implements VertexLabel {
    private static final String TEMPLATE = "osm:node:%s";

    @Override
    public String toString() {
      return TEMPLATE.formatted(nodeId);
    }
  }

  /**
   * A vertex label for an OSM node that also has a level, for example the upper and lower
   * vertices of an elevator edge.
   */
  record OsmNodeOnLevelLabel(long nodeId, String level) implements VertexLabel {
    private static final String TEMPLATE = "osm:node:%s/%s";

    @Override
    public String toString() {
      return TEMPLATE.formatted(nodeId, level);
    }
  }

  /**
   * A vertex label that represents a feed-scoped id, like a transit stop.
   */
  record FeedScopedIdLabel(FeedScopedId id) implements VertexLabel {
    @Override
    public String toString() {
      return id.toString();
    }
  }
}
