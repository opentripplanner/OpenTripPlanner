package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.StateEditor;

public class Transfer {

  /**
   * Since transfers costs are not computed through a full A* with pruning they can incur an
   * absurdly high cost that overflows the integer cost inside RAPTOR
   * (https://github.com/opentripplanner/OpenTripPlanner/issues/5509).
   * <p>
   * An example would be a transfer using lots of stairs being used on a wheelchair when no
   * wheelchair-specific one has been generated.
   * (see https://docs.opentripplanner.org/en/dev-2.x/Accessibility/).
   * <p>
   * For this reason there is this sanity limit that makes sure that the transfer cost stays below a
   * limit is still very high (several days of transit-equivalent cost) but far away from the
   * integer overflow.
   *
   * @see EdgeTraverser
   */
  private static final int MAX_TRANSFER_COST = Integer.MAX_VALUE / 100 / 30;

  private final int toStop;

  private final int distanceMeters;

  private final List<Edge> edges;

  public Transfer(int toStop, List<Edge> edges) {
    this.toStop = toStop;
    this.edges = edges;
    this.distanceMeters = (int) edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
  }

  public Transfer(int toStopIndex, int distanceMeters) {
    this.toStop = toStopIndex;
    this.distanceMeters = distanceMeters;
    this.edges = null;
  }

  public List<Coordinate> getCoordinates() {
    List<Coordinate> coordinates = new ArrayList<>();
    if (edges == null) {
      return coordinates;
    }
    for (Edge edge : edges) {
      if (edge.getGeometry() != null) {
        coordinates.addAll((Arrays.asList(edge.getGeometry().getCoordinates())));
      }
    }
    return coordinates;
  }

  public int getToStop() {
    return toStop;
  }

  public int getDistanceMeters() {
    return distanceMeters;
  }

  public List<Edge> getEdges() {
    return edges;
  }

  public Optional<RaptorTransfer> asRaptorTransfer(StreetSearchRequest request) {
    WalkPreferences walkPreferences = request.preferences().walk();
    if (edges == null || edges.isEmpty()) {
      double durationSeconds = distanceMeters / walkPreferences.speed();
      final double domainCost = durationSeconds * walkPreferences.reluctance();
      if (domainCost < MAX_TRANSFER_COST) {
        return Optional.of(
          new DefaultRaptorTransfer(
            this.toStop,
            (int) Math.ceil(durationSeconds),
            RaptorCostConverter.toRaptorCost(domainCost),
            this
          )
        );
      } else {
        return Optional.empty();
      }
    }

    StateEditor se = new StateEditor(edges.get(0).getFromVertex(), request);
    se.setTimeSeconds(0);

    var state = EdgeTraverser.traverseEdges(se.makeState(), edges);

    return state
      .filter(s -> s.weight < MAX_TRANSFER_COST)
      .map(s ->
        new DefaultRaptorTransfer(
          this.toStop,
          (int) s.getElapsedTimeSeconds(),
          RaptorCostConverter.toRaptorCost(s.getWeight()),
          this
        )
      );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(Transfer.class)
      .addNum("toStop", toStop)
      .addNum("distance", distanceMeters, "m")
      .toString();
  }
}
