package org.opentripplanner.updater.vehicle_rental;

import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.edgetype.StreetVehicleRentalLink;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Dynamic vehicle-rental station updater which updates the Graph with vehicle rental stations from one VehicleRentalDataSource.
 */
public class VehicleRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleRentalUpdater.class);

    private GraphUpdaterManager updaterManager;

    Map<String, VehicleRentalStationVertex> verticesByStation = new HashMap<>();

    Map<String, DisposableEdgeCollection> tempEdgesByStation = new HashMap<>();

    private final VehicleRentalDataSource source;

    private VertexLinker linker;

    private VehicleRentalStationService service;

    private final String network;

    public VehicleRentalUpdater(VehicleRentalUpdaterParameters parameters) throws IllegalArgumentException {
        super(parameters);
        // Configure updater
        LOG.info("Setting up vehicle rental updater.");

        VehicleRentalDataSource source = VehicleRentalDataSourceFactory.create(parameters.sourceParameters());

        this.source = source;
        this.network = parameters.getNetworks();
        if (pollingPeriodSeconds <= 0) {
            LOG.info("Creating vehicle-rental updater running once only (non-polling): {}", source);
        } else {
            LOG.info("Creating vehicle-rental updater running every {} seconds: {}", pollingPeriodSeconds, source);
        }
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Creation of network linker library will not modify the graph
        linker = graph.getLinker();
        // Adding a vehicle rental station service needs a graph writer runnable
        service = graph.getService(VehicleRentalStationService.class, true);
    }

    @Override
    protected void runPolling() {
        LOG.debug("Updating vehicle rental stations from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<VehicleRentalStation> stations = source.getStations();

        // Create graph writer runnable to apply these stations to the graph
        VehicleRentalGraphWriterRunnable graphWriterRunnable = new VehicleRentalGraphWriterRunnable(stations);
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class VehicleRentalGraphWriterRunnable implements GraphWriterRunnable {

        private final List<VehicleRentalStation> stations;

        public VehicleRentalGraphWriterRunnable(List<VehicleRentalStation> stations) {
            this.stations = stations;
        }

		@Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<String> stationSet = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Collections.singletonList(network));

            /* add any new stations and update vehicle counts for existing stations */
            for (VehicleRentalStation station : stations) {
                if (station.networks == null) {
                    /* API did not provide a network list, use default */
                    station.networks = defaultNetworks;
                }
                service.addVehicleRentalStation(station);
                stationSet.add(station.id);
                VehicleRentalStationVertex vehicleRentalVertex = verticesByStation.get(station.id);
                if (vehicleRentalVertex == null) {
                    vehicleRentalVertex = new VehicleRentalStationVertex(graph, station);
                    DisposableEdgeCollection tempEdges = linker.linkVertexForRealTime(
                        vehicleRentalVertex,
                        new TraverseModeSet(TraverseMode.WALK),
                        LinkingDirection.BOTH_WAYS,
                        (vertex, streetVertex) -> List.of(
                            new StreetVehicleRentalLink((VehicleRentalStationVertex) vertex, streetVertex),
                            new StreetVehicleRentalLink(streetVertex, (VehicleRentalStationVertex) vertex)
                        )
                    );
                    if (vehicleRentalVertex.getOutgoing().isEmpty()) {
                        // the toString includes the text "Bike rental station"
                        LOG.info("VehicleRentalStation {} is unlinked", vehicleRentalVertex);
                    }
                    tempEdges.addEdge(new VehicleRentalEdge(vehicleRentalVertex));
                    verticesByStation.put(station.id, vehicleRentalVertex);
                    tempEdgesByStation.put(station.id, tempEdges);
                } else {
                    vehicleRentalVertex.setVehiclesAvailable(station.vehiclesAvailable);
                    vehicleRentalVertex.setSpacesAvailable(station.spacesAvailable);
                }
            }
            /* remove existing stations that were not present in the update */
            List<String> toRemove = new ArrayList<>();
            for (Entry<String, VehicleRentalStationVertex> entry : verticesByStation.entrySet()) {
                String station = entry.getKey();
                if (stationSet.contains(station))
                    continue;
                toRemove.add(station);
                service.removeVehicleRentalStation(station);
            }
            for (String station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByStation.remove(station);
                tempEdgesByStation.get(station).disposeEdges();
                tempEdgesByStation.remove(station);
            }
        }
    }

}
