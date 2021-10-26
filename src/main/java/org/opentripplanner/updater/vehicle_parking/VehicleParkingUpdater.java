package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Graph updater that dynamically sets availability information on vehicle parking lots.
 * This updater fetches data from a single {@link VehicleParkingDataSource}.
 */
public class VehicleParkingUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleParkingUpdater.class);

    private GraphUpdaterManager updaterManager;

    private final Map<VehicleParking, List<VehicleParkingEntranceVertex>> verticesByPark = new HashMap<>();

    private final Map<VehicleParking, List<DisposableEdgeCollection>> tempEdgesByPark = new HashMap<>();

    private final VehicleParkingDataSource source;

    private VertexLinker linker;

    private VehicleParkingService vehicleParkingService;

    public VehicleParkingUpdater(VehicleParkingUpdaterParameters parameters) {
        super(parameters);
        // Set source from preferences
        source = new KmlBikeParkDataSource(parameters.sourceParameters());

        LOG.info("Creating bike-park updater running every {} seconds : {}", pollingPeriodSeconds, source);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Creation of network linker library will not modify the graph
        linker = graph.getLinker();
        // Adding a vehicle parking station service needs a graph writer runnable
        vehicleParkingService = graph.getService(VehicleParkingService.class, true);
    }

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating vehicle parkings from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<VehicleParking> vehicleParkings = source.getVehicleParkings();

        // Create graph writer runnable to apply these stations to the graph
        VehicleParkingGraphWriterRunnable graphWriterRunnable = new VehicleParkingGraphWriterRunnable(vehicleParkings, verticesByPark.keySet());
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class VehicleParkingGraphWriterRunnable implements GraphWriterRunnable {

        private final List<VehicleParking> updatedVehicleParkings;
        private final Map<FeedScopedId, VehicleParking> oldVehicleParkings;

        private VehicleParkingGraphWriterRunnable(List<VehicleParking> updatedVehicleParkings, Set<VehicleParking> oldVehicleParkings) {
            this.updatedVehicleParkings = updatedVehicleParkings;
            this.oldVehicleParkings = oldVehicleParkings.stream()
                .collect(Collectors.toMap(VehicleParking::getId, v -> v));
        }

        @Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<VehicleParking> vehicleParkingSet = new HashSet<>();
            /* Add any new park and update space available for existing parks */
            for (VehicleParking updatedVehicleParking : updatedVehicleParkings) {
                vehicleParkingService.addVehicleParking(updatedVehicleParking);
                vehicleParkingSet.add(updatedVehicleParking);
                List<VehicleParkingEntranceVertex> vehicleParkingVertices = verticesByPark.get(updatedVehicleParking);
                if (vehicleParkingVertices == null || vehicleParkingVertices.isEmpty()) {
                    vehicleParkingVertices = VehicleParkingHelper.createVehicleParkingVertices(graph, updatedVehicleParking);

                    var disposableEdgeCollectionsForVertex = linkVehicleParkingVertexToStreets(vehicleParkingVertices);

                    VehicleParkingHelper.linkVehicleParkingEntrances(vehicleParkingVertices);
                    verticesByPark.put(updatedVehicleParking, vehicleParkingVertices);
                    tempEdgesByPark.put(updatedVehicleParking, disposableEdgeCollectionsForVertex);
                } else {
                    oldVehicleParkings.get(updatedVehicleParking.getId()).updateVehiclePlaces(updatedVehicleParking.getAvailability());
                }
            }
            /* Remove existing parks that were not present in the update */
            List<VehicleParking> toRemove = new ArrayList<>();
            for (Entry<VehicleParking, List<VehicleParkingEntranceVertex>> entry : verticesByPark.entrySet()) {
                VehicleParking vehicleParking = entry.getKey();
                if (vehicleParkingSet.contains(vehicleParking))
                    continue;
                toRemove.add(vehicleParking);
                vehicleParkingService.removeVehicleParking(vehicleParking);
                entry.getValue().forEach(v -> removeVehicleParkingEdgesFromGraph(v, graph));
            }
            for (VehicleParking vehicleParking : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByPark.remove(vehicleParking);
                tempEdgesByPark.get(vehicleParking).forEach(DisposableEdgeCollection::disposeEdges);
                tempEdgesByPark.remove(vehicleParking);
            }
        }

        private List<DisposableEdgeCollection> linkVehicleParkingVertexToStreets(List<VehicleParkingEntranceVertex> vehicleParkingVertices) {
            List<DisposableEdgeCollection> disposableEdgeCollectionsForVertex = new ArrayList<>();
            for (var vehicleParkingVertex : vehicleParkingVertices) {
                var disposableEdges = linkVehicleParkingForRealtime(vehicleParkingVertex);
                disposableEdgeCollectionsForVertex.addAll(disposableEdges);

                if (vehicleParkingVertex.getOutgoing().isEmpty()) {
                    LOG.info("Vehicle parking {} unlinked", vehicleParkingVertex);
                }
            }
            return disposableEdgeCollectionsForVertex;
        }

        private List<DisposableEdgeCollection> linkVehicleParkingForRealtime(
            VehicleParkingEntranceVertex vehicleParkingEntranceVertex) {

            List<DisposableEdgeCollection> disposableEdgeCollections = new ArrayList<>();
            if (vehicleParkingEntranceVertex.isWalkAccessible()) {
                var disposableWalkEdges = linker.linkVertexForRealTime(
                    vehicleParkingEntranceVertex,
                    new TraverseModeSet(TraverseMode.WALK),
                    LinkingDirection.BOTH_WAYS,
                    (vertex, streetVertex) -> List.of(
                        new StreetVehicleParkingLink((VehicleParkingEntranceVertex) vertex, streetVertex),
                        new StreetVehicleParkingLink(streetVertex, (VehicleParkingEntranceVertex) vertex)
                    ));
                disposableEdgeCollections.add(disposableWalkEdges);
            }

            if (vehicleParkingEntranceVertex.isCarAccessible()) {
                var disposableCarEdges = linker.linkVertexForRealTime(
                    vehicleParkingEntranceVertex,
                    new TraverseModeSet(TraverseMode.CAR),
                    LinkingDirection.BOTH_WAYS,
                    (vertex, streetVertex) -> List.of(
                        new StreetVehicleParkingLink((VehicleParkingEntranceVertex) vertex, streetVertex),
                        new StreetVehicleParkingLink(streetVertex, (VehicleParkingEntranceVertex) vertex)
                    ));
                disposableEdgeCollections.add(disposableCarEdges);
            }

            return disposableEdgeCollections;
        }

        private void removeVehicleParkingEdgesFromGraph(VehicleParkingEntranceVertex entranceVertex, Graph graph) {
            entranceVertex.getIncoming().stream().filter(VehicleParkingEdge.class::isInstance).forEach(graph::removeEdge);
            entranceVertex.getOutgoing().stream().filter(VehicleParkingEdge.class::isInstance).forEach(graph::removeEdge);
        }
    }
}
