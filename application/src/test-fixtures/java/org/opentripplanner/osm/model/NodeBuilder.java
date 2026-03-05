package org.opentripplanner.osm.model;

import org.opentripplanner.street.geometry.WgsCoordinate;

public class NodeBuilder {

  public static OsmNode node(long id, WgsCoordinate wgsCoordinate) {
    var node = new OsmNode(wgsCoordinate.latitude(), wgsCoordinate.longitude());
    node.setId(id);
    return node;
  }
}
