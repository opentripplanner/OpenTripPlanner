package org.opentripplanner.updater.car_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opentripplanner.graph_builder.linking.StreetSplitter;
import org.opentripplanner.routing.car_rental.CarRentalRegion;
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.car_rental.CarRentalStationService;
import org.opentripplanner.routing.edgetype.RentACarOffEdge;
import org.opentripplanner.routing.edgetype.RentACarOnEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.CarRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.DateUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.opentripplanner.graph_builder.linking.StreetSplitter.NON_DESTRUCTIVE_SPLIT;

public class CarRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(CarRentalUpdater.class);

    private static DecimalFormat format = new DecimalFormat("##.000000");

    private Graph graph;

    private StreetSplitter splitter;

    private String network;

    private CarRentalStationService service;

    private CarRentalDataSource source;

    private int timeToLiveMinutes;

    private GraphUpdaterManager updaterManager;

    Map<CarRentalStation, CarRentalStationVertex> verticesByStation = new HashMap<>();

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating car rental stations and regions from " + source);
        List<CarRentalRegion> regions = new ArrayList<>();
        List<CarRentalStation> stations = new ArrayList<>();
        if (source.updateStations()) {
            stations = source.getStations();
        } else {
            LOG.debug("No station updates");
        }

        if (source.updateRegions()) {
            regions = source.getRegions();
        } else {
            LOG.debug("No region updates");
        }

        // Create graph writer runnable to apply these stations and regions to the graph
        updaterManager.execute(new CarRentalGraphWriterRunnable(stations, regions));
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        network = config.path("sourceType").asText();
        CarRentalDataSource source = null;
        if (network != null) {
            if (network.equals("car2go")) {
                source = new Car2GoCarRentalDataSource();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown car rental source type: " + network);
        } else if (source instanceof JsonConfigurable) {
            ((JsonConfigurable) source).configure(graph, config);
        }

        // Configure updater
        LOG.info("Setting up car rental updater.");
        this.graph = graph;
        this.source = source;
        this.timeToLiveMinutes = config.path("timeToLiveMinutes").asInt(Integer.MAX_VALUE);
        if (pollingPeriodSeconds <= 0) {
            LOG.info("Creating car rental updater running once only (non-polling): {}", source);
        } else {
            LOG.info("Creating car rental updater running every {} seconds : {}", pollingPeriodSeconds, source);
        }
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        splitter = graph.streetIndex.getStreetSplitter();

        service = graph.getService(CarRentalStationService.class, true);
    }

    @Override
    public void teardown() {}

    private class CarRentalGraphWriterRunnable implements GraphWriterRunnable {
        private List<CarRentalStation> stations;
        private List<CarRentalRegion> regions;
        private GeometryFactory geometryFactory = new GeometryFactory();


        public CarRentalGraphWriterRunnable(List<CarRentalStation> stations, List<CarRentalRegion> regions) {
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
            Set<CarRentalStation> stationSet = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Arrays.asList(network));
            LOG.info("Updating {} rental car stations for network {}.", stations.size(), network);
            /* add any new stations that have fresh-enough data and update car counts for existing stations */
            for (CarRentalStation station : stations) {
                if (!DateUtils.withinTimeToLive(station.lastReportedEpochSeconds, timeToLiveMinutes)) {
                    // skip station as it does not have fresh-enough data
                    continue;
                }
                service.addCarRentalStation(station);
                stationSet.add(station);
                CarRentalStationVertex vertex = verticesByStation.get(station);
                if (vertex == null) {
                    makeVertex(graph, station);
                } else if (vertex.hasDifferentApproximatePosition(station)) {
                    LOG.info("Rental car {} has changed position, re-graphing", station);

                    // First remove the old vertices and edges
                    splitter.removeRentalStationVertexAndAssociatedSemiPermanentVerticesAndEdges(vertex);

                    // then make a new vertices and edges
                    makeVertex(graph, station);
                } else {
                    vertex.setCarsAvailable(station.carsAvailable);
                    vertex.setSpacesAvailable(station.spacesAvailable);
                }
            }
            // Remove existing stations that were not present in the update
            List<CarRentalStation> toRemove = new ArrayList<>();
            for (Entry<CarRentalStation, CarRentalStationVertex> entry : verticesByStation.entrySet()) {
                CarRentalStation station = entry.getKey();
                if (stationSet.contains(station))
                    continue;

                splitter.removeRentalStationVertexAndAssociatedSemiPermanentVerticesAndEdges(entry.getValue());

                toRemove.add(station);
                service.removeCarRentalStation(station);
            }
            for (CarRentalStation station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByStation.remove(station);
            }
        }

        private void makeVertex(Graph graph, CarRentalStation station) {
            CarRentalStationVertex vertex = new CarRentalStationVertex(graph, station);
            if (!splitter.linkToClosestWalkableEdge(vertex, NON_DESTRUCTIVE_SPLIT, true)) {
                // the toString includes the text "Car rental station"
                LOG.warn("{} not near any streets; it will not be usable.", station);
            }
            verticesByStation.put(station, vertex);
            if (station.allowPickup)
                new RentACarOnEdge(vertex, station);
            if (station.allowDropoff)
                new RentACarOffEdge(vertex, station);
        }

        public void applyRegions(Graph graph) {
            // Adding car service regions to all edges of the network.
            LOG.info("Applying {} rental car regions.", regions.size());
            Collection<StreetEdge> edges = graph.getStreetEdges();
            Map<Coordinate, Set<String>> coordToNetworksMap = new HashMap<>();
            for (CarRentalRegion region : regions) {
                LOG.info("\t{}", region.network);
                service.addCarRentalRegion(region);
                Set<Coordinate> coordinates = intersectWithGraph(edges, region);

                coordinates.forEach(c -> coordToNetworksMap.putIfAbsent(c, new HashSet<>()));
                coordinates.forEach(c -> coordToNetworksMap.get(c).add(region.network));

            }
            addDropOffsToGraph(coordToNetworksMap);
            LOG.info("Added in total {} border dropoffs.", coordToNetworksMap.size());
            LOG.info("Finished applying service regions.");
        }

        /**
         * Labels edges that are inside the region with the car network name. For edges that are partially
         * inside the region, computes the intersection points and returns them.
         * Skips edges that are outside of the region.
         * @param edges
         * @param region The intersection locations
         */
        private Set<Coordinate> intersectWithGraph(Collection<StreetEdge> edges, CarRentalRegion region) {
            Set<Coordinate> coordinates = new HashSet<>();

            // use a prepared geometry to dramatically speed up "covers" operations
            PreparedGeometry preparedRegionGeometry = PreparedGeometryFactory.prepare(region.geometry);

            for (StreetEdge edge : edges) {
                Point[] edgePoints = getEdgeCoord(edge);
                boolean coversFrom = preparedRegionGeometry.covers(edgePoints[0]);
                boolean coversTo = preparedRegionGeometry.covers(edgePoints[1]);
                if (coversFrom && coversTo) {
                    edge.addCarNetwork(region.network);
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
         * @param region the car service area
         * @return intersection coordinates
         */
        private Set<Coordinate> intersect(Point[] edgePoints, CarRentalRegion region) {
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
                        // The reason is that we will create car drop off stations for those intersection points,
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
         * Adds a car dropoff station at each given (location, networks) pairs.
         */
        private void addDropOffsToGraph(Map<Coordinate, Set<String>> coordToNetworksMap) {
            coordToNetworksMap.forEach((coord, networks) -> {
                CarRentalStation station = makeDropOffStation(coord, networks);
                CarRentalStationVertex vertex = new CarRentalStationVertex(graph, station);
                if (!splitter.linkToClosestWalkableEdge(vertex, NON_DESTRUCTIVE_SPLIT, true)) {
                    LOG.warn("Ignoring {} since it's not near any streets; it will not be usable.", station);
                    return;
                }
                service.addCarRentalStation(station);
                new RentACarOffEdge(vertex, station);
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

        private CarRentalStation makeDropOffStation(Coordinate coord, Set<String> networks) {
            CarRentalStation station = new CarRentalStation();
            String networkNames = (networks == null || networks.size() == 0) ? "undefined network" : networks.toString();
            String id = String.format("border_dropoff_%3.6f_%3.6f_%s", coord.x, coord.y, networkNames);

            station.address = id;
            station.allowDropoff = true;
            station.allowPickup = false;
            station.carsAvailable = 0;
            station.id = id;
            station.isBorderDropoff = true;
            station.name = new NonLocalizedString(id);
            station.networks = networks;
            station.spacesAvailable = Integer.MAX_VALUE;
            station.x = coord.x;
            station.y = coord.y;
            return station;
        }
    }
}
