package org.opentripplanner.astar;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.StatisticsCallback;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AStarBuilder<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {

  Logger LOG = LoggerFactory.getLogger(AStarBuilder.class);

  private Runnable preStartHook = () ->
    LOG.warn("No pre-start hook provided. Call withPreStartHook() to set one.");
  private RemainingWeightHeuristic<State> heuristic;
  private SkipEdgeStrategy<State, Edge> skipEdgeStrategy;
  private TraverseVisitor<State, Edge> traverseVisitor;
  private boolean arriveBy;
  private Set<Vertex> goalVertices;
  private SearchTerminationStrategy<State> terminationStrategy;
  private DominanceFunction<State> dominanceFunction;
  private StatisticsCallback<Vertex> statisticsCallback = StatisticsCallback.NOOP;
  private Duration timeout;
  private Collection<State> initialStates;

  public AStarBuilder() {}

  public AStarBuilder<State, Edge, Vertex> withHeuristic(
    RemainingWeightHeuristic<State> heuristic
  ) {
    this.heuristic = heuristic;
    return this;
  }

  /**
   * Set a function that will be called before the search begins. Useful for checking that
   * a timeout has not been reached before the search begins.
   */
  public AStarBuilder<State, Edge, Vertex> withPreStartHook(Runnable hook) {
    this.preStartHook = hook;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withSkipEdgeStrategy(
    SkipEdgeStrategy<State, Edge> skipEdgeStrategy
  ) {
    this.skipEdgeStrategy = skipEdgeStrategy;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withTraverseVisitor(
    TraverseVisitor<State, Edge> traverseVisitor
  ) {
    this.traverseVisitor = traverseVisitor;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withStatisticsCallback(
    StatisticsCallback<Vertex> statisticsCallback
  ) {
    this.statisticsCallback = statisticsCallback;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withGoalVertices(Set<Vertex> goalVertices) {
    this.goalVertices = goalVertices;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withTerminationStrategy(
    SearchTerminationStrategy<State> terminationStrategy
  ) {
    this.terminationStrategy = terminationStrategy;
    return this;
  }

  /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
  public AStarBuilder<State, Edge, Vertex> withDominanceFunction(
    DominanceFunction<State> dominanceFunction
  ) {
    this.dominanceFunction = dominanceFunction;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public AStarBuilder<State, Edge, Vertex> withInitialStates(Collection<State> initialStates) {
    this.initialStates = initialStates;
    return this;
  }

  public AStar<State, Edge, Vertex> build() {
    return new AStar<>(
      initialStates,
      arriveBy,
      dominanceFunction,
      goalVertices,
      heuristic,
      traverseVisitor,
      skipEdgeStrategy,
      terminationStrategy,
      timeout,
      preStartHook,
      statisticsCallback
    );
  }
}
