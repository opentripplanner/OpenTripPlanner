package org.opentripplanner.place.nearbystopfinder;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.collection.MinMap;

/**
 * A {@link TraverseVisitor} that collects transit stops and flex area stops during an A* search,
 * replacing the post-search scan of all SPT states in {@link StreetNearbyStopFinder}.
 * <p>
 * With {@code MinimumWeight} dominance and a trivial heuristic (Dijkstra), {@code visitVertex} is
 * called exactly once per vertex with the optimal state, producing the same result set as
 * post-scan filtering of {@code getAllStates()}.
 */
class NearbyStopFinderVisitor implements TraverseVisitor<State, Edge> {

  private static final Predicate<Edge> CAN_BOARD_FLEX_PREDICATE = e ->
    e instanceof StreetEdge se && se.getPermission().allows(TraverseMode.CAR);

  private final Set<Vertex> originVertices;
  private final Set<Vertex> ignoreVertices;
  private final boolean reverseDirection;

  private final List<NearbyStop> transitStopsFound = new ArrayList<>();
  private final MinMap<FeedScopedId, State> statesForAreaStopIds = new MinMap<>(
    Comparator.comparingDouble(State::getWeight)
  );

  NearbyStopFinderVisitor(
    Set<Vertex> originVertices,
    Set<Vertex> ignoreVertices,
    boolean reverseDirection
  ) {
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
      transitStopsFound.add(NearbyStop.nearbyStopForState(state, tsv.getId()));
    }

    if (
      OTPFeature.FlexRouting.isOn() &&
      vertex instanceof StreetVertex streetVertex &&
      !streetVertex.areaStops().isEmpty()
    ) {
      for (FeedScopedId id : streetVertex.areaStops()) {
        if (canBoardFlex(state)) {
          statesForAreaStopIds.putMin(id, state);
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

  Collection<Map.Entry<FeedScopedId, State>> statesForAreaStopIds() {
    return statesForAreaStopIds.entries();
  }

  private boolean canBoardFlex(State state) {
    return reverseDirection
      ? state.getVertex().hasAnyIncomingMatching(CAN_BOARD_FLEX_PREDICATE)
      : state.getVertex().hasAnyOutgoingMatching(CAN_BOARD_FLEX_PREDICATE);
  }
}
