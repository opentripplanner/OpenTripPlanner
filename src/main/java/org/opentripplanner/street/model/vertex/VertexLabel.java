package org.opentripplanner.street.model.vertex;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public sealed interface VertexLabel {
  static OsmNodeLabel osm(long nodeId) {
    return new OsmNodeLabel(nodeId);
  }

  static VertexLabel string(String label) {
    return new StringLabel(label);
  }

  static VertexLabel osm(long nodeId, String level) {
    return new LevelledOsmNodeLabel(nodeId, level);
  }

  static void elevator(StationElementVertex fromVertex, FeedScopedId id) {}

  record StringLabel(String value) implements VertexLabel {
    @Override
    public String toString() {
      return value;
    }
  }

  record OsmNodeLabel(long nodeId) implements VertexLabel {
    private static final String TEMPLATE = "osm:node:%s";

    @Override
    public String toString() {
      return TEMPLATE.formatted(nodeId);
    }
  }

  record LevelledOsmNodeLabel(long nodeId, String level) implements VertexLabel {
    private static final String TEMPLATE = "osm:node:%s/%s";

    @Override
    public String toString() {
      return TEMPLATE.formatted(nodeId, level);
    }
  }
}
