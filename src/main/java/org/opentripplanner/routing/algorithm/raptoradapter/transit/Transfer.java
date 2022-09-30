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
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
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
    RouteRequest rr = request.clone();

    var transferPreferences = rr.preferences();

    rr.setArriveBy(false);
    rr.setDateTime(Instant.ofEpochSecond(0));
    rr.setFrom(null);
    rr.setTo(null);

    var streetPreferences = transferPreferences.street();

    // TODO VIA - Remove all rounding logic from here and move it into the Preference type
    //          - constructors - We should cache and route on the same normalized values to be
    //          - consistent.

    transferPreferences.withWalk(walk ->
      walk
        .setSpeed(roundToHalf(walk.speed()))
        .setReluctance(roundTo(walk.reluctance(), 1))
        .setStairsReluctance(roundTo(walk.stairsReluctance(), 1))
        .setStairsTimeFactor(roundTo(walk.stairsTimeFactor(), 1))
        .setSafetyFactor(roundTo(walk.safetyFactor(), 1))
    );

    // Some values are rounded to ease caching in RaptorRequestTransferCache
    transferPreferences.withBike(bike ->
      bike
        .setSwitchCost(roundTo100(bike.switchCost()))
        .setSwitchTime(roundTo100(bike.switchTime()))
        .setSpeed(roundToHalf(bike.speed()))
    );

    streetPreferences.setTurnReluctance(roundTo(streetPreferences.turnReluctance(), 1));

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

  public Optional<RaptorTransfer> asRaptorTransfer(RouteRequest request) {
    WalkPreferences walkPreferences = request.preferences().walk();
    if (edges == null || edges.isEmpty()) {
      double durationSeconds = distanceMeters / walkPreferences.speed();
      return Optional.of(
        new TransferWithDuration(
          this,
          (int) Math.ceil(durationSeconds),
          RaptorCostConverter.toRaptorCost(durationSeconds * walkPreferences.reluctance())
        )
      );
    }

    StateEditor se = new StateEditor(
      request,
      request.journey().transfer().mode(),
      edges.get(0).getFromVertex()
    );
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
