package org.opentripplanner.updater.vehicle_sharing;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.util.SloppyMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.routing.core.TraverseMode.WALK;

public class SharedCarUpdater extends PollingGraphUpdater {
    SimpleStreetSplitter simpleStreetSplitter;
    GraphUpdaterManager graphUpdaterManager;
    VehiclePositionsGetter vehiclePositionsGetter;
    TraverseMode traverseMode;
    Graph graph;
//    List<Vertex> rememberedVehicles = new LinkedList<>();
    List<RentVehicleAnywhereEdge> rememberedVehicles = new LinkedList<>();

    public SharedCarUpdater(VehiclePositionsGetter vehiclePositionsGetter, TraverseMode traverseMode) {
        this.vehiclePositionsGetter = vehiclePositionsGetter;
        this.traverseMode = traverseMode;
    }

    private Vertex chooseCloser(float lat, float lon, Vertex vertex1, Vertex vertex2) {
        if (vertex1 == null) {
            return vertex2;
        } else if (SloppyMath.haversin(lat, lon, vertex1.getLat(), vertex1.getLon()) <
                SloppyMath.haversin(lat, lon, vertex2.getLat(), vertex2.getLon())) {
            return vertex1;
        } else {
            return vertex2;
        }
    }

    Vertex findClosestVertex(VehicleDescription vehicleDescription, List<Vertex> vertexesToChooseFrom) {
        Stream<Vertex> stream;
        if (vertexesToChooseFrom == null) {
            Envelope envelope = new Envelope(new Coordinate(vehicleDescription.getLatitude(), vehicleDescription.getLongitude()));

            stream = simpleStreetSplitter.getIdx().query(envelope).stream()
                    .filter(edge -> edge instanceof StreetEdge)
                    .map(streetEdge -> (StreetEdge) streetEdge)
                    .filter(streetEdge -> streetEdge.canTraverse(new TraverseModeSet(WALK)))
                    .map(Edge::getFromVertex);
        } else {
            stream = vertexesToChooseFrom.stream();
        }

        Vertex v0 = graph.getVertices().stream().findFirst().orElse(null);

        return stream
                .reduce(v0, (previous_best, current) ->
                        chooseCloser(vehicleDescription.getLatitude(), vehicleDescription.getLongitude(), previous_best, current));
    }

    private List<Pair<Vertex,VehicleDescription>> coordsToVertex(List<VehicleDescription> vehiclePositions) {
            return vehiclePositions.stream().map(a -> new ImmutablePair<>(findClosestVertex(a, null),a)).collect(Collectors.toList());
    }

    public List<Pair<Edge,VehicleDescription>> prepareAppearedEdge(VehiclePositionsDiff vehiclePositionsDiff) {
        List<Pair<Vertex,VehicleDescription>> appearedVertex = coordsToVertex(vehiclePositionsDiff.appeared);
        List<Pair<Edge,VehicleDescription>> appearedEdge = new LinkedList<>();
        for (Pair<Vertex,VehicleDescription> vv : appearedVertex) {
            for (Edge edge : vv.getLeft().getOutgoing()) {
                if (edge instanceof RentCarAnywhereEdge) {
                    rememberedVehicles.add((RentVehicleAnywhereEdge) edge);

                    appearedEdge.add(new ImmutablePair<>(edge,vv.getValue()));
                }
            }
        }
        return appearedEdge;
    }



    @Override
    protected void runPolling() throws Exception {
        VehiclePositionsDiff vehiclePositionsDiff = vehiclePositionsGetter.getVehiclePositionsDiff();

        List<Pair<Edge,VehicleDescription>> appearedEdges = prepareAppearedEdge(vehiclePositionsDiff);
        List<Pair<Edge,VehicleDescription>> disappearedEdges = new LinkedList<>();
//        List<Edge> disappearedEdges = prepareDisappearedEdge(vehiclePositionsDiff);

        VehicleSharingGraphWriterRunnable graphWriterRunnable = new VehicleSharingGraphWriterRunnable(appearedEdges, rememberedVehicles);

        graphUpdaterManager.execute(graphWriterRunnable);
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        this.graph = graph;
        this.simpleStreetSplitter = new SimpleStreetSplitter(graph);

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
