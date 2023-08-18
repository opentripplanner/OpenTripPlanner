package org.opentripplanner.graph_builder.module.osm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * Contains the logic for extracting escalators out of OSM data
 */
class EscalatorProcessor {

  private final Map<Long, IntersectionVertex> intersectionNodes;

  public EscalatorProcessor(Map<Long, IntersectionVertex> intersectionNodes) {
    this.intersectionNodes = intersectionNodes;
  }

  public void buildEscalatorEdge(OSMWay escalatorWay, double length) {
    List<Long> nodes = Arrays
      .stream(escalatorWay.getNodeRefs().toArray())
      .filter(nodeRef ->
        intersectionNodes.containsKey(nodeRef) && intersectionNodes.get(nodeRef) != null
      )
      .boxed()
      .toList();

    for (int i = 0; i < nodes.size() - 1; i++) {
      if (escalatorWay.isForwardEscalator()) {
        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i)),
          intersectionNodes.get(nodes.get(i + 1)),
          length
        );
      } else if (escalatorWay.isBackwardEscalator()) {
        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i + 1)),
          intersectionNodes.get(nodes.get(i)),
          length
        );
      } else {
        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i)),
          intersectionNodes.get(nodes.get(i + 1)),
          length
        );

        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i + 1)),
          intersectionNodes.get(nodes.get(i)),
          length
        );
      }
    }
  }
}
