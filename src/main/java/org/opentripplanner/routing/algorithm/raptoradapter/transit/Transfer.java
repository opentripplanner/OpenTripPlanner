package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransferWithDuration;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class Transfer {

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

  public Optional<RaptorTransfer> asRaptorTransfer(RoutingContext routingContext) {
    RoutingPreferences routingPreferences = routingContext.opt.preferences();
    if (edges == null || edges.isEmpty()) {
      double durationSeconds = distanceMeters / routingPreferences.walk().speed();
      return Optional.of(
        new TransferWithDuration(
          this,
          (int) Math.ceil(durationSeconds),
          RaptorCostConverter.toRaptorCost(durationSeconds * routingPreferences.walk().reluctance())
        )
      );
    }

    StateEditor se = new StateEditor(routingContext, edges.get(0).getFromVertex());
    se.setTimeSeconds(0);

    State s = se.makeState();
    for (Edge e : edges) {
      s = e.traverse(s);
      if (s == null) {
        return Optional.empty();
      }
    }

    return Optional.of(
      new TransferWithDuration(
        this,
        (int) s.getElapsedTimeSeconds(),
        RaptorCostConverter.toRaptorCost(s.getWeight())
      )
    );
  }
}
