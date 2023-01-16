package org.opentripplanner.graph_builder.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.GraphConnectivity;
import org.opentripplanner.graph_builder.issues.GraphIsland;
import org.opentripplanner.graph_builder.issues.IsolatedStop;
import org.opentripplanner.graph_builder.issues.PrunedStopIsland;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntityLink;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this module is part of the  {@link GraphBuilderModule}
 * process. It extends the functionality of PruneFloatingIslands by considering also through traffic
 * limitations. It is quite common that no thru edges break connectivity of the graph, creating
 * islands. The quality of the graph can be improved by converting such islands to nothru state so
 * that routing can start / end from such an island.
 */
public class PruneIslands implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(PruneIslands.class);

  private final int islandCounter = 0;
  private final Graph graph;
  private final TransitModel transitModel;
  private final DataImportIssueStore issueStore;
  private final StreetLinkerModule streetLinkerModule;
  /**
   * this field indicate the maximum size for island without stops island under this size will be
   * pruned.
   */
  private int pruningThresholdIslandWithoutStops;
  /**
   * this field indicate the maximum size for island with stops island under this size will be
   * pruned.
   */
  private int pruningThresholdIslandWithStops;

  /**
   * search radius as meters when looking for island neighbours
   */
  private double adaptivePruningDistance;

  /**
   * Coefficient how much larger islands (compared to max sizes defined above) get pruned if they are close enough
   */
  private double adaptivePruningFactor;

  public PruneIslands(
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore,
    StreetLinkerModule streetLinkerModule
  ) {
    this.graph = graph;
    this.transitModel = transitModel;
    this.issueStore = issueStore;
    this.streetLinkerModule = streetLinkerModule;
  }

  @Override
  public void buildGraph() {
    LOG.info("Pruning islands and areas isolated by nothru edges in street network");
    LOG.info(
      "Threshold with stops {} without stops {}, adaptive coeff {} and distance {}",
      pruningThresholdIslandWithStops,
      pruningThresholdIslandWithoutStops,
      adaptivePruningFactor,
      adaptivePruningDistance
    );

    var vertexLinker = graph.getLinkerSafe(transitModel.getStopModel());
    StreetIndex streetIndex = graph.getStreetIndexSafe(transitModel.getStopModel());

    pruneIslands(
      graph,
      vertexLinker,
      streetIndex,
      pruningThresholdIslandWithoutStops,
      pruningThresholdIslandWithStops,
      issueStore,
      TraverseMode.BICYCLE
    );
    pruneIslands(
      graph,
      vertexLinker,
      streetIndex,
      pruningThresholdIslandWithoutStops,
      pruningThresholdIslandWithStops,
      issueStore,
      TraverseMode.WALK
    );
    pruneIslands(
      graph,
      vertexLinker,
      streetIndex,
      pruningThresholdIslandWithoutStops,
      pruningThresholdIslandWithStops,
      issueStore,
      TraverseMode.CAR
    );
    // reconnect stops that got disconnected
    if (streetLinkerModule != null) {
      LOG.info("Reconnecting stops");
      streetLinkerModule.linkTransitStops(graph, transitModel);
      int isolated = 0;
      for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
        if (tStop.getDegreeOut() + tStop.getDegreeIn() == 0) {
          issueStore.add(new IsolatedStop(tStop));
          isolated++;
        }
      }
      LOG.info("{} stops remain isolated", isolated);
    }

    // clean up pruned street vertices
    int removed = 0;
    List<Vertex> toRemove = new LinkedList<>();
    for (Vertex v : graph.getVerticesOfType(StreetVertex.class)) {
      if (v.getDegreeOut() + v.getDegreeIn() == 0) toRemove.add(v);
    }
    for (Vertex v : toRemove) {
      graph.remove(v);
      removed += 1;
    }
    LOG.info("Removed {} edgeless street vertices", removed);
  }

  @Override
  public void checkInputs() {
    //no inputs
  }

  public void setPruningThresholdIslandWithoutStops(int pruningThresholdIslandWithoutStops) {
    this.pruningThresholdIslandWithoutStops = pruningThresholdIslandWithoutStops;
  }

  public void setPruningThresholdIslandWithStops(int pruningThresholdIslandWithStops) {
    this.pruningThresholdIslandWithStops = pruningThresholdIslandWithStops;
  }

  public void setAdaptivePruningDistance(double adaptivePruningDistance) {
    this.adaptivePruningDistance = adaptivePruningDistance;
  }

  public void setAdaptivePruningFactor(double adaptivePruningFactor) {
    this.adaptivePruningFactor = adaptivePruningFactor;
  }

  /* Island pruning strategy:
       1. Extract islands without using noThruTraffic edges at all
       2. Then create expanded islands by accepting noThruTraffic edges, but do not jump across original islands!
          Note: these expanded islands can overlap.
       3  Relax connectivity even more: generate islands by allowing jumps between islands. Find out unreachable edges of small islands.
       4. Analyze small expanded islands (from step 2). Convert edges which are reachable only via noThruTraffic edges
          to noThruTraffic state. Remove traversal mode specific access from unreachable edges. Remove unconnected edges.
     */

  private void pruneIslands(
    Graph graph,
    VertexLinker vertexLinker,
    StreetIndex streetIndex,
    int maxIslandSize,
    int islandWithStopMaxSize,
    DataImportIssueStore issueStore,
    TraverseMode traverseMode
  ) {
    LOG.debug("nothru pruning");
    Map<Vertex, Subgraph> subgraphs = new HashMap<>();
    Map<Vertex, Subgraph> extgraphs = new HashMap<>();
    Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<>();
    Map<Edge, Boolean> isolated = new HashMap<>();
    ArrayList<Subgraph> islands = new ArrayList<>();
    int count;

    /* establish vertex neighbourhood without currently relevant noThruTrafficEdges */
    collectNeighbourVertices(graph, neighborsForVertex, traverseMode, false);

    /* associate each connected vertex with a subgraph */
    count = collectSubGraphs(graph, neighborsForVertex, subgraphs, null, null);
    LOG.info("Islands without {} noThruTraffic edges: {}", traverseMode, count);

    /* Expand vertex neighbourhood with relevant noThruTrafficEdges
           Note that we can reuse the original neighbour map here
           and simply process a smaller set of noThruTrafficEdges */
    collectNeighbourVertices(graph, neighborsForVertex, traverseMode, true);

    /* Next: generate subgraphs without considering access limitations */
    count = collectSubGraphs(graph, neighborsForVertex, extgraphs, null, islands);
    LOG.info("Islands with {} noThruTraffic edges: {}", traverseMode, count);

    /* collect unreachable edges to a map */
    processIslands(
      graph,
      vertexLinker,
      streetIndex,
      islands,
      isolated,
      true,
      maxIslandSize,
      islandWithStopMaxSize,
      issueStore,
      traverseMode
    );

    extgraphs = new HashMap<>(); // let old map go
    islands = new ArrayList<>(); // reset this too

    /* Recompute expanded subgraphs by accepting noThruTraffic edges in graph expansion.
       However, expansion is not allowed to jump from an original island to another one
     */
    collectSubGraphs(graph, neighborsForVertex, extgraphs, subgraphs, islands);

    /* Next round: generate purely noThruTraffic islands if such ones exist */
    count = collectSubGraphs(graph, neighborsForVertex, extgraphs, null, islands);
    LOG.info("{} noThruTraffic island count: {}", traverseMode, count);

    LOG.info("Total {} sub graphs found", islands.size());

    count =
      processIslands(
        graph,
        vertexLinker,
        streetIndex,
        islands,
        isolated,
        false,
        maxIslandSize,
        islandWithStopMaxSize,
        issueStore,
        traverseMode
      );
    LOG.info("Modified {} islands", count);
  }

  private int processIslands(
    Graph graph,
    VertexLinker vertexLinker,
    StreetIndex streetIndex,
    ArrayList<Subgraph> islands,
    Map<Edge, Boolean> isolated,
    boolean markIsolated,
    int maxIslandSize,
    int islandWithStopMaxSize,
    DataImportIssueStore issueStore,
    TraverseMode traverseMode
  ) {
    Map<String, Integer> stats = new HashMap<>();

    stats.put("isolated", 0);
    stats.put("removed", 0);
    stats.put("noThru", 0);
    stats.put("restricted", 0);

    Subgraph largest = null;
    int maxSize = 0;

    // Find largest sub graph
    for (Subgraph island : islands) {
      int streetCount = island.streetSize();
      if (streetCount >= maxSize) {
        maxSize = streetCount;
        largest = island;
      }
    }

    int count = 0;
    int islandsWithStops = 0;
    int islandsWithStopsChanged = 0;
    for (Subgraph island : islands) {
      if (island == largest) {
        continue;
      }
      boolean changed = false;
      if (island.stopSize() > 0) {
        //for islands with stops
        islandsWithStops++;
        boolean onlyFerry = true;
        for (Iterator<Vertex> vIter = island.stopIterator(); vIter.hasNext();) {
          TransitStopVertex v = (TransitStopVertex) vIter.next();
          Set<TransitMode> modes = v.getModes();
          // test if stop has other transit modes than FERRY
          if (!modes.isEmpty() && !modes.contains(TransitMode.FERRY)) {
            onlyFerry = false;
            break;
          }
        }
        // do not remove real islands which have only ferry stops
        if (!onlyFerry && island.streetSize() < islandWithStopMaxSize * adaptivePruningFactor) {
          double sizeCoeff = (adaptivePruningFactor > 1.0)
            ? island.distanceFromOtherGraph(streetIndex, adaptivePruningDistance) /
            adaptivePruningDistance
            : 1.0;

          if (island.streetSize() * sizeCoeff < islandWithStopMaxSize) {
            restrictOrRemove(
              graph,
              vertexLinker,
              island,
              isolated,
              stats,
              markIsolated,
              traverseMode,
              issueStore
            );
            changed = true;
            islandsWithStopsChanged++;
            count++;
          }
        }
      } else {
        //for islands without stops
        if (island.streetSize() < maxIslandSize * adaptivePruningFactor) {
          double sizeCoeff = (adaptivePruningFactor > 1.0)
            ? island.distanceFromOtherGraph(streetIndex, adaptivePruningDistance) /
            adaptivePruningDistance
            : 1.0;
          if (island.streetSize() * sizeCoeff < maxIslandSize) {
            restrictOrRemove(
              graph,
              vertexLinker,
              island,
              isolated,
              stats,
              markIsolated,
              traverseMode,
              issueStore
            );
            changed = true;
            count++;
          }
        }
      }
    }
    if (markIsolated) {
      LOG.info("Detected {} isolated edges", stats.get("isolated"));
    } else {
      LOG.info("Number of islands with stops: {}", islandsWithStops);
      LOG.warn("Modified connectivity of {} islands with stops", islandsWithStopsChanged);
      LOG.info("Removed {} edges", stats.get("removed"));
      LOG.info("Removed traversal mode from {} edges", stats.get("restricted"));
      LOG.info("Converted {} edges to noThruTraffic", stats.get("noThru"));
      issueStore.add(
        new GraphConnectivity(
          traverseMode,
          islands.size(),
          islandsWithStops,
          islandsWithStopsChanged,
          stats.get("removed"),
          stats.get("restricted"),
          stats.get("noThru")
        )
      );
    }
    return count;
  }

  private void collectNeighbourVertices(
    Graph graph,
    Map<Vertex, ArrayList<Vertex>> neighborsForVertex,
    TraverseMode traverseMode,
    boolean shouldMatchNoThruType
  ) {
    StreetMode streetMode =
      switch (traverseMode) {
        case WALK -> StreetMode.WALK;
        case BICYCLE -> StreetMode.BIKE;
        case CAR -> StreetMode.CAR;
        default -> throw new IllegalArgumentException();
      };

    StreetSearchRequest request = StreetSearchRequest.of().withMode(streetMode).build();

    for (Vertex gv : graph.getVertices()) {
      if (!(gv instanceof StreetVertex)) {
        continue;
      }
      State s0 = new State(gv, request);
      for (Edge e : gv.getOutgoing()) {
        if (
          !(
            e instanceof StreetEdge ||
            e instanceof StreetTransitStopLink ||
            e instanceof StreetTransitEntranceLink ||
            e instanceof ElevatorEdge ||
            e instanceof FreeEdge ||
            e instanceof StreetTransitEntityLink
          )
        ) {
          continue;
        }
        if (
          e instanceof StreetEdge &&
          shouldMatchNoThruType != ((StreetEdge) e).isNoThruTraffic(traverseMode)
        ) {
          continue;
        }
        State s1 = e.traverse(s0);
        if (s1 == null) {
          continue;
        }
        Vertex out = s1.getVertex();

        ArrayList<Vertex> vertexList = neighborsForVertex.get(gv);
        if (vertexList == null) {
          vertexList = new ArrayList<>();
          neighborsForVertex.put(gv, vertexList);
        }
        vertexList.add(out);

        vertexList = neighborsForVertex.get(out);
        if (vertexList == null) {
          vertexList = new ArrayList<>();
          neighborsForVertex.put(out, vertexList);
        }
        vertexList.add(gv);
      }
    }
  }

  private int collectSubGraphs(
    Graph graph,
    Map<Vertex, ArrayList<Vertex>> neighborsForVertex,
    Map<Vertex, Subgraph> newgraphs, // put new subgraphs here
    Map<Vertex, Subgraph> subgraphs, // optional isolation map from a previous round
    ArrayList<Subgraph> islands
  ) { // final list of islands or null
    int count = 0;
    for (Vertex gv : graph.getVertices()) {
      if (!(gv instanceof StreetVertex)) {
        continue;
      }

      if (subgraphs != null && !subgraphs.containsKey(gv)) {
        // do not start new graph generation from non-classified vertex
        continue;
      }
      if (newgraphs.containsKey(gv)) { // already processed
        continue;
      }
      if (!neighborsForVertex.containsKey(gv)) {
        continue;
      }
      Subgraph subgraph = computeConnectedSubgraph(neighborsForVertex, gv, subgraphs);
      if (subgraph != null) {
        for (Iterator<Vertex> vIter = subgraph.streetIterator(); vIter.hasNext();) {
          Vertex subnode = vIter.next();
          newgraphs.put(subnode, subgraph);
        }
        if (islands != null) {
          islands.add(subgraph);
        }
        count++;
      }
    }
    return count;
  }

  private void restrictOrRemove(
    Graph graph,
    VertexLinker vertexLinker,
    Subgraph island,
    Map<Edge, Boolean> isolated,
    Map<String, Integer> stats,
    boolean markIsolated,
    TraverseMode traverseMode,
    DataImportIssueStore issueStore
  ) {
    int nothru = 0, removed = 0, restricted = 0;
    //iterate over the street vertex of the subgraph
    for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
      Vertex v = vIter.next();
      Collection<Edge> outgoing = new ArrayList<>(v.getOutgoing());
      for (Edge e : outgoing) {
        if (e instanceof StreetEdge) {
          if (markIsolated) {
            isolated.put(e, true);
            stats.put("isolated", stats.get("isolated") + 1);
          } else {
            StreetEdge pse = (StreetEdge) e;
            if (!isolated.containsKey(e)) {
              // not a true island edge but has limited access
              // so convert to noThruTraffic
              if (traverseMode == TraverseMode.CAR) {
                pse.setMotorVehicleNoThruTraffic(true);
              } else if (traverseMode == TraverseMode.BICYCLE) {
                pse.setBicycleNoThruTraffic(true);
              } else if (traverseMode == TraverseMode.WALK) {
                pse.setWalkNoThruTraffic(true);
              }
              stats.put("noThru", stats.get("noThru") + 1);
              nothru++;
            } else {
              StreetTraversalPermission permission = pse.getPermission();
              if (traverseMode == TraverseMode.CAR) {
                permission = permission.remove(StreetTraversalPermission.CAR);
              } else if (traverseMode == TraverseMode.BICYCLE) {
                permission = permission.remove(StreetTraversalPermission.BICYCLE);
              } else if (traverseMode == TraverseMode.WALK) {
                permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
              }
              if (permission == StreetTraversalPermission.NONE) {
                // currently we must update spatial index manually, graph.removeEdge does not do that
                vertexLinker.removePermanentEdgeFromIndex(pse);
                graph.removeEdge(pse);
                stats.put("removed", stats.get("removed") + 1);
                removed++;
              } else {
                pse.setPermission(permission);
                stats.put("restricted", stats.get("restricted") + 1);
                restricted++;
              }
            }
          }
        }
      }
    }
    if (markIsolated) {
      return;
    }

    for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
      Vertex v = vIter.next();
      if (v.getDegreeOut() + v.getDegreeIn() == 0) {
        graph.remove(v);
      }
    }

    if (traverseMode == TraverseMode.WALK) {
      // note: do not unlink stop if only CAR mode is pruned
      // maybe this needs more logic for flex routing cases
      List<String> stopLabels = new ArrayList<String>();
      for (Iterator<Vertex> vIter = island.stopIterator(); vIter.hasNext();) {
        Vertex v = vIter.next();
        stopLabels.add(v.getLabel());
        Collection<Edge> edges = new ArrayList<>(v.getOutgoing());
        edges.addAll(v.getIncoming());
        for (Edge e : edges) {
          if (e instanceof StreetTransitStopLink || e instanceof StreetTransitEntranceLink) {
            graph.removeEdge(e);
          }
        }
      }
      if (island.stopSize() > 0) {
        // issue about stops that got unlinked in pruning
        issueStore.add(
          new PrunedStopIsland(
            island.getRepresentativeVertex(),
            island.streetSize(),
            island.stopSize(),
            nothru,
            restricted,
            removed,
            String.join(", ", stopLabels)
          )
        );
      }
    }
    issueStore.add(
      new GraphIsland(
        island.getRepresentativeVertex(),
        island.streetSize(),
        island.stopSize(),
        nothru,
        restricted,
        removed,
        traverseMode.name()
      )
    );
  }

  private Subgraph computeConnectedSubgraph(
    Map<Vertex, ArrayList<Vertex>> neighborsForVertex,
    Vertex startVertex,
    Map<Vertex, Subgraph> anchors
  ) {
    Subgraph subgraph = new Subgraph();
    Queue<Vertex> q = new LinkedList<>();
    Subgraph anchor = null;

    if (anchors != null) {
      // anchor subgraph expansion to this subgraph
      anchor = anchors.get(startVertex);
    }
    q.add(startVertex);
    while (!q.isEmpty()) {
      Vertex vertex = q.poll();
      for (Vertex neighbor : neighborsForVertex.get(vertex)) {
        if (!subgraph.contains(neighbor)) {
          if (anchor != null) {
            Subgraph compare = anchors.get(neighbor);
            if (compare != null && compare != anchor) { // do not enter a new island
              continue;
            }
          }
          subgraph.addVertex(neighbor);
          q.add(neighbor);
        }
      }
    }
    return subgraph;
  }
}
