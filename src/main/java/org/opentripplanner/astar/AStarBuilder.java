package org.opentripplanner.astar;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
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

public abstract class AStarBuilder<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>,
  Builder extends AStarBuilder<State, Edge, Vertex, Builder>
> {

  private Builder builder;
  private RemainingWeightHeuristic<State> heuristic = RemainingWeightHeuristic.TRIVIAL;
  private SkipEdgeStrategy<State, Edge> skipEdgeStrategy;
  private TraverseVisitor<State, Edge> traverseVisitor;
  private boolean arriveBy;
  private Set<Vertex> fromVertices;
  private Set<Vertex> toVertices;
  private SearchTerminationStrategy<State> terminationStrategy;
  private DominanceFunction<State> dominanceFunction;
  private Edge originBackEdge;
  private Collection<State> initialStates;

  protected AStarBuilder() {}

  protected void setBuilder(Builder builder) {
    this.builder = builder;
  }

  public Builder setHeuristic(RemainingWeightHeuristic<State> heuristic) {
    this.heuristic = heuristic;
    return builder;
  }

  public Builder setSkipEdgeStrategy(SkipEdgeStrategy<State, Edge> skipEdgeStrategy) {
    this.skipEdgeStrategy = skipEdgeStrategy;
    return builder;
  }

  public Builder setTraverseVisitor(TraverseVisitor<State, Edge> traverseVisitor) {
    this.traverseVisitor = traverseVisitor;
    return builder;
  }

  public Builder setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return builder;
  }

  protected boolean arriveBy() {
    return arriveBy;
  }

  public Builder setFrom(Set<Vertex> fromVertices) {
    this.fromVertices = fromVertices;
    return builder;
  }

  public Builder setFrom(Vertex fromVertex) {
    this.fromVertices = Collections.singleton(fromVertex);
    return builder;
  }

  public Builder setTo(Set<Vertex> toVertices) {
    this.toVertices = toVertices;
    return builder;
  }

  public Builder setTo(Vertex toVertex) {
    this.toVertices = Collections.singleton(toVertex);
    return builder;
  }

  public Builder setTerminationStrategy(SearchTerminationStrategy<State> terminationStrategy) {
    this.terminationStrategy = terminationStrategy;
    return builder;
  }

  /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
  public Builder setDominanceFunction(DominanceFunction<State> dominanceFunction) {
    this.dominanceFunction = dominanceFunction;
    return builder;
  }

  @Nonnull
  protected abstract Duration streetRoutingTimeout();

  public Builder setOriginBackEdge(Edge originBackEdge) {
    this.originBackEdge = originBackEdge;
    return builder;
  }

  public Builder setInitialStates(Collection<State> initialStates) {
    this.initialStates = initialStates;
    return builder;
  }

  public ShortestPathTree<State, Edge, Vertex> getShortestPathTree() {
    return build().getShortestPathTree();
  }

  public List<GraphPath<State, Edge, Vertex>> getPathsToTarget() {
    return build().getPathsToTarget();
  }

  private AStar<State, Edge, Vertex> build() {
    final Set<Vertex> origin = arriveBy ? toVertices : fromVertices;
    final Set<Vertex> destination = arriveBy ? fromVertices : toVertices;

    Collection<State> initialStates;

    if (this.initialStates != null) {
      initialStates = this.initialStates;
    } else {
      initialStates = createInitialStates(origin);

      if (originBackEdge != null) {
        for (var state : initialStates) {
          state.initBackEdge(originBackEdge);
        }
      }
    }

    prepareInitialStates(initialStates);
    initializeHeuristic(heuristic, origin, destination, arriveBy);

    return new AStar<>(
      heuristic,
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

  protected abstract void prepareInitialStates(Collection<State> initialStates);

  protected abstract void initializeHeuristic(
    RemainingWeightHeuristic<State> heuristic,
    Set<Vertex> origin,
    Set<Vertex> destination,
    boolean arriveBy
  );

  protected abstract DominanceFunction<State> createDefaultDominanceFunction();
}
