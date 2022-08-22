package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransferWithDuration;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.refactor.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.refactor.request.NewRouteRequest;
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

  // TODO: 2022-08-19 We should think about what we should about this and smilar methods
  // Previously it took one instance as an input returned another one
  // Now it has to operate on two instances
  // Maybe we should return a Pair<RoutingRequest,RoutingPreferences> instead?
  public static NewRouteRequest prepareTransferRoutingRequest(
    NewRouteRequest request,
    RoutingPreferences preferences
  ) {
    NewRouteRequest rr = request.getStreetSearchRequest(
      request.journeyRequest().transfer().mode(),
      preferences
    );

    rr.setArriveBy(false);
    rr.setDateTime(Instant.ofEpochSecond(0));
    rr.setFrom(null);
    rr.setTo(null);

    var bp = preferences.bike();
    var wp = preferences.walk();
    var sp = preferences.street();

    // Some values are rounded to ease caching in RaptorRequestTransferCache
    bp.setTriangleSafetyFactor(roundTo(bp.triangleSafetyFactor(), 1));
    bp.setTriangleSlopeFactor(roundTo(bp.triangleSlopeFactor(), 1));
    bp.setTriangleTimeFactor(1.0 - bp.triangleSafetyFactor() - bp.triangleSlopeFactor());
    bp.setSwitchCost(roundTo100(bp.switchCost()));
    bp.setSwitchTime(roundTo100(bp.switchTime()));

    // TODO: 2022-08-19 this now lies within parameter class - figure out what to do with it
    // it's a record (immutable) so can be safely reused
    //    rr.wheelchairAccessibility = request.wheelchairAccessibility;

    wp.setSpeed(roundToHalf(wp.speed()));
    bp.setSpeed(roundToHalf(bp.speed()));

    wp.setReluctance(roundTo(wp.reluctance(), 1));
    wp.setStairsReluctance(roundTo(wp.stairsReluctance(), 1));

    wp.setStairsReluctance(roundTo(wp.stairsReluctance(), 1));
    wp.setStairsTimeFactor(roundTo(wp.stairsTimeFactor(), 1));
    sp.setTurnReluctance(roundTo(sp.turnReluctance(), 1));
    wp.setSafetyFactor(roundTo(wp.safetyFactor(), 1));
    sp.setElevatorBoardCost(roundTo100(sp.elevatorBoardCost()));
    sp.setElevatorBoardTime(roundTo100(sp.elevatorBoardTime()));
    sp.setElevatorHopCost(roundTo100(sp.elevatorHopCost()));
    sp.setElevatorHopTime(roundTo100(sp.elevatorHopTime()));

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
    RoutingPreferences routingPreferences = routingContext.pref;
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
