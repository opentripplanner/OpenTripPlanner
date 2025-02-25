package org.opentripplanner.graph_builder.module.osm.moduletests._support;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.osm.model.OsmNode;

public class NodeBuilder {

  public static OsmNode node(long id, WgsCoordinate wgsCoordinate) {
    var node = new OsmNode(wgsCoordinate.latitude(), wgsCoordinate.longitude());
    node.setId(id);
    return node;
  }
}
