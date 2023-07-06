package org.opentripplanner.street.model.vertex;

import org.opentripplanner.openstreetmap.model.OSMNode;

/**
 * A vertex that represents an OSM node in conjunction with its level tag like both ends of an
 * elevator.
 * This is a separate class in order to conserve memory as only a tiny percentage of vertices
 * actually has level information.
 */
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
