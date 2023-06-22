package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the logic for extracting escalators out of OSM data
 */
class EscalatorProcessor {

  private final DataImportIssueStore issueStore;

  private final OsmDatabase osmdb;

  private final Map<Long, IntersectionVertex> intersectionNodes;

  public EscalatorProcessor(
    DataImportIssueStore issueStore,
    OsmDatabase osmdb,
    Map<Long, IntersectionVertex> intersectionNodes
  ) {
    this.issueStore = issueStore;
    this.osmdb = osmdb;
    this.intersectionNodes = intersectionNodes;
  }

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorProcessor.class);

  public void buildEscalatorEdge(OSMWay escalatorWay, double length) {
    ArrayList<EscalatorEdge> edges = new ArrayList<>();
    List<Long> nodes = Arrays
      .stream(escalatorWay.getNodeRefs().toArray())
      .filter(nodeRef ->
        intersectionNodes.containsKey(nodeRef) && intersectionNodes.get(nodeRef) != null
      )
      .boxed()
      .toList();

    for (int i = 0; i < nodes.size() - 1; i++) {
      if (escalatorWay.isForwardEscalator()) {
        edges.add(
          new EscalatorEdge(
            intersectionNodes.get(nodes.get(i)),
            intersectionNodes.get(nodes.get(i + 1)),
            length
          )
        );
      } else if (escalatorWay.isBackwardEscalator()) {
        edges.add(
          new EscalatorEdge(
            intersectionNodes.get(nodes.get(i + 1)),
            intersectionNodes.get(nodes.get(i)),
            length
          )
        );
      } else {
        edges.add(
          new EscalatorEdge(
            intersectionNodes.get(nodes.get(i)),
            intersectionNodes.get(nodes.get(i + 1)),
            length
          )
        );
        edges.add(
          new EscalatorEdge(
            intersectionNodes.get(nodes.get(i + 1)),
            intersectionNodes.get(nodes.get(i)),
            length
          )
        );
      }
    }
  }
}
