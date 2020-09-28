package org.opentripplanner.updater.bike_rental;

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
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterDataSourceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

import static org.opentripplanner.standalone.config.DefaultUpdaterDataSourceConfig.*;

/**
 * Dynamic bike-rental station updater which updates the Graph with bike rental stations from one BikeRentalDataSource.
 */
public class BikeRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater.class);

    private GraphUpdaterManager updaterManager;

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<>();

    private final BikeRentalDataSource source;

    private SimpleStreetSplitter linker;

    private BikeRentalStationService service;

    private final String network;

    public BikeRentalUpdater(Parameters parameters) throws IllegalArgumentException {
        super(parameters);

        // Set data source type from config JSON
        String sourceType = parameters.getSourceConfig().getType();
        String apiKey = parameters.getApiKey();
        // Each updater can be assigned a unique network ID in the configuration to prevent returning bikes at
        // stations for another network. TODO shouldn't we give each updater a unique network ID by default?
        String networkName = parameters.getNetwork();
        UpdaterDataSourceParameters sourceParameters = parameters.getSourceConfig().getUpdaterSourceParameters();
        BikeRentalDataSource source = null;
        if (sourceType != null) {
            switch (sourceType) {
                case JCDECAUX:
                    source = new JCDecauxBikeRentalDataSource(sourceParameters);
                    break;
                case B_CYCLE:
                    source = new BCycleBikeRentalDataSource(
                        sourceParameters,
                        apiKey,
                        networkName
                    );
                    break;
                case BIXI:
                    source = new BixiBikeRentalDataSource(sourceParameters);
                    break;
                case KEOLIS_RENNES:
                    source = new KeolisRennesBikeRentalDataSource(sourceParameters);
                    break;
                case OV_FIETS:
                    source = new OVFietsKMLDataSource(sourceParameters);
                    break;
                case CITY_BIKES:
                    source = new CityBikesBikeRentalDataSource(sourceParameters);
                    break;
                case VCUV:
                    source = new VCubDataSource(sourceParameters);
                    break;
                case CITI_BIKE_NYC:
                    source = new CitiBikeNycBikeRentalDataSource(sourceParameters, networkName);
                    break;
                case NEXT_BIKE:
                    source = new NextBikeRentalDataSource(sourceParameters, networkName);
                    break;
                case KML:
                    source = new GenericKmlBikeRentalDataSource((GenericKmlBikeRentalDataSource.Parameters) sourceParameters);
                    break;
                case SF_BAY_AREA:
                    source = new SanFranciscoBayAreaBikeRentalDataSource(
                        sourceParameters,
                        networkName
                    );
                    break;
                case SHARE_BIKE:
                    source = new ShareBikeRentalDataSource(sourceParameters);
                    break;
                case UIP_BIKE:
                    source = new UIPBikeRentalDataSource(sourceParameters, apiKey);
                    break;
                case GBFS:
                    source = new GbfsBikeRentalDataSource((GbfsBikeRentalDataSource.Parameters) sourceParameters, networkName);
                    break;
                case SMOOVE:
                    source = new SmooveBikeRentalDataSource(sourceParameters);
                    break;
                case BICIMAD:
                    source = new BicimadBikeRentalDataSource(sourceParameters);
                    break;
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown bike rental source type: " + sourceType);
        }

        // Configure updater
        LOG.info("Setting up bike rental updater.");
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
        linker = new SimpleStreetSplitter(graph, new DataImportIssueStore(false));
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
            List<BikeRentalStation> toRemove = new ArrayList<>();
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

    public interface Parameters extends PollingGraphUpdaterParameters {
        String getNetwork();
        String getNetworks();
        String getApiKey();

        /** The config name/type for the updater. Used to reference the configuration element. */
        String getConfigRef();
    }
}
