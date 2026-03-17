package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.list.TLongList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.graph_builder.issues.AllWaysOfElevatorNodeOnSameLevel;
import org.opentripplanner.graph_builder.issues.CouldNotApplyMultiLevelInfoToElevatorWay;
import org.opentripplanner.graph_builder.issues.FewerThanTwoIntersectionNodesInElevatorWay;
import org.opentripplanner.graph_builder.issues.MoreThanTwoIntersectionNodesInElevatorWay;
import org.opentripplanner.graph_builder.issues.OnlyOneConnectionToElevatorNode;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmLevelFactory;
import org.opentripplanner.osm.model.OsmLevelSource;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmElevatorVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.streetadapter.VertexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the logic for extracting elevator data from OSM and converting it to edges.
 * <p>
 * It depends heavily on the idiosyncratic processing of the OSM data in {@link OsmModule}
 * which is the reason this is not a public class.
 * <p>
 * Elevators have three types of edges: ElevatorAlightEdges, ElevatorHopEdges, and
 * ElevatorBoardEdges. Elevators also have two types of vertices: the OsmElevatorVertex and the
 * ElevatorHopVertex.
 * <p>
 * For elevator nodes, the build process first generates OsmElevatorVertices during previous phases
 * of the graph build. These vertices serve as attachment points to the graph for elevators.
 * Elevator ways connect to the graph by using existing intersection vertices that are also part of
 * the elevator way.
 * <p>
 * The next step is to iterate over these attachment points and generate the ElevatorBoardEdges and
 * ElevatorAlightEdges. The other end for these edges is an ElevatorHopVertex. The
 * ElevatorBoardEdge allows boarding while the ElevatorAlightEdge allows alighting the elevator.
 * <p>
 * The last step is to connect all ElevatorHopVertices with ElevatorHopEdges. The amount of levels
 * between ElevatorHopVertices is stored in the edge. This incurs a cost dependent on the amount of
 * levels traveled. If the ElevatorHopVertices are on the same level (for example because of bad
 * data), the ElevatorHopEdge can have a cost of zero, but the board cost still applies in the
 * ElevatorBoardEdge.
 * <p>
 * With two connected ways to a node (which can be on the same level), after building the
 * ElevatorAlightEdge and ElevatorBoardEdge the graph will look like this (side view):
 *
 * +==X
 *
 * +==X
 *
 * +  ElevatorHopVertex
 * X  OsmElevatorVertex or IntersectionVertex
 * == ElevatorBoardEdge and ElevatorAlightEdge
 * <p>
 * Another loop fills in the ElevatorHopEdges. After filling in the ElevatorHopEdges when a node
 * has 3 connected ways the graph will look like this (side view):
 *
 * +==X
 * |
 * +==X
 * |
 * +==X
 *
 * +  ElevatorHopVertex
 * X  OsmElevatorVertex or IntersectionVertex
 * == ElevatorBoardEdge and ElevatorAlightEdge
 * |  ElevatorHopEdge
 */
class ElevatorProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorProcessor.class);

  private final OsmDatabase osmdb;
  private final VertexGenerator vertexGenerator;
  private final VertexFactory vertexFactory;
  private final Consumer<String> osmEntityDurationIssueConsumer;
  private final DataImportIssueStore issueStore;
  private final StreetDetailsRepository streetDetailsRepository;

  public ElevatorProcessor(
    DataImportIssueStore issueStore,
    OsmDatabase osmdb,
    VertexGenerator vertexGenerator,
    Graph graph,
    StreetDetailsRepository streetDetailsRepository
  ) {
    this.osmdb = osmdb;
    this.vertexGenerator = vertexGenerator;
    this.vertexFactory = new VertexFactory(graph);
    this.osmEntityDurationIssueConsumer = v ->
      issueStore.add(
        Issue.issue(
          "InvalidDuration",
          "Duration for osm node {} is not a valid duration: '{}'; the value is ignored.",
          v
        )
      );
    this.issueStore = issueStore;
    this.streetDetailsRepository = streetDetailsRepository;
  }

  /**
   * Needs to be called after relevant intersection vertices have been created.
   */
  public void buildElevatorEdges() {
    buildElevatorEdgesFromElevatorNodes();
    buildElevatorEdgesFromElevatorWays();
  }

  /**
   * Add nodes with tag highway=elevator to graph as elevators.
   * <p>
   * Needs to be called after elevatorNodes have been created in vertexGenerator.
   */
  private void buildElevatorEdgesFromElevatorNodes() {
    for (Long nodeId : vertexGenerator.elevatorNodes().keySet()) {
      OsmNode node = osmdb.getNode(nodeId);
      Map<OsmElevatorKey, OsmElevatorVertex> vertices = vertexGenerator.elevatorNodes().get(nodeId);
      Map<OsmElevatorKey, OsmLevel> verticeLevels = vertexGenerator.elevatorNodeLevels();

      if (vertices.size() < 2) {
        issueStore.add(new OnlyOneConnectionToElevatorNode(node));
        // Do not create unnecessary ElevatorBoardEdges, ElevatorAlightEdges, or ElevatorHopEdges.
        continue;
      }

      List<OsmElevatorKey> osmElevatorKeys = new ArrayList<>(vertices.keySet());
      if (
        osmElevatorKeys
          .stream()
          .map(key -> verticeLevels.get(key))
          .distinct()
          .count() ==
        1
      ) {
        issueStore.add(new AllWaysOfElevatorNodeOnSameLevel(node));
      }
      // Sort to make logic correct and create a deterministic order.
      osmElevatorKeys.sort(
        Comparator.comparing((OsmElevatorKey key) -> verticeLevels.get(key))
          .thenComparing(OsmElevatorKey::entityId)
          .thenComparing(OsmElevatorKey::osmEntityType)
      );
      List<ElevatorHopVertex> elevatorHopVertices = new ArrayList<>();
      for (OsmElevatorKey key : osmElevatorKeys) {
        OsmElevatorVertex sourceVertex = vertices.get(key);
        OsmLevel level = verticeLevels.get(key);
        createElevatorVertices(
          elevatorHopVertices,
          sourceVertex,
          sourceVertex.getLabelString(),
          level
        );
      }

      var wheelchair = node.explicitWheelchairAccessibility();
      long travelTime = node
        .getDuration(osmEntityDurationIssueConsumer)
        .map(Duration::toSeconds)
        .orElse(-1L);
      createElevatorHopEdges(
        elevatorHopVertices,
        osmElevatorKeys
          .stream()
          .map(key -> verticeLevels.get(key))
          .toList(),
        wheelchair,
        !node.isBicycleDenied(),
        (int) travelTime
      );
      LOG.debug("Created elevator edges for node {}", node.getId());
    }
  }

  /**
   * Add way with tag highway=elevator to graph as elevator.
   * <p>
   * Needs to be called after:
   * - intersection vertices have been created in vertexGenerator
   * - elevator ways have been collected
   */
  private void buildElevatorEdgesFromElevatorWays() {
    for (OsmWay way : osmdb.getWays()) {
      if (!isElevatorWay(way)) {
        continue;
      }
      List<OsmLevel> nodeLevels = osmdb.getLevelsForEntity(way);
      List<Long> nodes = Arrays.stream(way.getNodeRefs().toArray())
        .filter(nodeRef -> vertexGenerator.intersectionNodes().get(nodeRef) != null)
        .boxed()
        .toList();

      if (nodes.size() < 2) {
        var nodeRefs = way.getNodeRefs();
        long firstNodeRef = nodeRefs.get(0);
        long lastNodeRef = nodeRefs.get(nodeRefs.size() - 1);
        issueStore.add(
          new FewerThanTwoIntersectionNodesInElevatorWay(
            way,
            osmdb.getNode(firstNodeRef).getCoordinate(),
            osmdb.getNode(lastNodeRef).getCoordinate(),
            nodes.size()
          )
        );
        // Do not create unnecessary ElevatorBoardEdges, ElevatorAlightEdges, or ElevatorHopEdges.
        continue;
      } else if (nodes.size() > 2) {
        issueStore.add(
          new MoreThanTwoIntersectionNodesInElevatorWay(
            way,
            osmdb.getNode(nodes.getFirst()).getCoordinate(),
            osmdb.getNode(nodes.getLast()).getCoordinate(),
            nodes.size()
          )
        );
      }

      if (nodeLevels.size() != nodes.size()) {
        issueStore.add(
          new CouldNotApplyMultiLevelInfoToElevatorWay(
            way,
            osmdb.getNode(nodes.getFirst()).getCoordinate(),
            osmdb.getNode(nodes.getLast()).getCoordinate(),
            nodeLevels.size(),
            nodes.size()
          )
        );
        nodeLevels = Collections.nCopies(nodes.size(), OsmLevelFactory.DEFAULT);
      }

      List<ElevatorHopVertex> elevatorHopVertices = new ArrayList<>();
      for (int i = 0; i < nodes.size(); i++) {
        Long node = nodes.get(i);
        var sourceVertex = vertexGenerator.intersectionNodes().get(node);
        OsmLevel level = nodeLevels.get(i);
        createElevatorVertices(
          elevatorHopVertices,
          sourceVertex,
          way.getId() + "_" + i + "_" + sourceVertex.getLabelString(),
          level
        );
      }

      var wheelchair = way.explicitWheelchairAccessibility();
      long travelTime = way
        .getDuration(osmEntityDurationIssueConsumer)
        .map(Duration::toSeconds)
        .orElse(-1L);
      createElevatorHopEdges(
        elevatorHopVertices,
        nodeLevels,
        wheelchair,
        !way.isBicycleDenied(),
        (int) travelTime
      );
      LOG.debug("Created elevator edges for way {}", way.getId());
    }
  }

  private void createElevatorVertices(
    List<ElevatorHopVertex> elevatorHopVertices,
    IntersectionVertex sourceVertex,
    String label,
    OsmLevel level
  ) {
    ElevatorHopVertex elevatorHopVertex = vertexFactory.elevator(sourceVertex, label);

    ElevatorBoardEdge elevatorBoardEdge = ElevatorBoardEdge.createElevatorBoardEdge(
      sourceVertex,
      elevatorHopVertex
    );
    ElevatorAlightEdge elevatorAlightEdge = ElevatorAlightEdge.createElevatorAlightEdge(
      elevatorHopVertex,
      sourceVertex
    );

    if (level.source() != OsmLevelSource.DEFAULT) {
      Level repositoryLevel = new Level(level.level(), level.name());
      streetDetailsRepository.addHorizontalEdgeLevelInfo(elevatorBoardEdge, repositoryLevel);
      streetDetailsRepository.addHorizontalEdgeLevelInfo(elevatorAlightEdge, repositoryLevel);
    }

    // Accumulate ElevatorHopVertices so they can be connected by ElevatorHopEdges later.
    elevatorHopVertices.add(elevatorHopVertex);
  }

  private static void createElevatorHopEdges(
    List<ElevatorHopVertex> elevatorHopVertices,
    List<OsmLevel> elevatorHopVertexLevels,
    Accessibility wheelchair,
    boolean bicycleAllowed,
    int travelTime
  ) {
    // -1 because we loop over elevatorHopVertices two at a time
    for (int i = 0, vSize = elevatorHopVertices.size() - 1; i < vSize; i++) {
      Vertex from = elevatorHopVertices.get(i);
      Vertex to = elevatorHopVertices.get(i + 1);
      OsmLevel fromLevel = elevatorHopVertexLevels.get(i);
      OsmLevel toLevel = elevatorHopVertexLevels.get(i + 1);

      // default permissions: pedestrian, wheelchair, check tag bicycle=yes
      StreetTraversalPermission permission = bicycleAllowed
        ? StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
        : StreetTraversalPermission.PEDESTRIAN;

      ElevatorHopEdge.bidirectional(
        from,
        to,
        permission,
        wheelchair,
        Math.abs(toLevel.level() - fromLevel.level()),
        travelTime
      );
    }
  }

  public boolean isElevatorWay(OsmWay way) {
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
