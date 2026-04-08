package org.opentripplanner.astar;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AStarBuilder<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>,
  Builder extends AStarBuilder<State, Edge, Vertex, Builder>
> {

  Logger LOG = LoggerFactory.getLogger(AStarBuilder.class);

  private Builder builder;
  private Runnable preStartHook = () ->
    LOG.warn("No pre-start hook provided. Call withPreStartHook() to set one.");
  private RemainingWeightHeuristic<State> heuristic = RemainingWeightHeuristic.TRIVIAL;
  private SkipEdgeStrategy<State, Edge> skipEdgeStrategy;
  private TraverseVisitor<State, Edge> traverseVisitor;
  private boolean arriveBy;
  private Set<Vertex> fromVertices;
  private Set<Vertex> toVertices;
  private SearchTerminationStrategy<State> terminationStrategy;
  private DominanceFunction<State> dominanceFunction;

  protected AStarBuilder() {}

  protected void setBuilder(Builder builder) {
    this.builder = builder;
  }

  public Builder withHeuristic(RemainingWeightHeuristic<State> heuristic) {
    this.heuristic = heuristic;
    return builder;
  }

  /**
   * Set a function that will be called before the search begins. Useful for checking that
   * a timeout has not been reached before the search begins.
   */
  public Builder withPreStartHook(Runnable hook) {
    this.preStartHook = hook;
    return builder;
  }

  public Builder withSkipEdgeStrategy(SkipEdgeStrategy<State, Edge> skipEdgeStrategy) {
    this.skipEdgeStrategy = skipEdgeStrategy;
    return builder;
  }

  public Builder withTraverseVisitor(TraverseVisitor<State, Edge> traverseVisitor) {
    this.traverseVisitor = traverseVisitor;
    return builder;
  }

  public Builder withArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return builder;
  }

  protected boolean arriveBy() {
    return arriveBy;
  }

  public Builder withFrom(Set<Vertex> fromVertices) {
    this.fromVertices = fromVertices;
    return builder;
  }

  public Builder withFrom(Vertex fromVertex) {
    this.fromVertices = Collections.singleton(fromVertex);
    return builder;
  }

  public Builder withTo(Set<Vertex> toVertices) {
    this.toVertices = toVertices;
    return builder;
  }

  public Builder withTo(Vertex toVertex) {
    this.toVertices = Collections.singleton(toVertex);
    return builder;
  }

  public Builder withTerminationStrategy(SearchTerminationStrategy<State> terminationStrategy) {
    this.terminationStrategy = terminationStrategy;
    return builder;
  }

  /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
  public Builder withDominanceFunction(DominanceFunction<State> dominanceFunction) {
    this.dominanceFunction = dominanceFunction;
    return builder;
  }

  protected abstract Duration streetRoutingTimeout();

  public ShortestPathTree<State, Edge, Vertex> getShortestPathTree() {
    return build().getShortestPathTree();
  }

  public List<GraphPath<State, Edge, Vertex>> getPathsToTarget() {
    return build().getPathsToTarget();
  }

  private AStar<State, Edge, Vertex> build() {
    final Set<Vertex> origin = arriveBy ? toVertices : fromVertices;
    final Set<Vertex> destination = arriveBy ? fromVertices : toVertices;

    Collection<State> initialStates = createInitialStates(origin);

    initializeHeuristic(heuristic, origin, destination, arriveBy);

    return new AStar<>(
      heuristic,
      preStartHook,
      skipEdgeStrategy,
      traverseVisitor,
      arriveBy,
      origin,
      destination,
      terminationStrategy,
      Optional.ofNullable(dominanceFunction).orElseGet(this::createDefaultDominanceFunction),
      streetRoutingTimeout(),
      initialStates
    );
  }

  protected abstract Collection<State> createInitialStates(Set<Vertex> originVertices);

  protected abstract void initializeHeuristic(
    RemainingWeightHeuristic<State> heuristic,
    Set<Vertex> origin,
    Set<Vertex> destination,
    boolean arriveBy
  );

  protected abstract DominanceFunction<State> createDefaultDominanceFunction();
}
