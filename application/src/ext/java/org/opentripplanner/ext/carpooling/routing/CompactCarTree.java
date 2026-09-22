package org.opentripplanner.ext.carpooling.routing;

import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A one-to-many earliest-arrival car search from a root vertex (or, for a reverse tree, to it),
 * stored as one integer label per reached vertex instead of one {@link State} per settled vertex.
 * <p>
 * The generic street search allocates a state, a map entry and a heap entry for every vertex it
 * settles, which made the trees the memory of a carpool request. This tree keeps, per label, the
 * vertex, the elapsed milliseconds, the parent label and the edge it was reached by, in flat
 * arrays. A label is a (vertex, entered-no-thru-traffic-area) pair, the same split the street
 * dominance function makes.
 * <p>
 * The search reproduces {@link StreetEdge#traverse} for a plain {@link StreetMode#CAR} request:
 * car permissions, no U-turns, no leaving a no-through-traffic area onto a normal street, time =
 * length over car speed plus the intersection traversal duration of the turn (each rounded up to
 * whole milliseconds), free edges take no time, and no other edge type is driven. It is a
 * label-setting Dijkstra ordered by time, so it agrees with the generic search except in rare
 * turn-cost ties. Nothing is expanded past the duration limit, and nothing outside the optional
 * {@link EllipseBounds}.
 * <p>
 * Evaluation only reads {@link #elapsedSeconds}. For the few segments that end up in an itinerary,
 * {@link #path} replays the label chain through the real edge traversals, which yields a genuine
 * state chain with the standard weights and geometry.
 */
final class CompactCarTree {

  private static final Logger LOG = LoggerFactory.getLogger(CompactCarTree.class);

  /** Poll the request's cancellation flag this often, counted in settled labels. */
  private static final int TIMEOUT_CHECK_INTERVAL = 4096;

  private static final int NO_LABEL = -1;

  private final Vertex root;
  private final boolean reverse;
  private final long limitSeconds;
  private final StreetSearchRequest request;
  private final IntersectionTraversalCalculator turnCosts;

  @Nullable
  private final EllipseBounds bounds;

  private Vertex[] labelVertex = new Vertex[1024];
  private long[] labelElapsedMs = new long[1024];
  private int[] labelParent = new int[1024];
  private Edge[] labelEdge = new Edge[1024];
  private boolean[] labelNoThru = new boolean[1024];
  private int labelCount;

  /** Label index per vertex, one map per no-thru flag. Identity keys, like the generic search. */
  private final TObjectIntCustomHashMap<Vertex> plainLabels = newLabelMap();
  private final TObjectIntCustomHashMap<Vertex> noThruLabels = newLabelMap();

  private CompactCarTree(
    Vertex root,
    boolean reverse,
    Duration limit,
    @Nullable EllipseBounds bounds
  ) {
    this.root = root;
    this.reverse = reverse;
    this.limitSeconds = limit.toSeconds();
    this.request = carRequest(reverse);
    this.turnCosts = request.intersectionTraversalCalculator();
    this.bounds = bounds;
  }

  /**
   * Runs the search to completion.
   *
   * @param root the vertex to search from (or, when {@code reverse}, to)
   * @param reverse arrive-by: follow incoming edges backwards in time
   * @param limit no label is expanded once its elapsed time exceeds this
   * @param bounds if not {@code null}, no edge is followed to a vertex outside this ellipse
   * @throws OTPRequestTimeoutException when the request is cancelled while searching
   */
  static CompactCarTree build(
    Vertex root,
    boolean reverse,
    Duration limit,
    @Nullable EllipseBounds bounds
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    var tree = new CompactCarTree(root, reverse, limit, bounds);
    tree.search();
    return tree;
  }

  /** Number of (vertex, flag) labels the search reached. */
  int size() {
    return labelCount;
  }

  /**
   * Elapsed travel time to (or, for a reverse tree, from) {@code vertex} in whole seconds, rounded
   * up like {@link State#getElapsedTimeSeconds()}; {@code -1} if the search did not reach it.
   */
  int elapsedSeconds(Vertex vertex) {
    int label = bestLabel(vertex);
    return label == NO_LABEL ? -1 : toSeconds(labelElapsedMs[label]);
  }

  /**
   * The edges between the root and {@code vertex} in search order (root first), or {@code null}
   * if the search did not reach it. A few hundred references that rebuild the path with
   * {@link #path(Edge[], Vertex, boolean)} once the tree itself is gone.
   */
  @Nullable
  Edge[] edgesTo(Vertex vertex) {
    int label = bestLabel(vertex);
    return label == NO_LABEL ? null : edgeChain(label);
  }

  /**
   * The street path between the root and {@code vertex} in chronological order, or {@code null} if
   * the search did not reach it.
   */
  @Nullable
  GraphPath<State, Edge, Vertex> path(Vertex vertex) {
    var edges = edgesTo(vertex);
    return edges == null ? null : path(edges, root, reverse);
  }

  /**
   * Drives {@code edges} (from {@link #edgesTo}) through the real edge traversals, starting at
   * {@code root}, into a chronological path. Should the replay not reproduce the search, which the
   * traversal model above rules out, the segment is re-routed with a goal-directed search.
   */
  static GraphPath<State, Edge, Vertex> path(Edge[] edges, Vertex root, boolean reverse) {
    State state = new State(root, carRequest(reverse));
    for (Edge edge : edges) {
      Vertex expected = reverse ? edge.getFromVertex() : edge.getToVertex();
      State next = null;
      for (State candidate : edge.traverse(state)) {
        if (candidate.getVertex() == expected) {
          next = candidate;
          break;
        }
      }
      if (next == null) {
        return reroute(edges, root, reverse);
      }
      state = next;
    }
    return new GraphPath<>(state);
  }

  private static GraphPath<State, Edge, Vertex> reroute(
    Edge[] edges,
    Vertex root,
    boolean reverse
  ) {
    var last = edges[edges.length - 1];
    var farEnd = reverse ? last.getFromVertex() : last.getToVertex();
    var from = reverse ? farEnd : root;
    var to = reverse ? root : farEnd;
    LOG.warn("Replaying the carpool tree path {} -> {} failed; re-routing it", from, to);
    var paths = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(CarpoolTrip.MAX_TRIP_DURATION))
      .withDominanceFunction(new DominanceFunctions.EarliestArrival())
      .withRequest(carRequest(reverse))
      .withFrom(from)
      .withTo(to)
      .getPathsToTarget();
    if (paths.isEmpty()) {
      throw new IllegalStateException("No street path " + from + " -> " + to);
    }
    return paths.getFirst().toGraphPath();
  }

  private static StreetSearchRequest carRequest(boolean reverse) {
    return StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(reverse).build();
  }

  /* ---------------------------------------------------------------- search */

  private void search() {
    var heap = new LongIntHeap();
    heap.push(0L, newLabel(root, false, 0L, NO_LABEL, null));
    int settled = 0;

    while (!heap.isEmpty()) {
      long elapsedMs = heap.peekKey();
      int label = heap.pop();
      if (elapsedMs > labelElapsedMs[label]) {
        // A better entry for this label was pushed later.
        continue;
      }
      if (++settled % TIMEOUT_CHECK_INTERVAL == 0) {
        OTPRequestTimeoutException.checkForTimeout();
      }
      long elapsedSeconds = toSeconds(elapsedMs);
      if (elapsedSeconds > limitSeconds) {
        // As DurationSkipEdgeStrategy: nothing beyond the limit is expanded.
        continue;
      }
      Vertex vertex = labelVertex[label];
      Edge parentEdge = labelEdge[label];
      boolean noThru = labelNoThru[label];

      for (Edge edge : reverse ? vertex.getIncoming() : vertex.getOutgoing()) {
        Vertex target = reverse ? edge.getFromVertex() : edge.getToVertex();
        if (bounds != null && !bounds.contains(elapsedSeconds, target.getLat(), target.getLon())) {
          continue;
        }
        long edgeMs = traversalMs(edge, parentEdge, noThru);
        if (edgeMs < 0) {
          continue;
        }
        boolean targetNoThru = noThruAfter(edge, parentEdge, noThru);
        long targetMs = elapsedMs + edgeMs;
        int existing = labels(targetNoThru).get(target);
        if (existing == NO_LABEL) {
          heap.push(targetMs, newLabel(target, targetNoThru, targetMs, label, edge));
        } else if (targetMs < labelElapsedMs[existing]) {
          labelElapsedMs[existing] = targetMs;
          labelParent[existing] = label;
          labelEdge[existing] = edge;
          heap.push(targetMs, existing);
        }
      }
    }
  }

  /**
   * Milliseconds it takes to drive {@code edge} after arriving over {@code parentEdge}, or
   * {@code -1} if a car may not take it in this situation. Mirrors {@code StreetEdge.doTraverse}
   * for {@link TraverseMode#CAR}.
   */
  private long traversalMs(Edge edge, @Nullable Edge parentEdge, boolean noThru) {
    if (edge instanceof FreeEdge) {
      return 0;
    }
    if (!(edge instanceof StreetEdge street) || !street.canTraverse(TraverseMode.CAR)) {
      return -1;
    }
    if (parentEdge != null && street.isReverseOf(parentEdge)) {
      return -1;
    }
    if (noThru && !street.isNoThruTraffic(TraverseMode.CAR)) {
      // Leaving a no-through-traffic area onto a normal street.
      return -1;
    }
    double speed = street.calculateSpeed(request, TraverseMode.CAR, false);
    long ms = (long) Math.ceil((1000.0 * street.getDistanceMeters()) / speed);
    if (parentEdge instanceof StreetEdge back) {
      double backSpeed = back.calculateSpeed(request, TraverseMode.CAR, false);
      double turnDuration = 0;
      if (reverse && street.getToVertex() instanceof IntersectionVertex traversed) {
        turnDuration = turnCosts.computeTraversalDuration(
          traversed,
          street,
          back,
          TraverseMode.CAR,
          (float) speed,
          (float) backSpeed
        );
      } else if (!reverse && street.getFromVertex() instanceof IntersectionVertex traversed) {
        turnDuration = turnCosts.computeTraversalDuration(
          traversed,
          back,
          street,
          TraverseMode.CAR,
          (float) backSpeed,
          (float) speed
        );
      }
      ms += (long) Math.ceil(1000.0 * turnDuration);
    }
    return ms;
  }

  /** The no-thru flag after taking {@code edge}: set on entering such a street from a normal one. */
  private static boolean noThruAfter(Edge edge, @Nullable Edge parentEdge, boolean noThru) {
    return (
      noThru ||
      (edge instanceof StreetEdge street &&
        street.isNoThruTraffic(TraverseMode.CAR) &&
        parentEdge instanceof StreetEdge back &&
        !back.isNoThruTraffic(TraverseMode.CAR))
    );
  }

  private int newLabel(Vertex vertex, boolean noThru, long elapsedMs, int parent, Edge edge) {
    if (labelCount == labelVertex.length) {
      int capacity = labelCount * 2;
      labelVertex = Arrays.copyOf(labelVertex, capacity);
      labelElapsedMs = Arrays.copyOf(labelElapsedMs, capacity);
      labelParent = Arrays.copyOf(labelParent, capacity);
      labelEdge = Arrays.copyOf(labelEdge, capacity);
      labelNoThru = Arrays.copyOf(labelNoThru, capacity);
    }
    int label = labelCount++;
    labelVertex[label] = vertex;
    labelElapsedMs[label] = elapsedMs;
    labelParent[label] = parent;
    labelEdge[label] = edge;
    labelNoThru[label] = noThru;
    labels(noThru).put(vertex, label);
    return label;
  }

  private TObjectIntCustomHashMap<Vertex> labels(boolean noThru) {
    return noThru ? noThruLabels : plainLabels;
  }

  private static TObjectIntCustomHashMap<Vertex> newLabelMap() {
    return new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE, 1024, 0.5f, NO_LABEL);
  }

  /** The earliest of the (at most two) labels at {@code vertex}, or {@link #NO_LABEL}. */
  private int bestLabel(Vertex vertex) {
    int plain = plainLabels.get(vertex);
    int marked = noThruLabels.get(vertex);
    if (plain == NO_LABEL || marked == NO_LABEL) {
      return Math.max(plain, marked);
    }
    return labelElapsedMs[plain] <= labelElapsedMs[marked] ? plain : marked;
  }

  private static int toSeconds(long elapsedMs) {
    return (int) ((elapsedMs + 999L) / 1000L);
  }

  /** The edges from the root to {@code label}, root first. */
  private Edge[] edgeChain(int label) {
    List<Edge> edges = new ArrayList<>();
    for (int l = label; labelParent[l] != NO_LABEL; l = labelParent[l]) {
      edges.add(labelEdge[l]);
    }
    Collections.reverse(edges);
    return edges.toArray(new Edge[0]);
  }

  /** Binary min-heap of (long key, int value) pairs in two flat arrays. */
  private static final class LongIntHeap {

    private long[] keys = new long[1024];
    private int[] values = new int[1024];
    private int size = 0;

    boolean isEmpty() {
      return size == 0;
    }

    long peekKey() {
      return keys[0];
    }

    void push(long key, int value) {
      if (size == keys.length) {
        keys = Arrays.copyOf(keys, size * 2);
        values = Arrays.copyOf(values, size * 2);
      }
      int i = size++;
      while (i > 0) {
        int parent = (i - 1) >>> 1;
        if (keys[parent] <= key) {
          break;
        }
        keys[i] = keys[parent];
        values[i] = values[parent];
        i = parent;
      }
      keys[i] = key;
      values[i] = value;
    }

    int pop() {
      int result = values[0];
      size--;
      if (size > 0) {
        long key = keys[size];
        int value = values[size];
        int i = 0;
        while (true) {
          int child = 2 * i + 1;
          if (child >= size) {
            break;
          }
          if (child + 1 < size && keys[child + 1] < keys[child]) {
            child++;
          }
          if (keys[child] >= key) {
            break;
          }
          keys[i] = keys[child];
          values[i] = values[child];
          i = child;
        }
        keys[i] = key;
        values[i] = value;
      }
      return result;
    }
  }
}
