package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransferWithDuration;
import org.opentripplanner.routing.api.request.RouteRequest;
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

  public static RouteRequest prepareTransferRoutingRequest(RouteRequest request) {
    RouteRequest rr = request.getStreetSearchRequest(request.journey().transfer().mode());

    var transferPreferences = rr.preferences();

    rr.setArriveBy(false);
    rr.setDateTime(Instant.ofEpochSecond(0));
    rr.setFrom(null);
    rr.setTo(null);

    var bikePreferences = transferPreferences.bike();
    var walkPreferences = transferPreferences.walk();
    var streetPreferences = transferPreferences.street();

    // Some values are rounded to ease caching in RaptorRequestTransferCache
    bikePreferences.setSwitchCost(roundTo100(bikePreferences.switchCost()));
    bikePreferences.setSwitchTime(roundTo100(bikePreferences.switchTime()));

    // it's a record (immutable) so can be safely reused
    transferPreferences.setWheelchairAccessibility(request.preferences().wheelchairAccessibility());

    walkPreferences.setSpeed(roundToHalf(walkPreferences.speed()));
    bikePreferences.setSpeed(roundToHalf(bikePreferences.speed()));

    walkPreferences.setReluctance(roundTo(walkPreferences.reluctance(), 1));
    walkPreferences.setStairsReluctance(roundTo(walkPreferences.stairsReluctance(), 1));
    walkPreferences.setStairsTimeFactor(roundTo(walkPreferences.stairsTimeFactor(), 1));
    streetPreferences.setTurnReluctance(roundTo(streetPreferences.turnReluctance(), 1));
    walkPreferences.setSafetyFactor(roundTo(walkPreferences.safetyFactor(), 1));

    streetPreferences.setElevatorBoardCost(roundTo100(streetPreferences.elevatorBoardCost()));
    streetPreferences.setElevatorBoardTime(roundTo100(streetPreferences.elevatorBoardTime()));
    streetPreferences.setElevatorHopCost(roundTo100(streetPreferences.elevatorHopCost()));
    streetPreferences.setElevatorHopTime(roundTo100(streetPreferences.elevatorHopTime()));

    return rr;
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

  private static double roundToHalf(double input) {
    return ((int) (input * 2 + 0.5)) / 2.0;
  }

  private static double roundTo(double input, int decimals) {
    return Math.round(input * Math.pow(10, decimals)) / Math.pow(10, decimals);
  }

  private static int roundTo100(int input) {
    if (input > 0 && input < 100) {
      return 100;
    }

    return ((input + 50) / 100) * 100;
  }
}
