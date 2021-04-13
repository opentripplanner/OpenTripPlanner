package org.opentripplanner.updater.bike_rental;

import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.bike_rental.datasources.BikeRentalDataSourceFactory;
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
 * Dynamic bike-rental station updater which updates the Graph with bike rental stations from one BikeRentalDataSource.
 */
public class BikeRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater.class);

    private GraphUpdaterManager updaterManager;

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<>();

    Map<BikeRentalStation, DisposableEdgeCollection> tempEdgesByStation = new HashMap<>();

    private final BikeRentalDataSource source;

    private VertexLinker linker;

    private BikeRentalStationService service;

    private final String network;

    public BikeRentalUpdater(BikeRentalUpdaterParameters parameters) throws IllegalArgumentException {
        super(parameters);
        // Configure updater
        LOG.info("Setting up bike rental updater.");

        BikeRentalDataSource source = BikeRentalDataSourceFactory.create(parameters.sourceParameters());

        this.source = source;
        this.network = parameters.getNetworks();
        if (pollingPeriodSeconds <= 0) {
            LOG.info("Creating bike-rental updater running once only (non-polling): {}", source);
        } else {
            LOG.info("Creating bike-rental updater running every {} seconds: {}", pollingPeriodSeconds, source);
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
        // Adding a bike rental station service needs a graph writer runnable
        service = graph.getService(BikeRentalStationService.class, true);
    }

    @Override
    protected void runPolling() {
        LOG.debug("Updating bike rental stations from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<BikeRentalStation> stations = source.getStations();

        // Create graph writer runnable to apply these stations to the graph
        BikeRentalGraphWriterRunnable graphWriterRunnable = new BikeRentalGraphWriterRunnable(stations);
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class BikeRentalGraphWriterRunnable implements GraphWriterRunnable {

        private final List<BikeRentalStation> stations;

        public BikeRentalGraphWriterRunnable(List<BikeRentalStation> stations) {
            this.stations = stations;
        }

		@Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<BikeRentalStation> stationSet = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Collections.singletonList(network));

            /* add any new stations and update bike counts for existing stations */
            for (BikeRentalStation station : stations) {
                if (station.networks == null) {
                    /* API did not provide a network list, use default */
                    station.networks = defaultNetworks;
                }
                service.addBikeRentalStation(station);
                stationSet.add(station);
                BikeRentalStationVertex bikeRentalVertex = verticesByStation.get(station);
                if (bikeRentalVertex == null) {
                    bikeRentalVertex = new BikeRentalStationVertex(graph, station);
                    DisposableEdgeCollection tempEdges = linker.linkVertexForRealTime(
                        bikeRentalVertex,
                        new TraverseModeSet(TraverseMode.WALK),
                        LinkingDirection.BOTH_WAYS,
                        (vertex, streetVertex) -> List.of(
                            new StreetBikeRentalLink((BikeRentalStationVertex) vertex, streetVertex),
                            new StreetBikeRentalLink(streetVertex, (BikeRentalStationVertex) vertex)
                        )
                    );
                    if (bikeRentalVertex.getOutgoing().isEmpty()) {
                        // the toString includes the text "Bike rental station"
                        LOG.info("BikeRentalStation {} is unlinked", bikeRentalVertex);
                    }
                    tempEdges.addEdge(new RentABikeOnEdge(bikeRentalVertex, bikeRentalVertex, station.networks));
                    if (station.allowDropoff) {
                        tempEdges.addEdge(new RentABikeOffEdge(bikeRentalVertex,
                            bikeRentalVertex,
                            station.networks
                        ));
                    }
                    verticesByStation.put(station, bikeRentalVertex);
                    tempEdgesByStation.put(station, tempEdges);
                } else {
                    bikeRentalVertex.setBikesAvailable(station.bikesAvailable);
                    bikeRentalVertex.setSpacesAvailable(station.spacesAvailable);
                }
            }
            /* remove existing stations that were not present in the update */
            List<BikeRentalStation> toRemove = new ArrayList<>();
            for (Entry<BikeRentalStation, BikeRentalStationVertex> entry : verticesByStation.entrySet()) {
                BikeRentalStation station = entry.getKey();
                if (stationSet.contains(station))
                    continue;
                toRemove.add(station);
                service.removeBikeRentalStation(station);
            }
            for (BikeRentalStation station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByStation.remove(station);
                tempEdgesByStation.get(station).disposeEdges();
                tempEdgesByStation.remove(station);
            }
        }
    }

}
