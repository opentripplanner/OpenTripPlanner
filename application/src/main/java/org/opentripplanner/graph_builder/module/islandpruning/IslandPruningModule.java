package org.opentripplanner.graph_builder.module.islandpruning;

import gnu.trove.list.array.TIntArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.GraphConnectivity;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.utils.collection.StreamUtils;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module is part of the {@link GraphBuilderModule} process. It extends the functionality of
 * PruneFloatingIslands by considering also through traffic limitations. It is quite common that no
 * thru edges break connectivity of the graph, creating islands. The quality of the graph can be
 * improved by converting such islands to nothru state so that routing can start / end from such an
 * island.
 */
public class IslandPruningModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(IslandPruningModule.class);

  private static final int NO_SUBGRAPH = -1;

  private final Graph graph;
  private final TransitRepository transitRepository;
  private final DataImportIssueStore issueStore;

  @Nullable
  private final StreetLinkerModule streetLinkerModule;

  private final IslandPruningParameters parameters;

  public IslandPruningModule(
    Graph graph,
    TransitRepository transitRepository,
    DataImportIssueStore issueStore,
    @Nullable StreetLinkerModule streetLinkerModule,
    IslandPruningParameters parameters
  ) {
    this.graph = graph;
    this.transitRepository = transitRepository;
    this.issueStore = issueStore;
    this.streetLinkerModule = streetLinkerModule;
    this.parameters = parameters;
  }

  @Override
  public void buildGraph() {
    Instant start = Instant.now();

    LOG.info(
      "Threshold with stops {}, without stops {}, adaptive coeff {} and distance {}",
      parameters.pruningThresholdIslandWithStops(),
      parameters.pruningThresholdIslandWithoutStops(),
      parameters.adaptivePruningFactor(),
      parameters.adaptivePruningDistance()
    );

    // vertices are only removed at the very end, so the index can be shared by all modes
    var vertexIndex = new VertexIndex(graph.getVertices());
    pruneIslands(vertexIndex, TraverseMode.BICYCLE);
    pruneIslands(vertexIndex, TraverseMode.WALK);
    pruneIslands(vertexIndex, TraverseMode.CAR);

    // reconnect stops that got disconnected
    if (streetLinkerModule != null) {
      LOG.info("Reconnecting stops");
      streetLinkerModule.linkTransitStops(graph, transitRepository);
    }

    // clean up pruned street vertices
    // note that visibility vertices must not be removed from the graph
    // because serialization will break. Edge lists are reconstructed
    // only for graph vertices after loading the graph
    Set<Vertex> visibilityVertices = StreamUtils.ofIterable(graph.findEdges(AreaEdge.class))
      .map(AreaEdge::getArea)
      .distinct()
      .flatMap(area -> area.visibilityVertices().stream())
      .collect(Collectors.toSet());

    int removed = 0;
    for (Vertex v : graph.getVerticesOfType(StreetVertex.class)) {
      if (v.getDegreeOut() + v.getDegreeIn() == 0 && !visibilityVertices.contains(v)) {
        graph.remove(v);
        removed += 1;
      }
    }
    LOG.info("Removed {} edgeless street vertices", removed);

    LOG.info(
      "Island pruning completed in {}",
      DurationUtils.durationToStr(Duration.between(start, Instant.now()))
    );
  }

  /**
   * Island pruning strategy:
   * 1. Extract islands without using noThruTraffic edges at all.
   * 2. Then create expanded islands by accepting noThruTraffic edges, but do not jump across
   *    original islands! Note: these expanded islands can overlap.
   * 3. Relax connectivity even more: generate islands by allowing jumps between islands. Find out
   *    unreachable edges of small islands.
   * 4. Analyze small expanded islands (from step 2). Convert edges which are reachable only via
   *    noThruTraffic edges to noThruTraffic state. Remove traversal mode specific access from
   *    unreachable edges. Remove unconnected edges.
   */
  private void pruneIslands(VertexIndex vertexIndex, TraverseMode traverseMode) {
    LOG.debug("nothru pruning");
    Map<Edge, Boolean> isolated = new HashMap<>();
    ArrayList<Subgraph> islands = new ArrayList<>();
    int count;

    /* establish vertex neighbourhood, with and without currently relevant noThruTrafficEdges */
    var thruEdges = new TIntArrayList();
    var noThruEdges = new TIntArrayList();
    collectNeighbourVertices(vertexIndex, traverseMode, thruEdges, noThruEdges);
    int vertexCount = vertexIndex.size();
    var thruNeighbours = new Adjacency(vertexCount, thruEdges);
    var allNeighbours = new Adjacency(vertexCount, thruEdges, noThruEdges);

    var search = new SubgraphSearch(vertexIndex);

    /* associate each connected vertex with a subgraph */
    int[] subgraphs = newSubgraphMapping(vertexCount);
    count = search.collectSubGraphs(thruNeighbours, subgraphs, null, null);
    LOG.info("Islands when {} noThruTraffic is considered: {}", traverseMode, count);

    /* Next: generate subgraphs without considering access limitations */
    int[] extgraphs = newSubgraphMapping(vertexCount);
    count = search.collectSubGraphs(allNeighbours, extgraphs, null, islands);
    LOG.info("Islands when {} noThruTraffic is ignored: {}", traverseMode, count);

    /* collect unreachable edges to a map */
    processIslands(islands, isolated, true, traverseMode);

    extgraphs = newSubgraphMapping(vertexCount);
    islands = new ArrayList<>();

    /* Recompute expanded subgraphs by accepting noThruTraffic edges in graph expansion.
       However, expansion is not allowed to jump from an original island to another one
     */
    search.collectSubGraphs(allNeighbours, extgraphs, subgraphs, islands);

    /* Next round: generate purely noThruTraffic islands if such ones exist */
    count = search.collectSubGraphs(allNeighbours, extgraphs, null, islands);

    LOG.info("{} noThruTraffic island count: {}", traverseMode, count);

    LOG.info("Total {} sub graphs found", islands.size());

    count = processIslands(islands, isolated, false, traverseMode);
    LOG.info("Modified {} islands", count);
  }

  /**
   * A vertex to subgraph mapping, indexed on {@link VertexIndex} ids. Each subgraph has a unique
   * id, {@link #NO_SUBGRAPH} marks vertices that do not belong to one.
   */
  private static int[] newSubgraphMapping(int vertexCount) {
    int[] mapping = new int[vertexCount];
    Arrays.fill(mapping, NO_SUBGRAPH);
    return mapping;
  }

  private int processIslands(
    ArrayList<Subgraph> islands,
    Map<Edge, Boolean> isolated,
    boolean markIsolated,
    TraverseMode traverseMode
  ) {
    var stats = new PruningStats();

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

    double adaptivePruningFactor = parameters.adaptivePruningFactor();
    int adaptivePruningDistance = parameters.adaptivePruningDistance();

    for (Subgraph island : islands) {
      if (island == largest) {
        continue;
      }
      if (island.stopSize() > 0) {
        //for islands with stops
        stats.incrementIslandsWithStops();
        boolean onlyFerry = island.hasOnlyFerryStops();
        int pruningThresholdWithStops = parameters.pruningThresholdIslandWithStops();
        // do not remove real islands which have only ferry stops
        if (!onlyFerry && island.streetSize() < pruningThresholdWithStops * adaptivePruningFactor) {
          double sizeCoeff =
            adaptivePruningFactor > 1.0
              ? island.distanceFromOtherGraph(graph, adaptivePruningDistance) /
                adaptivePruningDistance
              : 1.0;

          if (island.streetSize() * sizeCoeff < pruningThresholdWithStops) {
            if (restrictOrRemove(island, isolated, stats, markIsolated, traverseMode)) {
              stats.incrementIslandsWithStopsChanged();
              stats.incrementModifiedIslands();
            }
          }
        }
      } else {
        //for islands without stops
        int pruningThresholdWithoutStops = parameters.pruningThresholdIslandWithoutStops();
        if (island.streetSize() < pruningThresholdWithoutStops * adaptivePruningFactor) {
          double sizeCoeff =
            adaptivePruningFactor > 1.0
              ? island.distanceFromOtherGraph(graph, adaptivePruningDistance) /
                adaptivePruningDistance
              : 1.0;
          if (island.streetSize() * sizeCoeff < pruningThresholdWithoutStops) {
            if (restrictOrRemove(island, isolated, stats, markIsolated, traverseMode)) {
              stats.incrementModifiedIslands();
            }
          }
        }
      }
    }
    if (markIsolated) {
      LOG.info("Detected {} isolated edges", stats.isolated());
    } else {
      LOG.info("Number of islands with stops: {}", stats.islandsWithStops());
      LOG.info("Modified connectivity of {} islands with stops", stats.islandsWithStopsChanged());
      LOG.info("Removed {} edges", stats.removed());
      LOG.info("Removed traversal mode from {} edges", stats.restricted());
      LOG.info("Converted {} edges to noThruTraffic", stats.noThru());
      issueStore.add(
        new GraphConnectivity(
          traverseMode,
          islands.size(),
          stats.islandsWithStops(),
          stats.islandsWithStopsChanged(),
          stats.removed(),
          stats.restricted(),
          stats.noThru()
        )
      );
    }
    return stats.modifiedIslands();
  }

  /**
   * Collect the vertex pairs connected by an edge traversable with the given mode, as interleaved
   * {@code (from, to)} {@link VertexIndex} ids. Street edges which are noThruTraffic for the mode
   * go to {@code noThruEdges}, all others to {@code thruEdges}.
   */
  private void collectNeighbourVertices(
    VertexIndex vertexIndex,
    TraverseMode traverseMode,
    TIntArrayList thruEdges,
    TIntArrayList noThruEdges
  ) {
    StreetMode streetMode = switch (traverseMode) {
      case WALK -> StreetMode.WALK;
      case BICYCLE -> StreetMode.BIKE;
      case CAR -> StreetMode.CAR;
      default -> throw new IllegalArgumentException();
    };

    StreetSearchRequest request = StreetSearchRequest.of().withMode(streetMode).build();

    // only the graph vertices, not those added to the index while collecting
    int graphVertexCount = vertexIndex.size();
    for (int from = 0; from < graphVertexCount; from++) {
      if (!vertexIndex.isStreetVertex(from)) {
        continue;
      }
      Vertex gv = vertexIndex.vertex(from);
      State s0 = null;
      for (Edge e : gv.getOutgoing()) {
        if (e instanceof StreetEdge se) {
          if (!canTraverse(se, traverseMode)) {
            continue;
          }
          // note: this assumes that edges are bi-directional. Maybe explicit state traversal is needed for CAR mode.
          var edges = se.isNoThruTraffic(traverseMode) ? noThruEdges : thruEdges;
          edges.add(from);
          edges.add(vertexIndex.idOf(se.getToVertex()));
        } else {
          // Fall back to a real traversal for edge types (eg. escalators, pathways, vehicle
          // rental/parking edges) that don't behave like a plain permission-gated street edge.
          if (s0 == null) {
            s0 = new State(gv, request);
          }
          State[] states = e.traverse(s0);
          if (State.isEmpty(states)) {
            continue;
          }
          for (State state : states) {
            thruEdges.add(from);
            thruEdges.add(vertexIndex.idOf(state.getVertex()));
          }
        }
      }
    }
  }

  /**
   * Cheap connectivity-only equivalent of {@link StreetEdge#traverse}, for the plain WALK/
   * BICYCLE/CAR travel modes this module cares about: a permission (incl. barrier vertex) check,
   * with the same "walk the bike if it can't be ridden" fallback {@link StreetEdge#traverse}
   * applies. This avoids allocating a {@link State}/{@code StateEditor} and computing speed/cost,
   * none of which this module reads - it only needs to know whether the edge can be used at all.
   */
  private static boolean canTraverse(StreetEdge edge, TraverseMode traverseMode) {
    if (traverseMode == TraverseMode.BICYCLE) {
      return edge.canTraverse(TraverseMode.BICYCLE) || edge.canTraverse(TraverseMode.WALK);
    }
    return edge.canTraverse(traverseMode);
  }

  private boolean restrictOrRemove(
    Subgraph island,
    Map<Edge, Boolean> isolated,
    PruningStats stats,
    boolean markIsolated,
    TraverseMode traverseMode
  ) {
    int nothru = 0;
    int removed = 0;
    int restricted = 0;
    //iterate over the street vertex of the subgraph
    for (Vertex v : island.streetVertices()) {
      Collection<Edge> outgoing = new ArrayList<>(v.getOutgoing());
      for (Edge e : outgoing) {
        if (e instanceof StreetEdge) {
          if (markIsolated) {
            isolated.put(e, true);
            stats.incrementIsolated();
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
                stats.incrementNoThru();
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
                  graph.removeEdge(pse);
                  stats.incrementRemoved();
                  removed++;
                } else {
                  pse.setPermission(permission);
                  stats.incrementRestricted();
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

    if (traverseMode == TraverseMode.WALK) {
      // note: do not unlink stop if only CAR mode is pruned
      // maybe this needs more logic for flex routing cases
      List<VertexLabel> stopLabels = new ArrayList<>();
      for (var v : island.stopVertices()) {
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

  /**
   * Breadth-first search for connected subgraphs over an {@link Adjacency}. Keeps its scratch
   * arrays between searches, so that each search only costs the size of the subgraph found.
   */
  private static class SubgraphSearch {

    private final VertexIndex vertexIndex;
    private final int[] queue;
    /** The search that last visited each vertex, avoids clearing a visited set between searches. */
    private final int[] visitedBy;
    private int searchId = 0;
    private int nextSubgraphId = 0;

    SubgraphSearch(VertexIndex vertexIndex) {
      this.vertexIndex = vertexIndex;
      // a vertex is enqueued at most once per search, except the start vertex which is enqueued
      // again when reached from one of its neighbours
      this.queue = new int[vertexIndex.size() + 1];
      this.visitedBy = new int[vertexIndex.size()];
    }

    /**
     * @param newgraphs put new subgraphs here
     * @param anchors optional isolation mapping from a previous round
     * @param islands final list of islands or null
     */
    int collectSubGraphs(
      Adjacency neighbours,
      int[] newgraphs,
      @Nullable int[] anchors,
      @Nullable List<Subgraph> islands
    ) {
      int count = 0;
      for (int v = 0; v < newgraphs.length; v++) {
        if (!vertexIndex.isStreetVertex(v)) {
          continue;
        }
        if (anchors != null && anchors[v] == NO_SUBGRAPH) {
          // do not start new graph generation from non-classified vertex
          continue;
        }
        // already processed
        if (newgraphs[v] != NO_SUBGRAPH) {
          continue;
        }
        if (!neighbours.hasNeighbours(v)) {
          continue;
        }
        Subgraph subgraph = computeConnectedSubgraph(neighbours, v, anchors, newgraphs);
        if (islands != null) {
          islands.add(subgraph);
        }
        count++;
      }
      return count;
    }

    /**
     * Find the subgraph connected to {@code startVertex}, which may not enter a vertex already
     * mapped in {@code alreadyMapped} or, if {@code anchors} is given, belonging to a different
     * anchor subgraph than the start vertex. Stop vertices are not marked in {@code alreadyMapped},
     * so they can be part of several subgraphs.
     */
    private Subgraph computeConnectedSubgraph(
      Adjacency neighbours,
      int startVertex,
      @Nullable int[] anchors,
      int[] alreadyMapped
    ) {
      int subgraphId = nextSubgraphId++;
      int visited = ++searchId;
      Subgraph subgraph = new Subgraph(vertexIndex);
      int anchor = anchors == null ? NO_SUBGRAPH : anchors[startVertex];

      int head = 0;
      int tail = 0;
      queue[tail++] = startVertex;
      while (head < tail) {
        int vertex = queue[head++];
        for (int i = neighbours.firstNeighbour(vertex); i < neighbours.endNeighbour(vertex); i++) {
          int neighbour = neighbours.neighbour(i);
          if (visitedBy[neighbour] == visited || alreadyMapped[neighbour] != NO_SUBGRAPH) {
            continue;
          }
          if (anchor != NO_SUBGRAPH) {
            int compare = anchors[neighbour];
            if (compare != NO_SUBGRAPH && compare != anchor) {
              // do not enter a new island
              continue;
            }
          }
          visitedBy[neighbour] = visited;
          if (!vertexIndex.isStopVertex(neighbour)) {
            alreadyMapped[neighbour] = subgraphId;
          }
          subgraph.addVertex(neighbour);
          queue[tail++] = neighbour;
        }
      }
      return subgraph;
    }
  }
}
