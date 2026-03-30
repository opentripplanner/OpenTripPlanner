package org.opentripplanner.graph_builder.module.nearbystops;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * A {@link TraverseVisitor} that collects transit stops and flex area stops during an A* search,
 * replacing the post-search scan of all SPT states in {@link StreetNearbyStopFinder}.
 * <p>
 * With {@code MinimumWeight} dominance and a trivial heuristic (Dijkstra), {@code visitVertex} is
 * called exactly once per vertex with the optimal state, producing the same result set as
 * post-scan filtering of {@code getAllStates()}.
 */
class NearbyStopFinderVisitor implements TraverseVisitor<State, Edge> {

  private final StopResolver stopResolver;
  private final Set<Vertex> originVertices;
  private final Set<Vertex> ignoreVertices;
  private final boolean reverseDirection;

  private final List<NearbyStop> transitStopsFound = new ArrayList<>();
  private final Multimap<AreaStop, State> areaStopStates = ArrayListMultimap.create();

  NearbyStopFinderVisitor(
    StopResolver stopResolver,
    Set<Vertex> originVertices,
    Set<Vertex> ignoreVertices,
    boolean reverseDirection
  ) {
    this.stopResolver = requireNonNull(stopResolver);
    this.originVertices = requireNonNull(originVertices);
    this.ignoreVertices = requireNonNull(ignoreVertices);
    this.reverseDirection = reverseDirection;
  }

  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();

    if (originVertices.contains(vertex) || ignoreVertices.contains(vertex)) {
      return;
    }

    if (vertex instanceof TransitStopVertex tsv && state.isFinal()) {
      var stop = requireNonNull(stopResolver.getRegularStop(tsv.getId()));
      transitStopsFound.add(NearbyStop.nearbyStopForState(state, stop));
    }

    if (
      OTPFeature.FlexRouting.isOn() &&
      vertex instanceof StreetVertex streetVertex &&
      !streetVertex.areaStops().isEmpty()
    ) {
      for (FeedScopedId id : streetVertex.areaStops()) {
        AreaStop areaStop = Objects.requireNonNull(stopResolver.getAreaStop(id));
        if (canBoardFlex(state)) {
          areaStopStates.put(areaStop, state);
        }
      }
    }
  }

  @Override
  public void visitEdge(Edge edge) {}

  @Override
  public void visitEnqueue() {}

  List<NearbyStop> transitStopsFound() {
    return transitStopsFound;
  }

  Multimap<AreaStop, State> areaStopStates() {
    return areaStopStates;
  }

  private boolean canBoardFlex(State state) {
    var edges = reverseDirection
      ? state.getVertex().getIncoming()
      : state.getVertex().getOutgoing();

    return edges
      .stream()
      .anyMatch(e -> e instanceof StreetEdge se && se.getPermission().allows(TraverseMode.CAR));
  }
}
