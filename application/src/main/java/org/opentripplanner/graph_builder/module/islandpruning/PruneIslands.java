package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.GraphConnectivity;
import org.opentripplanner.graph_builder.issues.IsolatedStop;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.service.TimetableRepository;
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

  private final Graph graph;
  private final TimetableRepository timetableRepository;
  private final DataImportIssueStore issueStore;
  private final StreetLinkerModule streetLinkerModule;
  private int pruningThresholdWithoutStops;
  private int pruningThresholdWithStops;
  private int adaptivePruningDistance;
  private double adaptivePruningFactor;
  private VertexLinker vertexLinker;
  private StreetIndex streetIndex;

  public PruneIslands(
    Graph graph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    StreetLinkerModule streetLinkerModule
  ) {
    this.graph = graph;
    this.timetableRepository = timetableRepository;
    this.issueStore = issueStore;
    this.streetLinkerModule = streetLinkerModule;
  }

  @Override
  public void buildGraph() {
    LOG.info("Pruning islands and areas isolated by nothru edges in street network");
    LOG.info(
      "Threshold with stops {}, without stops {}, adaptive coeff {} and distance {}",
      pruningThresholdWithStops,
      pruningThresholdWithoutStops,
      adaptivePruningFactor,
      adaptivePruningDistance
    );

    this.vertexLinker = graph.getLinkerSafe(timetableRepository.getSiteRepository());
    this.streetIndex = graph.getStreetIndexSafe(timetableRepository.getSiteRepository());

    pruneIslands(TraverseMode.BICYCLE);
    pruneIslands(TraverseMode.WALK);
    pruneIslands(TraverseMode.CAR);

    // reconnect stops that got disconnected
    if (streetLinkerModule != null) {
      LOG.info("Reconnecting stops");
      streetLinkerModule.linkTransitStops(graph, timetableRepository);
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
    // note that visibility vertices must not be removed from the graph
    // because serialization will break. Edge lists are reconstructed
    // only for graph vertices after loading the graph
    List<AreaEdge> areaEdges = graph.getEdgesOfType(AreaEdge.class);
    HashSet<AreaGroup> areas = new HashSet<>();
    HashSet<Vertex> visibilityVertices = new HashSet<>();

    for (AreaEdge ae : areaEdges) {
      areas.add(ae.getArea());
    }
    for (AreaGroup a : areas) {
      visibilityVertices.addAll(a.visibilityVertices());
    }

    int removed = 0;
    List<Vertex> toRemove = new LinkedList<>();
    for (Vertex v : graph.getVerticesOfType(StreetVertex.class)) {
      if (v.getDegreeOut() + v.getDegreeIn() == 0 && !visibilityVertices.contains(v)) {
        toRemove.add(v);
      }
    }
    for (Vertex v : toRemove) {
      graph.remove(v);
      removed += 1;
    }
    LOG.info("Removed {} edgeless street vertices", removed);
  }

  /**
   * island without stops and with less than this number of street vertices will be pruned
   */
  public void setPruningThresholdIslandWithoutStops(int pruningThresholdIslandWithoutStops) {
    this.pruningThresholdWithoutStops = pruningThresholdIslandWithoutStops;
  }

  /**
   * island with stops and with less than this number of street vertices will be pruned
   */
  public void setPruningThresholdIslandWithStops(int pruningThresholdIslandWithStops) {
    this.pruningThresholdWithStops = pruningThresholdIslandWithStops;
  }

  /**
   * search radius as meters when looking for island neighbours
   */
  public void setAdaptivePruningDistance(int adaptivePruningDistance) {
    this.adaptivePruningDistance = adaptivePruningDistance;
  }

  /**
   * coefficient how much larger islands (compared to threshold values defined above) get pruned if they are close enough
   */
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

  private void pruneIslands(TraverseMode traverseMode) {
    LOG.debug("nothru pruning");
    Map<Vertex, Subgraph> subgraphs = new HashMap<>();
    Map<Vertex, Subgraph> extgraphs = new HashMap<>();
    Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<>();
    Map<Edge, Boolean> isolated = new HashMap<>();
    ArrayList<Subgraph> islands = new ArrayList<>();
    int count;

    /* establish vertex neighbourhood without currently relevant noThruTrafficEdges */
    collectNeighbourVertices(neighborsForVertex, traverseMode, false);

    /* associate each connected vertex with a subgraph */
    count = collectSubGraphs(neighborsForVertex, subgraphs, null, null);
    LOG.info("Islands when {} noThruTraffic is considered: {}", traverseMode, count);

    /* Expand vertex neighbourhood with relevant noThruTrafficEdges
       Note that we can reuse the original neighbour map here
       and simply process a smaller set of noThruTrafficEdges */
    collectNeighbourVertices(neighborsForVertex, traverseMode, true);

    /* Next: generate subgraphs without considering access limitations */
    count = collectSubGraphs(neighborsForVertex, extgraphs, null, islands);
    LOG.info("Islands when {} noThruTraffic is ignored: {}", traverseMode, count);

    /* collect unreachable edges to a map */
    processIslands(islands, isolated, true, traverseMode);

    extgraphs = new HashMap<>(); // let old map go
    islands = new ArrayList<>(); // reset this too

    /* Recompute expanded subgraphs by accepting noThruTraffic edges in graph expansion.
       However, expansion is not allowed to jump from an original island to another one
     */
    collectSubGraphs(neighborsForVertex, extgraphs, subgraphs, islands);

    /* Next round: generate purely noThruTraffic islands if such ones exist */
    count = collectSubGraphs(neighborsForVertex, extgraphs, null, islands);

    LOG.info("{} noThruTraffic island count: {}", traverseMode, count);

    LOG.info("Total {} sub graphs found", islands.size());

    count = processIslands(islands, isolated, false, traverseMode);
    LOG.info("Modified {} islands", count);
  }

  private int processIslands(
    ArrayList<Subgraph> islands,
    Map<Edge, Boolean> isolated,
    boolean markIsolated,
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
      if (island.stopSize() > 0) {
        //for islands with stops
        islandsWithStops++;
        boolean onlyFerry = island.hasOnlyFerryStops();
        // do not remove real islands which have only ferry stops
        if (!onlyFerry && island.streetSize() < pruningThresholdWithStops * adaptivePruningFactor) {
          double sizeCoeff = (adaptivePruningFactor > 1.0)
            ? island.distanceFromOtherGraph(streetIndex, adaptivePruningDistance) /
            adaptivePruningDistance
            : 1.0;

          if (island.streetSize() * sizeCoeff < pruningThresholdWithStops) {
            if (restrictOrRemove(island, isolated, stats, markIsolated, traverseMode)) {
              islandsWithStopsChanged++;
              count++;
            }
          }
        }
      } else {
        //for islands without stops
        if (island.streetSize() < pruningThresholdWithoutStops * adaptivePruningFactor) {
          double sizeCoeff = (adaptivePruningFactor > 1.0)
            ? island.distanceFromOtherGraph(streetIndex, adaptivePruningDistance) /
            adaptivePruningDistance
            : 1.0;
          if (island.streetSize() * sizeCoeff < pruningThresholdWithoutStops) {
            if (restrictOrRemove(island, isolated, stats, markIsolated, traverseMode)) {
              count++;
            }
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
          e instanceof StreetEdge &&
          shouldMatchNoThruType != ((StreetEdge) e).isNoThruTraffic(traverseMode)
        ) {
          continue;
        }
        State[] states = e.traverse(s0);
        if (State.isEmpty(states)) {
          continue;
        }
        Arrays
          .stream(states)
          .map(State::getVertex)
          .forEach(out -> {
            var vertexList = neighborsForVertex.computeIfAbsent(gv, k -> new ArrayList<>());
            vertexList.add(out);

            // note: this assumes that edges are bi-directional. Maybe explicit state traversal is needed for CAR mode.
            vertexList = neighborsForVertex.computeIfAbsent(out, k -> new ArrayList<>());
            vertexList.add(gv);
          });
      }
    }
  }

  private int collectSubGraphs(
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
      Subgraph subgraph = computeConnectedSubgraph(neighborsForVertex, gv, subgraphs, newgraphs);
      for (Iterator<Vertex> vIter = subgraph.streetIterator(); vIter.hasNext();) {
        Vertex subnode = vIter.next();
        newgraphs.put(subnode, subgraph);
      }
      if (islands != null) {
        islands.add(subgraph);
      }
      count++;
    }
    return count;
  }

  private boolean restrictOrRemove(
    Subgraph island,
    Map<Edge, Boolean> isolated,
    Map<String, Integer> stats,
    boolean markIsolated,
    TraverseMode traverseMode
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
              boolean changed = false;

              // not a true island edge but has limited access
              // so convert to noThruTraffic
              if (traverseMode == TraverseMode.CAR) {
                if (!pse.isMotorVehicleNoThruTraffic()) {
                  pse.setMotorVehicleNoThruTraffic(true);
                  changed = true;
                }
              } else if (traverseMode == TraverseMode.BICYCLE) {
                if (!pse.isBicycleNoThruTraffic()) {
                  pse.setBicycleNoThruTraffic(true);
                  changed = true;
                }
              } else if (traverseMode == TraverseMode.WALK) {
                if (!pse.isWalkNoThruTraffic()) {
                  pse.setWalkNoThruTraffic(true);
                  changed = true;
                }
              }
              if (changed) {
                stats.put("noThru", stats.get("noThru") + 1);
                nothru++;
              }
            } else {
              StreetTraversalPermission permission = pse.getPermission();
              boolean changed = false;
              if (traverseMode == TraverseMode.CAR) {
                if (permission.allows(StreetTraversalPermission.CAR)) {
                  permission = permission.remove(StreetTraversalPermission.CAR);
                  changed = true;
                }
              } else if (traverseMode == TraverseMode.BICYCLE) {
                if (permission.allows(StreetTraversalPermission.BICYCLE)) {
                  permission = permission.remove(StreetTraversalPermission.BICYCLE);
                  changed = true;
                }
              } else if (traverseMode == TraverseMode.WALK) {
                if (permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
                  permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                  changed = true;
                }
              }
              if (changed) {
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
    }
    if (markIsolated) {
      return false;
    }

    if (stats.isEmpty()) {
      return false;
    }
    if (traverseMode == TraverseMode.WALK) {
      // note: do not unlink stop if only CAR mode is pruned
      // maybe this needs more logic for flex routing cases
      List<VertexLabel> stopLabels = new ArrayList<>();
      for (Iterator<TransitStopVertex> vIter = island.stopIterator(); vIter.hasNext();) {
        TransitStopVertex v = vIter.next();
        stopLabels.add(v.getLabel());
        Collection<Edge> edges = new ArrayList<>(v.getOutgoing());
        edges.addAll(v.getIncoming());
        for (Edge e : edges) {
          graph.removeEdge(e);
        }
      }
      if (island.stopSize() > 0) {
        // issue about stops that got unlinked in pruning
        issueStore.add(
          new PrunedStopIsland(
            island,
            nothru,
            restricted,
            removed,
            stopLabels.stream().map(Object::toString).collect(Collectors.joining(","))
          )
        );
      }
    }
    issueStore.add(new GraphIsland(island, nothru, restricted, removed, traverseMode.name()));
    return true;
  }

  private Subgraph computeConnectedSubgraph(
    Map<Vertex, ArrayList<Vertex>> neighborsForVertex,
    Vertex startVertex,
    Map<Vertex, Subgraph> anchors,
    Map<Vertex, Subgraph> alreadyMapped
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
        if (!subgraph.contains(neighbor) && !alreadyMapped.containsKey(neighbor)) {
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
