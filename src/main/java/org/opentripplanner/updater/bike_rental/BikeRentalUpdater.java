package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.linking.StreetSplitter;
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
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.opentripplanner.graph_builder.linking.StreetSplitter.NON_DESTRUCTIVE_SPLIT;

/**
 * Dynamic bike-rental station updater which updates the Graph with bike rental stations from one BikeRentalDataSource.
 */
public class BikeRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater.class);

    private GraphUpdaterManager updaterManager;

    private static final String DEFAULT_NETWORK_LIST = "default";

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<>();

    private BikeRentalDataSource source;

    private Graph graph;

    private StreetSplitter splitter;

    private BikeRentalStationService service;

    private String network = "default";

    private int timeToLiveMinutes;

    private String networkName = "default";

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling (Graph graph, JsonNode config) throws Exception {

        // Set data source type from config JSON
        String sourceType = config.path("sourceType").asText();
        String apiKey = config.path("apiKey").asText();
        // Each updater can be assigned a unique network ID in the configuration to prevent returning bikes at
        // stations for another network. TODO shouldn't we give each updater a unique network ID by default?
        networkName = config.path("network").asText();
        BikeRentalDataSource source = null;
        if (sourceType != null) {
            if (sourceType.equals("jcdecaux")) {
                source = new JCDecauxBikeRentalDataSource();
            } else if (sourceType.equals("b-cycle")) {
                source = new BCycleBikeRentalDataSource(apiKey, networkName);
            } else if (sourceType.equals("bixi")) {
                source = new BixiBikeRentalDataSource();
            } else if (sourceType.equals("keolis-rennes")) {
                source = new KeolisRennesBikeRentalDataSource();
            } else if (sourceType.equals("ov-fiets")) {
                source = new OVFietsKMLDataSource();
            } else if (sourceType.equals("city-bikes")) {
                source = new CityBikesBikeRentalDataSource();
            } else if (sourceType.equals("vcub")) {
                source = new VCubDataSource();
            } else if (sourceType.equals("citi-bike-nyc")) {
                source = new CitiBikeNycBikeRentalDataSource(networkName);
            } else if (sourceType.equals("next-bike")) {
                source = new NextBikeRentalDataSource(networkName);
            } else if (sourceType.equals("kml")) {
                source = new GenericKmlBikeRentalDataSource();
            } else if (sourceType.equals("sf-bay-area")) {
                source = new SanFranciscoBayAreaBikeRentalDataSource(networkName);
            } else if (sourceType.equals("share-bike")) {
                source = new ShareBikeRentalDataSource();
            } else if (sourceType.equals("uip-bike")) {
                source = new UIPBikeRentalDataSource(apiKey);
            } else if (sourceType.equals("gbfs")) {
                source = new GbfsBikeRentalDataSource(networkName);
            } else if (sourceType.equals("smoove")) {
                source = new SmooveBikeRentalDataSource();
            } else if (sourceType.equals("bicimad")) {
                source = new BicimadBikeRentalDataSource();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown bike rental source type: " + sourceType);
        } else if (source instanceof JsonConfigurable) {
            ((JsonConfigurable) source).configure(graph, config);
        }

        // Configure updater
        LOG.info("Setting up bike rental updater.");
        this.source = source;
        this.network = config.path("networks").asText(DEFAULT_NETWORK_LIST);
        this.timeToLiveMinutes = config.path("timeToLiveMinutes").asInt(Integer.MAX_VALUE);
        if (pollingPeriodSeconds <= 0) {
            LOG.info("Creating bike-rental updater running once only (non-polling): {}", source);
        } else {
            LOG.info("Creating bike-rental updater running every {} seconds: {}", pollingPeriodSeconds, source);
        }

    }

    @Override
    public void setup(Graph graph) throws InterruptedException, ExecutionException {
        splitter = graph.streetIndex.getStreetSplitter();

        // Adding a bike rental station service needs a graph writer runnable
        service = graph.getService(BikeRentalStationService.class, true);
    }

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating bike rental stations from " + source);
        source.update();

        updaterManager.execute(new BikeRentalGraphWriterRunnable(source));
    }

    @Override
    public void teardown() {
    }

    private class BikeRentalGraphWriterRunnable implements GraphWriterRunnable {
        private final List<RentalUpdaterError> errors;
        private final List<BikeRentalStation> stations;
        private final SystemInformation.SystemInformationData systemInformationData;

        public BikeRentalGraphWriterRunnable(BikeRentalDataSource source) {
            errors = source.getErrors();
            stations = source.getStations();
            systemInformationData = null;
        }

		@Override
        public void run(Graph graph) {
            service.setErrorsForNetwork(networkName, errors);
            service.setSystemInformationDataForNetwork(networkName, systemInformationData);

            // check if any critical errors occurred
            boolean feedWideError = false;
            boolean allStationsError = false;
            boolean allFloatingVehiclesError = false;
            for (RentalUpdaterError error : errors) {
                switch (error.severity) {
                case FEED_WIDE:
                    feedWideError = true;
                    break;
                case ALL_STATIONS:
                    allStationsError = true;
                    break;
                case ALL_FLOATING_VEHICLES:
                    allFloatingVehiclesError = true;
                    break;
                }
            }

            // Apply stations to graph
            Set<BikeRentalStation> toRemove = new HashSet<>();
            Set<BikeRentalStation> stationsInUpdate = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Arrays.asList(network));
            LOG.info("Updating rental bike stations for network.", network);

            // Apply stations to graph if a feed-wide error did not occur
            if (!feedWideError) {
                // add any new stations that have fresh-enough data and update bike counts for existing stations
                for (BikeRentalStation station : stations) {
                    if (!DateUtils.withinTimeToLive(station.lastReportedEpochSeconds, timeToLiveMinutes)) {
                        // skip station as it does not have fresh-enough data
                        continue;
                    }
                    if (station.networks == null) {
                        // API did not provide a network list, use default
                        station.networks = defaultNetworks;
                    }
                    service.addBikeRentalStation(station);
                    stationsInUpdate.add(station);
                    BikeRentalStationVertex vertex = verticesByStation.get(station);
                    if (vertex == null) {
                        vertex = new BikeRentalStationVertex(graph, station);
                        if (!splitter.linkToClosestWalkableEdge(vertex, NON_DESTRUCTIVE_SPLIT, true)) {
                            // the toString includes the text "Bike rental station"
                            LOG.warn("{} not near any streets; it will not be usable.", station);
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
            }

            // remove existing stations that were not present in the update
            for (Entry<BikeRentalStation, BikeRentalStationVertex> entry : verticesByStation.entrySet()) {
                BikeRentalStation station = entry.getKey();
                if (stationsInUpdate.contains(station)) {
                    // station present in update, do not remove
                    continue;
                }

                // if there was an error with fetching stations, do not remove any stations that had a last reported
                // time within the time to live threshold
                if (
                    allStationsError &&
                        !station.isFloatingBike &&
                        DateUtils.withinTimeToLive(station.lastReportedEpochSeconds, timeToLiveMinutes)
                ) {
                    continue;
                }

                // if there was an error with fetching floating bikes, do not remove any stations that had a last
                // reported time within the time to live threshold
                if (
                    allFloatingVehiclesError &&
                        station.isFloatingBike &&
                        DateUtils.withinTimeToLive(station.lastReportedEpochSeconds, timeToLiveMinutes)
                ) {
                    continue;
                }

                splitter.removeRentalStationVertexAndAssociatedSemiPermanentVerticesAndEdges(entry.getValue());

                // first get the outgoing
                toRemove.add(station);
                service.removeBikeRentalStation(station);
            }

            for (BikeRentalStation station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByStation.remove(station);
            }
        }
    }
}
