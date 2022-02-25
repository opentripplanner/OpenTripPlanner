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
        if (edges == null) { return coordinates; }
        for (Edge edge : edges) {
            if (edge.getGeometry() != null) {
                coordinates.addAll((Arrays.asList(edge.getGeometry().getCoordinates())));
            }
        }
        return coordinates;
    }

    public int getToStop() { return toStop; }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Optional<RaptorTransfer> asRaptorTransfer(RoutingRequest routingRequest) {
        if (edges == null || edges.isEmpty()) {
            double durationSeconds = distanceMeters / routingRequest.walkSpeed;
            return Optional.of(new TransferWithDuration(
                    this,
                    (int) Math.ceil(durationSeconds),
                    RaptorCostConverter.toRaptorCost(durationSeconds * routingRequest.walkReluctance)
            ));
        }

        StateEditor se = new StateEditor(routingRequest, edges.get(0).getFromVertex());
        se.setTimeSeconds(0);

        State s = se.makeState();
        for (Edge e : edges) {
            s = e.traverse(s);
            if (s == null) {
                return Optional.empty();
            }
        }

        return Optional.of(new TransferWithDuration(
            this,
            (int) s.getElapsedTimeSeconds(),
            RaptorCostConverter.toRaptorCost(s.getWeight())
        ));
    }

    public static RoutingRequest prepareTransferRoutingRequest(RoutingRequest request) {
        RoutingRequest transferRoutingRequest = request.getStreetSearchRequest(request.modes.transferMode);
        transferRoutingRequest.arriveBy = false;
        transferRoutingRequest.setDateTime(Instant.ofEpochSecond(0));
        transferRoutingRequest.from = null;
        transferRoutingRequest.to = null;

        // Some of the values are rounded to ease caching in RaptorRequestTransferCache
        transferRoutingRequest.bikeTriangleSafetyFactor = roundTo(request.bikeTriangleSafetyFactor, 1);
        transferRoutingRequest.bikeTriangleSlopeFactor = roundTo(request.bikeTriangleSlopeFactor, 1);
        transferRoutingRequest.bikeTriangleTimeFactor = 1.0 - transferRoutingRequest.bikeTriangleSafetyFactor - transferRoutingRequest.bikeTriangleSlopeFactor;
        transferRoutingRequest.bikeSwitchCost = roundTo100(request.bikeSwitchCost);
        transferRoutingRequest.bikeSwitchTime = roundTo100(request.bikeSwitchTime);

        transferRoutingRequest.wheelchairAccessible = request.wheelchairAccessible;
        transferRoutingRequest.maxWheelchairSlope = request.maxWheelchairSlope;

        transferRoutingRequest.walkSpeed = roundToHalf(request.walkSpeed);
        transferRoutingRequest.bikeSpeed = roundToHalf(request.bikeSpeed);

        transferRoutingRequest.walkReluctance = roundTo(request.walkReluctance, 1);
        transferRoutingRequest.stairsReluctance = roundTo(request.stairsReluctance, 1);
        transferRoutingRequest.stairsTimeFactor = roundTo(request.stairsTimeFactor, 1);
        transferRoutingRequest.turnReluctance = roundTo(request.turnReluctance, 1);

        transferRoutingRequest.elevatorBoardCost = roundTo100(request.elevatorBoardCost);
        transferRoutingRequest.elevatorBoardTime = roundTo100(request.elevatorBoardTime);
        transferRoutingRequest.elevatorHopCost = roundTo100(request.elevatorHopCost);
        transferRoutingRequest.elevatorHopTime = roundTo100(request.elevatorHopTime);

        return transferRoutingRequest;
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
