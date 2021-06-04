package org.opentripplanner.routing.algorithm.raptor.transit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TransferWithDuration;
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
            return Optional.of(new TransferWithDuration(
                    this,
                    (int) Math.ceil(distanceMeters / routingRequest.walkSpeed)
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
            (int) s.getElapsedTimeSeconds()
        ));
    }

    public static RoutingRequest prepareTransferRoutingRequest(RoutingRequest request) {
        RoutingRequest transferRoutingRequest = request.getStreetSearchRequest(request.modes.transferMode);
        transferRoutingRequest.arriveBy = false;
        transferRoutingRequest.dateTime = 0;
        transferRoutingRequest.from = null;
        transferRoutingRequest.to = null;
        return transferRoutingRequest;
    }
}
