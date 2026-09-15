package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
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
 * The generic street search builds a {@code State} — and behind it a {@code StateData}, a map
 * entry and a heap entry — for every vertex it settles. A carpool request builds several such
 * trees per candidate trip and reads a few hundred durations off each, so the trees were the
 * request's memory: at 30 trips they filled about two gigabytes, and the garbage they left behind
 * made concurrent requests stall each other in collection pauses. This class keeps only what the
 * lookups need: for each reached label the vertex, the elapsed milliseconds, the parent label and
 * the edge it was reached by, in flat arrays that grow by doubling. A whole-region tree costs
 * a few tens of bytes per vertex and allocates nothing per step.
 *
 * <h2>Traversal model</h2>
 * The search reproduces what {@link StreetEdge#traverse} does for a plain {@link StreetMode#CAR}
 * request, so it reads the same graph attributes and comes out with the same times:
 * <ul>
 *   <li>a street edge is traversable if its permissions allow a car (barrier vertices included);</li>
 *   <li>no U-turn onto the reverse of the edge just traversed;</li>
 *   <li>no-thru-traffic streets: entering one from a normal street marks the label, and a marked
 *       label may not leave onto a normal street again — one label per vertex and flag, exactly
 *       the split the street dominance function makes;</li>
 *   <li>time is length over the edge's car speed, rounded up to whole milliseconds, plus the
 *       intersection traversal duration of the turn taken (also rounded up), computed with the
 *       request's {@link IntersectionTraversalCalculator} in the same argument order as the
 *       forward and arrive-by branches of {@code StreetEdge};</li>
 *   <li>free edges (the links of temporary origin/destination vertices) take no time;</li>
 *   <li>any other edge type — transit links, elevators, pathways, parking and rental links — is
 *       not driven: a plain car search never needs to leave the drivable network.</li>
 * </ul>
 * The search is a label-setting Dijkstra ordered by elapsed time; the generic search is a
 * label-correcting search ordered by weight with time dominance. Both keep one label per vertex
 * and flag and both converge to the same recurrence, so their times agree except in rare
 * turn-cost tie situations where they may differ by a turn's duration. The tree is bounded like
 * the generic one: no expansion past the duration limit (see {@link DurationSkipEdgeStrategy}),
 * and optionally none outside the legs' feasibility ellipses (see {@link EllipseBounds}).
 *
 * <h2>Paths</h2>
 * Insertion evaluation only reads {@link #elapsedSeconds}. For the few segments that end up in an
 * itinerary, {@link #path} replays the label chain's edges through the real {@code traverse}
 * methods, producing a genuine {@link State} chain with the standard weights and geometry; if the
 * replay disagrees with the label (it should not — the traversal model above is checked against
 * the generic search in tests) the segment is re-routed with a goal-directed street search.
 *
 * <h2>Lifetime</h2>
 * A request builds trees for every candidate trip but queries each trip's trees only while that
 * trip is evaluated. {@link #release()} drops the label storage once a tree is done, so a request
 * holds one trip's trees at a time rather than all of them. Before releasing, the edge chains of
 * the segments that must stay reproducible are taken with {@link #edgesTo} and replayed later via
 * {@link #pathFromEdges}; a path asked of a released tree without such a chain is re-routed.
 */
final class CompactCarTree {

  private static final Logger LOG = LoggerFactory.getLogger(CompactCarTree.class);

  /** Poll the request's cancellation flag this often, counted in settled labels. */
  private static final int TIMEOUT_CHECK_INTERVAL = 4096;

  private static final int NO_LABEL = -1;

  private final Vertex root;
  private final boolean reverse;
  private final Duration limit;
  private final long limitSeconds;
  private final StreetSearchRequest request;
  private final IntersectionTraversalCalculator turnCosts;

  @Nullable
  private final EllipseBounds bounds;

  // One entry per label. A label is a (vertex, enteredNoThruTrafficArea) pair.
  private Vertex[] labelVertex;
  private long[] labelElapsedMs;
  private int[] labelParent;
  private Edge[] labelEdge;
  private boolean[] labelNoThru;
  private int labelCount;

  private LabelMap labels = new LabelMap();
  private boolean released = false;

  private CompactCarTree(
    Vertex root,
    boolean reverse,
    Duration limit,
    @Nullable EllipseBounds bounds,
    int expectedLabels
  ) {
    this.root = root;
    this.reverse = reverse;
    this.limit = limit;
    this.limitSeconds = limit.toSeconds();
    this.request = reverse
      ? StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(true).build()
      : StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    this.turnCosts = request.intersectionTraversalCalculator();
    this.bounds = bounds;
    int capacity = Math.max(64, expectedLabels);
    this.labelVertex = new Vertex[capacity];
    this.labelElapsedMs = new long[capacity];
    this.labelParent = new int[capacity];
    this.labelEdge = new Edge[capacity];
    this.labelNoThru = new boolean[capacity];
  }

  /**
   * Builds the tree, i.e. runs the search to completion.
   *
   * @param root the vertex to search from (or, when {@code reverse}, to)
   * @param reverse arrive-by: follow incoming edges backwards in time
   * @param limit no label is expanded once its elapsed time exceeds this
   * @param bounds if not {@code null}, no edge is followed to a vertex outside these ellipses
   * @throws OTPRequestTimeoutException when the request is cancelled while searching
   */
  static CompactCarTree build(
    Vertex root,
    boolean reverse,
    Duration limit,
    @Nullable EllipseBounds bounds
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    var tree = new CompactCarTree(root, reverse, limit, bounds, 1024);
    tree.search();
    return tree;
  }

  Vertex root() {
    return root;
  }

  boolean isReverse() {
    return reverse;
  }

  /** Number of (vertex, flag) labels the search reached. */
  int size() {
    return labelCount;
  }

  /**
   * Drops the label storage. Afterwards only {@link #pathFromEdges} and {@link #path} (which then
   * re-routes) work; {@link #elapsedSeconds} and {@link #edgesTo} may not be called any more.
   */
  void release() {
    released = true;
    labelVertex = null;
    labelElapsedMs = null;
    labelParent = null;
    labelEdge = null;
    labelNoThru = null;
    labels = null;
  }

  boolean isReleased() {
    return released;
  }

  private void checkNotReleased() {
    if (released) {
      throw new IllegalStateException("This carpool tree has been released");
    }
  }

  /**
   * Elapsed travel time to (or, for a reverse tree, from) {@code vertex} in whole seconds, rounded
   * up like {@link State#getElapsedTimeSeconds()}; {@code -1} if the search did not reach it.
   */
  int elapsedSeconds(Vertex vertex) {
    checkNotReleased();
    int label = bestLabel(vertex);
    return label == NO_LABEL ? -1 : toSeconds(labelElapsedMs[label]);
  }

  /**
   * The edges of the label chain between the root and {@code vertex}, in search order (root
   * first), or {@code null} if the search did not reach it. A few hundred references that let
   * {@link #pathFromEdges} rebuild the path after the tree has been {@link #release() released}.
   */
  @Nullable
  Edge[] edgesTo(Vertex vertex) {
    checkNotReleased();
    int label = bestLabel(vertex);
    return label == NO_LABEL ? null : edgeChain(label);
  }

  /**
   * The street path between the root and {@code vertex} in chronological order, or {@code null} if
   * the search did not reach it. See the class comment on how it is built.
   */
  @Nullable
  GraphPath<State, Edge, Vertex> path(Vertex vertex) {
    if (released) {
      LOG.debug(
        "Path {} {} {} asked of a released carpool tree; re-routing it",
        root,
        arrow(),
        vertex
      );
      return reroute(vertex);
    }
    int label = bestLabel(vertex);
    if (label == NO_LABEL) {
      return null;
    }
    return pathFromEdges(edgeChain(label), vertex);
  }

  /**
   * Replays {@code edges} (as returned by {@link #edgesTo}) through the real edge traversals into a
   * chronological path ending (or, for a reverse tree, starting) at {@code vertex}. Works on a
   * released tree. Falls back to a goal-directed search if the replay does not reproduce the chain.
   */
  @Nullable
  GraphPath<State, Edge, Vertex> pathFromEdges(Edge[] edges, Vertex vertex) {
    var replayed = replay(edges);
    if (replayed != null) {
      return replayed;
    }
    LOG.warn(
      "Replaying the carpool tree path {} {} {} did not reproduce the search; re-routing it",
      root,
      arrow(),
      vertex
    );
    return reroute(vertex);
  }

  private String arrow() {
    return reverse ? "<-" : "->";
  }

  /* ---------------------------------------------------------------- search */

  private void search() {
    var heap = new LongIntHeap();
    int rootLabel = newLabel(root, false, 0L, NO_LABEL, null);
    heap.push(0L, rootLabel);
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
        int existing = labels.get(target, targetNoThru);
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
    if (edge instanceof StreetEdge street) {
      if (!street.canTraverse(TraverseMode.CAR)) {
        return -1;
      }
      if (parentEdge != null && street.isReverseOf(parentEdge)) {
        return -1;
      }
      if (!street.isNoThruTraffic(TraverseMode.CAR) && noThru) {
        // Leaving a no-through-traffic area onto a normal street is blocked.
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
    if (edge instanceof FreeEdge) {
      return 0;
    }
    return -1;
  }

  /** The no-thru-traffic flag after taking {@code edge}; see {@code StreetEdge}. */
  private static boolean noThruAfter(Edge edge, @Nullable Edge parentEdge, boolean noThru) {
    if (
      edge instanceof StreetEdge street &&
      street.isNoThruTraffic(TraverseMode.CAR) &&
      parentEdge instanceof StreetEdge back &&
      !back.isNoThruTraffic(TraverseMode.CAR)
    ) {
      return true;
    }
    return noThru;
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
    labels.put(vertex, noThru, label);
    return label;
  }

  /** The earliest of the (at most two) labels at {@code vertex}, or {@link #NO_LABEL}. */
  private int bestLabel(Vertex vertex) {
    int plain = labels.get(vertex, false);
    int marked = labels.get(vertex, true);
    if (plain == NO_LABEL) {
      return marked;
    }
    if (marked == NO_LABEL) {
      return plain;
    }
    return labelElapsedMs[plain] <= labelElapsedMs[marked] ? plain : marked;
  }

  private static int toSeconds(long elapsedMs) {
    return (int) ((elapsedMs + 999L) / 1000L);
  }

  /* ---------------------------------------------------------------- paths */

  /** The edges from the root to {@code label}, root first. */
  private Edge[] edgeChain(int label) {
    List<Edge> edges = new ArrayList<>();
    for (int l = label; labelParent[l] != NO_LABEL; l = labelParent[l]) {
      edges.add(labelEdge[l]);
    }
    Collections.reverse(edges);
    return edges.toArray(new Edge[0]);
  }

  /**
   * Drives the edges through the real edge traversals, starting from an initial state at the
   * root. Returns {@code null} if some edge refuses the traversal or arrives at a different vertex
   * than the search did.
   */
  @Nullable
  private GraphPath<State, Edge, Vertex> replay(Edge[] edges) {
    State state = new State(root, request);
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
        return null;
      }
      state = next;
    }
    return new GraphPath<>(state);
  }

  /** Goal-directed fallback for a segment whose replay failed. */
  @Nullable
  private GraphPath<State, Edge, Vertex> reroute(Vertex vertex) {
    var from = reverse ? vertex : root;
    var to = reverse ? root : vertex;
    var paths = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(limit))
      .withDominanceFunction(new DominanceFunctions.EarliestArrival())
      .withRequest(request)
      .withFrom(from)
      .withTo(to)
      .getPathsToTarget();
    return paths.isEmpty() ? null : paths.getFirst().toGraphPath();
  }

  /* ---------------------------------------------------------------- primitive collections */

  /**
   * Open-addressing hash map from (vertex identity, flag) to label index. Identity, not
   * {@link Vertex#equals}: co-located temporary vertices are distinct vertices in the graph and
   * are reached separately, exactly as in the generic search's identity-keyed tree.
   */
  private static final class LabelMap {

    private Vertex[] keys = new Vertex[2048];
    private boolean[] flags = new boolean[2048];
    private int[] values = new int[2048];
    private int mask = 2047;
    private int size = 0;

    int get(Vertex vertex, boolean flag) {
      int i = slot(vertex, flag);
      while (keys[i] != null) {
        if (keys[i] == vertex && flags[i] == flag) {
          return values[i];
        }
        i = (i + 1) & mask;
      }
      return NO_LABEL;
    }

    void put(Vertex vertex, boolean flag, int value) {
      if (2 * (size + 1) > keys.length) {
        grow();
      }
      int i = slot(vertex, flag);
      while (keys[i] != null) {
        if (keys[i] == vertex && flags[i] == flag) {
          values[i] = value;
          return;
        }
        i = (i + 1) & mask;
      }
      keys[i] = vertex;
      flags[i] = flag;
      values[i] = value;
      size++;
    }

    private int slot(Vertex vertex, boolean flag) {
      int h = System.identityHashCode(vertex) * 0x9E3779B1;
      h ^= h >>> 15;
      if (flag) {
        h = ~h;
      }
      return h & mask;
    }

    private void grow() {
      var oldKeys = keys;
      var oldFlags = flags;
      var oldValues = values;
      keys = new Vertex[oldKeys.length * 2];
      flags = new boolean[oldKeys.length * 2];
      values = new int[oldKeys.length * 2];
      mask = keys.length - 1;
      size = 0;
      for (int i = 0; i < oldKeys.length; i++) {
        if (oldKeys[i] != null) {
          put(oldKeys[i], oldFlags[i], oldValues[i]);
        }
      }
    }
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
