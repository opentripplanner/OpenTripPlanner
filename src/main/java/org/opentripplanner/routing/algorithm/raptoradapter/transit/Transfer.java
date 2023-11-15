package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Duration;
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
   * Since transfers costs are not computed through a full A* they can incur an absurdly high
   * cost that overflows the integer cost inside raptor.
   * <p>
   * An example would be a transfer using lots of stairs being used on a wheelchair when no
   * wheelchair-specific one has been generated.
   * (see https://docs.opentripplanner.org/en/dev-2.x/Accessibility/).
   * <p>
   * For this reason there is this sanit limit that make sure that the transfer cost stays inside a
   * bound that is still very high but far away from the integer overflow.
   */
  private final double MAX_TRANSFER_COST = Duration.ofDays(3).toSeconds();

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
      if (domainCost > MAX_TRANSFER_COST) {
        return Optional.empty();
      } else {
        return Optional.of(
          new DefaultRaptorTransfer(
            this.toStop,
            (int) Math.ceil(durationSeconds),
            RaptorCostConverter.toRaptorCost(domainCost),
            this
          )
        );
      }
    }

    StateEditor se = new StateEditor(edges.get(0).getFromVertex(), request);
    se.setTimeSeconds(0);

    var state = EdgeTraverser.traverseEdges(se.makeState(), edges);

    return state
      .filter(s -> s.weight < MAX_TRANSFER_COST)
      .map(s -> {
        final int raptorCost = RaptorCostConverter.toRaptorCost(s.getWeight());
        return new DefaultRaptorTransfer(
          this.toStop,
          (int) s.getElapsedTimeSeconds(),
          raptorCost,
          this
        );
      });
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
