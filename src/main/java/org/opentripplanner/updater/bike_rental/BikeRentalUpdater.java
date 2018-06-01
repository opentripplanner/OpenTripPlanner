/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.*;
import org.opentripplanner.graph_builder.linking.StreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalRegion;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.opentripplanner.graph_builder.linking.SimpleStreetSplitter.DESTRUCTIVE_SPLIT;

/**
 * Dynamic bike-rental station updater which updates the Graph with bike rental stations from one BikeRentalDataSource.
 */
public class BikeRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater.class);

    private static DecimalFormat format = new DecimalFormat("##.000000");

    private GraphUpdaterManager updaterManager;

    private static final String DEFAULT_NETWORK_LIST = "default";

    Map<BikeRentalStation, BikeRentalStationVertex> verticesByStation = new HashMap<>();

    private BikeRentalDataSource source;

    private Graph graph;

    private StreetSplitter splitter;

    private BikeRentalStationService service;

    private String network = "default";

    // processedRegions is a set of bike network names. It keeps track of regions that are already
    // apply to the graph, so we don't apply them again.
    private Set<String> processedRegions = new HashSet<>();

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling (Graph graph, JsonNode config) throws Exception {

        // Set data source type from config JSON
        String sourceType = config.path("sourceType").asText();
        String apiKey = config.path("apiKey").asText();
        String networkName = config.path("network").asText();
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
                source = new GbfsBikeRentalDataSource();
            } else if (sourceType.equals("coord")) {
                source = new CoordBikeRentalDataSource();
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
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<BikeRentalStation> stations = source.getStations();
        List<BikeRentalRegion> regions = source.getRegions();

        // Filter out the regions that are already applied.
        regions = regions.stream()
                .filter(c -> !processedRegions.contains(c.network))
                .collect(Collectors.toList());

        // Create graph writer runnable to apply these stations and regions to the graph
        updaterManager.execute(new BikeRentalGraphWriterRunnable(stations, regions));

        if (!regions.isEmpty()) {
            regions.forEach(r -> processedRegions.add(r.network));
        }
    }

    @Override
    public void teardown() {
    }

    private class BikeRentalGraphWriterRunnable implements GraphWriterRunnable {

        private List<BikeRentalStation> stations;
        private List<BikeRentalRegion> regions;
        private GeometryFactory geometryFactory = new GeometryFactory();


        public BikeRentalGraphWriterRunnable(List<BikeRentalStation> stations, List<BikeRentalRegion> regions) {
            this.stations = stations;
            this.regions = regions;
        }

		@Override
        public void run(Graph graph) {
            if (!this.stations.isEmpty()) {
                applyStations(graph);
            }
            if (!this.regions.isEmpty()) {
                applyRegions(graph);
            }
        }

        private void applyStations(Graph graph) {
            // Apply stations to graph
            Set<BikeRentalStation> stationSet = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Arrays.asList(network));
            LOG.info("Updating {} rental bike stations.", stations.size());
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
                    if (!splitter.linkToClosestWalkableEdge(vertex, DESTRUCTIVE_SPLIT)) {
                        // the toString includes the text "Bike rental station"
                        LOG.warn("Ignoring {} since it's not near any streets; it will not be usable.", station);
                    }
                    verticesByStation.put(station, vertex);
                    if (station.allowPickup)
                        new RentABikeOnEdge(vertex, vertex, station.networks);
                    if (station.allowDropoff)
                        new RentABikeOffEdge(vertex, vertex, station.networks);
                } else if (station.x != vertex.getX() || station.y != vertex.getY()) {
                    LOG.info("{} has changed, re-graphing", station);

                    // First remove the old one.
                    if (graph.containsVertex(vertex)) {
                        graph.removeVertexAndEdges(vertex);
                    }
                    // Next, create a new vertex.
                    vertex = new BikeRentalStationVertex(graph, station);
                    if (!splitter.linkToClosestWalkableEdge(vertex, DESTRUCTIVE_SPLIT)) {
                        // the toString includes the text "Bike rental station"
                        LOG.warn("Ignoring {} since it's not near any streets; it will not be usable.", station);
                    }
                    verticesByStation.put(station, vertex);
                    if (station.allowPickup)
                        new RentABikeOnEdge(vertex, vertex, station.networks);
                    if (station.allowDropoff)
                        new RentABikeOffEdge(vertex, vertex, station.networks);
                } else {
                    // Update the station metadata.
                    vertex.setBikesAvailable(station.bikesAvailable);
                    vertex.setSpacesAvailable(station.spacesAvailable);
                    vertex.setPickupAllowed(station.allowPickup);
                }
            }
            // Remove existing stations that were not present in the update
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
        public void applyRegions(Graph graph) {
            // Adding bike service regions to all edges of the network.
            LOG.info("Applying {} rental bike regions.", regions.size());
            Collection<StreetEdge> edges = graph.getStreetEdges();
            Map<Coordinate, Set<String>> coordToNetworksMap = new HashMap<>();
            for (BikeRentalRegion region : regions) {
                LOG.info("\t{}", region.network);
                service.addBikeRentalRegion(region);
                Set<Coordinate> coordinates = intersectWithGraph(edges, region);

                coordinates.forEach(c -> coordToNetworksMap.putIfAbsent(c, new HashSet<>()));
                coordinates.forEach(c -> coordToNetworksMap.get(c).add(region.network));

            }
            addDropOffsToGraph(coordToNetworksMap);
            LOG.info("Added in total {} border dropoffs.", coordToNetworksMap.size());
            LOG.info("Finished applying service regions.");
        }

        /**
         * Labels edges that are inside the region with the bike network name. For edges that are partially
         * inside the region, computes the intersection points and returns them.
         * Skips edges that are outside of the region.
         * @param edges
         * @param region The intersection locations
         */
        private Set<Coordinate> intersectWithGraph(Collection<StreetEdge> edges, BikeRentalRegion region) {
            Set<Coordinate> coordinates = new HashSet<>();
            for (StreetEdge edge : edges) {
                Point[] edgePoints = getEdgeCoord(edge);
                boolean coversFrom = region.geometry.covers(edgePoints[0]);
                boolean coversTo = region.geometry.covers(edgePoints[1]);
                if (coversFrom && coversTo) {
                    edge.addBikeNetwork(region.network);
                } else if (coversFrom || coversTo) {
                    coordinates.addAll(intersect(edgePoints, region));
                }
            }
            return coordinates;
        }

        /**
         * Finds the intersection points of the edge and the region.
         *
         * @param edgePoints an array of two points representing an edge
         * @param region the bike service area
         * @return intersection coordinates
         */
        private Set<Coordinate> intersect(Point[] edgePoints, BikeRentalRegion region) {
            // Intersect the edge with the region's geometry.
            Point from = edgePoints[0];
            Point to = edgePoints[1];
            LineString lineString = geometryFactory.createLineString(new Coordinate[]{from.getCoordinate(), to.getCoordinate()});
            Geometry intersectionLineStrings = region.geometry.intersection(lineString);
            // intersection results in one or more linestrings representing the parts of the edge that overlaps
            // the polygon.
            Set<Coordinate> intersectionPoints = new HashSet<>();
            for (int i = 0; i < intersectionLineStrings.getNumGeometries(); i++) {
                Geometry intersectionLineString = intersectionLineStrings.getGeometryN(i);
                for (Coordinate coordinate : intersectionLineString.getCoordinates()) {
                    if (coordinate.equals(equals(from.getCoordinate())) ||
                            coordinate.equals(equals(to.getCoordinate()))){
                        // Skip the 'from' and 'to' nodes of the edge and only return the intersection points.
                        // The reason is that we will create bike drop off stations for those intersection points,
                        // and not for 'from' and 'to' nodes.
                        continue;
                    }
                    intersectionPoints.add(roundLatLng(coordinate));
                }
            }
            return intersectionPoints;
        }

        private Coordinate roundLatLng(Coordinate coordinate) {
            return new Coordinate(Double.parseDouble(format.format(coordinate.x)), Double.parseDouble(format.format(coordinate.y)), 0);
        }

        /**
         * Adds a bike dropoff station at each given (location, networks) pairs.
         */
        private void addDropOffsToGraph(Map<Coordinate, Set<String>> coordToNetworksMap) {
            coordToNetworksMap.forEach((coord, networks) -> {
                BikeRentalStation station = makeDropOffStation(coord, networks);
                BikeRentalStationVertex vertex = new BikeRentalStationVertex(graph, station);
                if (!splitter.linkToClosestWalkableEdge(vertex, DESTRUCTIVE_SPLIT)) {
                    LOG.warn("Ignoring {} since it's not near any streets; it will not be usable.", station);
                    return;
                }
                service.addBikeRentalStation(station);
                new RentABikeOffEdge(vertex, vertex, networks);
            });
        }

        /**
         * @param edge an Edge
         * @return an array of size 2 of Point, with the first and the second items representing the edge's
         *         `from` and `to` locations respectively.
         */
        private Point[] getEdgeCoord(final Edge edge) {
            Point fromPoint = geometryFactory.createPoint(new Coordinate(edge.getFromVertex().getX(), edge.getFromVertex().getY(), 0));
            Point toPoint = geometryFactory.createPoint(new Coordinate(edge.getToVertex().getX(), edge.getToVertex().getY(), 0));
            return new Point[]{fromPoint, toPoint};
        }

        private BikeRentalStation makeDropOffStation(Coordinate coord, Set<String> networks) {
            BikeRentalStation station = new BikeRentalStation();
            station.id = String.format("border_dropoff_%3.6f_%3.6f", coord.x, coord.y);
            station.name = new NonLocalizedString(station.id);
            station.x = coord.x;
            station.y = coord.y;
            station.allowDropoff = true;
            station.bikesAvailable = 0;
            station.spacesAvailable = Integer.MAX_VALUE;
            station.allowPickup = false;
            station.networks = networks;
            return station;
        }
    }
}
