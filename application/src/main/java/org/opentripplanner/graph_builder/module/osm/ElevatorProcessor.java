package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.list.TLongList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.ElevatorVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the logic for extracting elevator data from OSM and converting it to edges.
 * <p>
 * It depends heavily on the idiosyncratic processing of the OSM data in {@link OsmModule}
 * which is the reason this is not a public class.
 */
class ElevatorProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorProcessor.class);

  private final OsmDatabase osmdb;
  private final VertexGenerator vertexGenerator;
  private final Consumer<String> osmEntityDurationIssueConsumer;

  public ElevatorProcessor(
    DataImportIssueStore issueStore,
    OsmDatabase osmdb,
    VertexGenerator vertexGenerator
  ) {
    this.osmdb = osmdb;
    this.vertexGenerator = vertexGenerator;
    this.osmEntityDurationIssueConsumer = v ->
      issueStore.add(
        Issue.issue(
          "InvalidDuration",
          "Duration for osm node {} is not a valid duration: '{}'; the value is ignored.",
          v
        )
      );
  }

  public void buildElevatorEdges(Graph graph) {
    /* build elevator edges */
    for (Long nodeId : vertexGenerator.multiLevelNodes().keySet()) {
      OsmNode node = osmdb.getNode(nodeId);
      // this allows skipping levels, e.g., an elevator that stops
      // at floor 0, 2, 3, and 5.
      // Converting to an Array allows us to
      // subscript it so we can loop over it in twos. Assumedly, it will stay
      // sorted when we convert it to an Array.
      // The objects are Integers, but toArray returns Object[]
      Map<OsmLevel, OsmVertex> vertices = vertexGenerator.multiLevelNodes().get(nodeId);

      /*
       * first, build FreeEdges to disconnect from the graph, GenericVertices to serve as attachment points, and ElevatorBoard and
       * ElevatorAlight edges to connect future ElevatorHop edges to. After this iteration, graph will look like (side view): +==+~~X
       *
       * +==+~~X
       *
       * +==+~~X
       *
       * + GenericVertex, X EndpointVertex, ~~ FreeEdge, == ElevatorBoardEdge/ElevatorAlightEdge Another loop will fill in the
       * ElevatorHopEdges.
       */
      OsmLevel[] levels = vertices.keySet().toArray(new OsmLevel[0]);
      Arrays.sort(levels);
      ArrayList<Vertex> onboardVertices = new ArrayList<>();
      for (OsmLevel level : levels) {
        // get the node to build the elevator out from
        OsmVertex sourceVertex = vertices.get(level);
        String levelName = level.longName;

        createElevatorVertices(
          graph,
          onboardVertices,
          sourceVertex,
          sourceVertex.getLabelString(),
          levelName
        );
      }
      long travelTime = node
        .getDuration(osmEntityDurationIssueConsumer)
        .map(Duration::toSeconds)
        .orElse(-1L);

      var wheelchair = node.wheelchairAccessibility();

      createElevatorHopEdges(
        onboardVertices,
        wheelchair,
        !node.isBicycleDenied(),
        levels.length,
        (int) travelTime
      );
    } // END elevator edge loop

    // Add highway=elevators to graph as elevators
    Iterator<OsmWay> elevators = osmdb.getWays().stream().filter(this::isElevatorWay).iterator();

    while (elevators.hasNext()) {
      OsmWay elevatorWay = elevators.next();

      List<Long> nodes = Arrays.stream(elevatorWay.getNodeRefs().toArray())
        .filter(
          nodeRef ->
            vertexGenerator.intersectionNodes().containsKey(nodeRef) &&
            vertexGenerator.intersectionNodes().get(nodeRef) != null
        )
        .boxed()
        .toList();

      ArrayList<Vertex> onboardVertices = new ArrayList<>();
      for (int i = 0; i < nodes.size(); i++) {
        Long node = nodes.get(i);
        var sourceVertex = vertexGenerator.intersectionNodes().get(node);
        String sourceVertexLabel = sourceVertex.getLabelString();
        String levelName = elevatorWay.getId() + " / " + i;
        createElevatorVertices(
          graph,
          onboardVertices,
          sourceVertex,
          elevatorWay.getId() + "_" + sourceVertexLabel,
          levelName
        );
      }

      long travelTime = elevatorWay
        .getDuration(osmEntityDurationIssueConsumer)
        .map(Duration::toSeconds)
        .orElse(-1L);
      int levels = nodes.size();
      var wheelchair = elevatorWay.wheelchairAccessibility();

      createElevatorHopEdges(
        onboardVertices,
        wheelchair,
        !elevatorWay.isBicycleDenied(),
        levels,
        (int) travelTime
      );
      LOG.debug("Created elevatorHopEdges for way {}", elevatorWay.getId());
    }
  }

  private static void createElevatorVertices(
    Graph graph,
    ArrayList<Vertex> onboardVertices,
    IntersectionVertex sourceVertex,
    String label,
    String levelName
  ) {
    var factory = new VertexFactory(graph);
    ElevatorVertex onboardVertex = factory.elevator(sourceVertex, label, levelName);

    ElevatorBoardEdge.createElevatorBoardEdge(sourceVertex, onboardVertex);
    ElevatorAlightEdge.createElevatorAlightEdge(
      onboardVertex,
      sourceVertex,
      new NonLocalizedString(levelName)
    );

    // accumulate onboard vertices to so they can be connected by hop edges later
    onboardVertices.add(onboardVertex);
  }

  private static void createElevatorHopEdges(
    ArrayList<Vertex> onboardVertices,
    Accessibility wheelchair,
    boolean bicycleAllowed,
    int levels,
    int travelTime
  ) {
    // -1 because we loop over onboardVertices two at a time
    for (int i = 0, vSize = onboardVertices.size() - 1; i < vSize; i++) {
      Vertex from = onboardVertices.get(i);
      Vertex to = onboardVertices.get(i + 1);

      // default permissions: pedestrian, wheelchair, check tag bicycle=yes
      StreetTraversalPermission permission = bicycleAllowed
        ? StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
        : StreetTraversalPermission.PEDESTRIAN;

      if (travelTime > -1 && levels > 0) {
        ElevatorHopEdge.bidirectional(from, to, permission, wheelchair, levels, travelTime);
      } else {
        ElevatorHopEdge.bidirectional(from, to, permission, wheelchair);
      }
    }
  }

  private boolean isElevatorWay(OsmWay way) {
    if (!way.isElevator()) {
      return false;
    }

    if (osmdb.isAreaWay(way.getId())) {
      return false;
    }

    TLongList nodeRefs = way.getNodeRefs();
    // A way whose first and last node are the same is probably an area, skip that.
    // https://www.openstreetmap.org/way/503412863
    // https://www.openstreetmap.org/way/187719215
    return nodeRefs.get(0) != nodeRefs.get(nodeRefs.size() - 1);
  }
}
