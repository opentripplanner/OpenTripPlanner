package org.opentripplanner.updater.bike_rental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic bike-rental station updater which updates the Graph with bike rental stations from one BikeRentalDataSource.
 */
public class BikeRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater.class);

    private GraphUpdaterManager updaterManager;

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<BikeRentalStation, BikeRentalStationVertex>();

    private BikeRentalDataSource source;

    private SimpleStreetSplitter linker;

    private BikeRentalStationService service;

    private String network = "default";

    public BikeRentalUpdater(BikeRentalUpdaterConfig config) throws Exception {
        super(config);

        // Set data source type from config JSON
        String sourceType = config.getSource().getName();
        String apiKey = config.getApiKey();
        // Each updater can be assigned a unique network ID in the configuration to prevent returning bikes at
        // stations for another network. TODO shouldn't we give each updater a unique network ID by default?
        String networkName = config.getNetwork();
        BikeRentalDataSource source = null;
        if (sourceType != null) {
            if (sourceType.equals("jcdecaux")) {
                source = new JCDecauxBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("b-cycle")) {
                source = new BCycleBikeRentalDataSource(config.getSource(), apiKey, networkName);
            } else if (sourceType.equals("bixi")) {
                source = new BixiBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("keolis-rennes")) {
                source = new KeolisRennesBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("ov-fiets")) {
                source = new OVFietsKMLDataSource(config.getSource());
            } else if (sourceType.equals("city-bikes")) {
                source = new CityBikesBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("vcub")) {
                source = new VCubDataSource(config.getSource());
            } else if (sourceType.equals("citi-bike-nyc")) {
                source = new CitiBikeNycBikeRentalDataSource(config.getSource(), networkName);
            } else if (sourceType.equals("next-bike")) {
                source = new NextBikeRentalDataSource(config.getSource(), networkName);
            } else if (sourceType.equals("kml")) {
                source = new GenericKmlBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("sf-bay-area")) {
                source = new SanFranciscoBayAreaBikeRentalDataSource(config.getSource(), networkName);
            } else if (sourceType.equals("share-bike")) {
                source = new ShareBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("uip-bike")) {
                source = new UIPBikeRentalDataSource(config.getSource(), apiKey);
            } else if (sourceType.equals("gbfs")) {
                source = new GbfsBikeRentalDataSource(config.getSource(), networkName);
            } else if (sourceType.equals("smoove")) {
                source = new SmooveBikeRentalDataSource(config.getSource());
            } else if (sourceType.equals("bicimad")) {
                source = new BicimadBikeRentalDataSource(config.getSource());
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown bike rental source type: " + sourceType);
        }

        // Configure updater
        LOG.info("Setting up bike rental updater.");
        this.source = source;
        this.network = config.getNetworks();
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
    public void setup(Graph graph) throws InterruptedException, ExecutionException {
        // Creation of network linker library will not modify the graph
        linker = new SimpleStreetSplitter(graph, new DataImportIssueStore(false));
        // Adding a bike rental station service needs a graph writer runnable
        service = graph.getService(BikeRentalStationService.class, true);
    }

    @Override
    protected void runPolling() throws Exception {
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

        private List<BikeRentalStation> stations;

        public BikeRentalGraphWriterRunnable(List<BikeRentalStation> stations) {
            this.stations = stations;
        }

		@Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<BikeRentalStation> stationSet = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Arrays.asList(network));
            /* add any new stations and update bike counts for existing stations */
            for (BikeRentalStation station : stations) {
                if (station.networks == null) {
                    /* API did not provide a network list, use default */
                    station.networks = defaultNetworks;
                }
                service.addBikeRentalStation(station);
                stationSet.add(station);
                BikeRentalStationVertex vertex = verticesByStation.get(station);
                if (vertex == null) {
                    vertex = new BikeRentalStationVertex(graph, station);
                    if (!linker.link(vertex)) {
                        // the toString includes the text "Bike rental station"
                        LOG.info("BikeRentalStation {} is unlinked", vertex);
                    }
                    verticesByStation.put(station, vertex);
                    new RentABikeOnEdge(vertex, vertex, station.networks);
                    if (station.allowDropoff)
                        new RentABikeOffEdge(vertex, vertex, station.networks);
                } else {
                    vertex.setBikesAvailable(station.bikesAvailable);
                    vertex.setSpacesAvailable(station.spacesAvailable);
                }
            }
            /* remove existing stations that were not present in the update */
            List<BikeRentalStation> toRemove = new ArrayList<BikeRentalStation>();
            for (Entry<BikeRentalStation, BikeRentalStationVertex> entry : verticesByStation.entrySet()) {
                BikeRentalStation station = entry.getKey();
                if (stationSet.contains(station))
                    continue;
                BikeRentalStationVertex vertex = entry.getValue();
                if (graph.containsVertex(vertex)) {
                    graph.removeVertexAndEdges(vertex);
                }
                toRemove.add(station);
                service.removeBikeRentalStation(station);
                // TODO: need to unsplit any streets that were split
            }
            for (BikeRentalStation station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByStation.remove(station);
            }
        }
    }

    public interface BikeRentalUpdaterConfig extends PollingGraphUpdaterConfig {
        String getNetwork();
        String getNetworks();
        String getApiKey();
    }
}
