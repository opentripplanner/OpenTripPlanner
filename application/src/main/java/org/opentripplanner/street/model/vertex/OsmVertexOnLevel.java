package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * A vertex that represents an OSM node in conjunction with its level tag like both ends of an
 * elevator.
 * This is a separate class in order to conserve memory as only a tiny percentage of vertices
 * actually has level information.
 */
public class OsmVertexOnLevel extends OsmVertex {

  private final double level;

  public OsmVertexOnLevel(long id, WgsCoordinate coordinate, double level) {
    super(coordinate.longitude(), coordinate.latitude(), id);
    this.level = level;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.osm(nodeId, level);
  }
}
