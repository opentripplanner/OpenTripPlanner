package org.opentripplanner.updater.vehicle_sharing;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.util.SloppyMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class SharedVehiclesUpdater extends PollingGraphUpdater {
    SimpleStreetSplitter simpleStreetSplitter;
    GraphUpdaterManager graphUpdaterManager;
    VehiclePositionsGetter vehiclePositionsGetter;
    Graph graph;

    public SharedVehiclesUpdater(VehiclePositionsGetter vehiclePositionsGetter) {
        this.vehiclePositionsGetter = vehiclePositionsGetter;
    }

    private Vertex chooseCloser(double lat, double lon, Vertex vertex1, Vertex vertex2) {
        if (vertex1 == null) {
            return vertex2;
        } else if (SloppyMath.haversin(lat, lon, vertex1.getLat(), vertex1.getLon()) <
                SloppyMath.haversin(lat, lon, vertex2.getLat(), vertex2.getLon())) {
            return vertex1;
        } else {
            return vertex2;
        }
    }

    private Vertex findClosestVertex(VehicleDescription vehicleDescription, List<Vertex> vertexesToChooseFrom) {
        Stream<Vertex> stream;
        Envelope envelope = new Envelope(new Coordinate(vehicleDescription.getLongitude(), vehicleDescription.getLatitude()));

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(500);

        final double xscale = Math.cos(vehicleDescription.getLatitude() * Math.PI / 180);

        // Expand more in the longitude direction than the latitude direction to account for converging meridians.
        envelope.expandBy(radiusDeg / xscale, radiusDeg);


        if (vertexesToChooseFrom == null) {
            stream = simpleStreetSplitter.getIdx().query(envelope).stream().map(Edge::getFromVertex);
//            stream = graph.getEdges().stream()
//                    .filter(edge -> edge instanceof RentVehicleAnywhereEdge)
//                    .map(Edge::getFromVertex);
        } else {
            stream = vertexesToChooseFrom.stream();
        }

//        closestVertexes

        Vertex v0 = graph.getVertices().stream().findFirst().orElse(null);


        return stream.reduce(v0, (previous_best, current) ->
                chooseCloser(vehicleDescription.getLatitude(), vehicleDescription.getLongitude(), previous_best, current));
    }

    @VisibleForTesting
    public List<Pair<Vertex, VehicleDescription>> coordsToVertex(List<VehicleDescription> vehiclePositions) {
        return vehiclePositions.stream()
                .map(vehicle -> new ImmutablePair<>(findClosestVertex(vehicle, null), vehicle))
                .collect(toList());
    }

    @VisibleForTesting
    public List<Pair<RentVehicleAnywhereEdge, VehicleDescription>> prepareAppearedEdge(
            List<Pair<Vertex, VehicleDescription>> appearedVertex) {

        List<Pair<RentVehicleAnywhereEdge, VehicleDescription>> appearedEdge = new LinkedList<>();

        for (Pair<Vertex, VehicleDescription> vv : appearedVertex) {
            vv.getLeft().getOutgoing().stream()
                    .filter(edge -> edge instanceof RentVehicleAnywhereEdge)
                    .map(edge -> (RentVehicleAnywhereEdge) edge)
                    .forEach(edge -> appearedEdge.add(new ImmutablePair<>(edge, vv.getRight())));
        }
        return appearedEdge;
    }

    public List<RentVehicleAnywhereEdge> getRememberedEdges(List<Pair<Vertex, VehicleDescription>> appearedVertex) {
        return appearedVertex.stream()
                .map(Pair::getLeft)
                .map(Vertex::getOutgoing)
                .flatMap(Collection::stream)
                .filter(edge -> edge instanceof RentVehicleAnywhereEdge)
                .map(edge -> (RentVehicleAnywhereEdge) edge)
                .collect(toList());
    }

    @Override
    protected void runPolling() {
        VehiclePositionsDiff diff = vehiclePositionsGetter.getVehiclePositionsDiff();
        List<Pair<Vertex, VehicleDescription>> appearedVertex = coordsToVertex(diff.appeared);

        List<Pair<RentVehicleAnywhereEdge, VehicleDescription>> appearedEdges = prepareAppearedEdge(appearedVertex);
        List<RentVehicleAnywhereEdge> rememberedVehicles = getRememberedEdges(appearedVertex);
        VehicleSharingGraphWriterRunnable graphWriterRunnable =
                new VehicleSharingGraphWriterRunnable(appearedEdges, rememberedVehicles);
        graphUpdaterManager.execute(graphWriterRunnable);
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        this.pollingPeriodSeconds = 3;
        this.graph = graph;
        this.simpleStreetSplitter = new SimpleStreetSplitter(graph);
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        configurePolling(graph, config);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.graphUpdaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) throws Exception {
        this.graph = graph;
        this.simpleStreetSplitter = new SimpleStreetSplitter(graph);
    }

    @Override
    public void teardown() {

    }
}
