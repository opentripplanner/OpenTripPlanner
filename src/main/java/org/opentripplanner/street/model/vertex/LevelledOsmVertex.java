package org.opentripplanner.street.model.vertex;

import org.opentripplanner.openstreetmap.model.OSMNode;

public class LevelledOsmVertex extends OsmVertex {
  private final String level;

  public LevelledOsmVertex(OSMNode node, String level) {
    super(node.getCoordinate().x, node.getCoordinate().y, node.getId());
    this.level = level;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.osm(nodeId, level);
  }
}
