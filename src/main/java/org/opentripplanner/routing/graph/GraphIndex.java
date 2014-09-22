package org.opentripplanner.routing.graph;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.resource.SimpleIsochrone;
import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.profile.ProfileTransfer;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.profile.StopNameNormalizer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.TablePatternEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class contains all the transient indexes of graph elements -- those that are not
 * serialized with the graph. Caching these maps is essentially an optimization, but a big one.
 * The index is bootstrapped from the graph's list of edges.
 */
public class GraphIndex {

    private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);
    private static final int CLUSTER_RADIUS = 400; // meters

    // TODO: consistently key on model object or id string
    public final Map<String, Vertex> vertexForId = Maps.newHashMap();
    public final Map<String, Agency> agencyForId = Maps.newHashMap();
    public final Map<AgencyAndId, Stop> stopForId = Maps.newHashMap();
    public final Map<AgencyAndId, Trip> tripForId = Maps.newHashMap();
    public final Map<AgencyAndId, Route> routeForId = Maps.newHashMap();
    public final Map<AgencyAndId, String> serviceForId = Maps.newHashMap();
    public final Map<String, TripPattern> patternForId = Maps.newHashMap();
    public final Map<Stop, TransitStop> stopVertexForStop = Maps.newHashMap();
    public final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
    public final Multimap<Agency, TripPattern> patternsForAgency = ArrayListMultimap.create();
    public final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    public final Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
    public final Multimap<String, Stop> stopsForParentStation = ArrayListMultimap.create();
    public final HashGrid<TransitStop> stopSpatialIndex = new HashGrid<TransitStop>();
    public final Map<Stop, StopCluster> stopClusterForStop = Maps.newHashMap();
    public final Map<String, StopCluster> stopClusterForId = Maps.newHashMap();

    /* Should eventually be replaced with new serviceId indexes. */
    private final CalendarService calendarService;
    private final Map<AgencyAndId,Integer> serviceCodes;

    /* Full-text search extensions */
    public LuceneIndex luceneIndex;

    /* Separate transfers for profile routing */
    public Multimap<StopCluster, ProfileTransfer> transfersFromStopCluster;
    public HashGrid<StopCluster> stopClusterSpatialIndex;

    /* This is a workaround, and should probably eventually be removed. */
    public Graph graph;

    /** Used for finding first/last trip of the day. This is the time at which service ends for the day. */
    public final int overnightBreak = 60 * 60 * 2; // FIXME not being set, this was done in transitIndex

    public GraphIndex (Graph graph) {
        LOG.info("Indexing graph...");
        for (Agency a : graph.getAgencies()) {
            agencyForId.put(a.getId(), a);
        }
        Collection<Edge> edges = graph.getEdges();
        /* We will keep a separate set of all vertices in case some have the same label. 
         * Maybe we should just guarantee unique labels. */
        Set<Vertex> vertices = Sets.newHashSet();
        for (Edge edge : edges) {
            vertices.add(edge.getFromVertex());
            vertices.add(edge.getToVertex());
            if (edge instanceof TablePatternEdge) {
                TablePatternEdge patternEdge = (TablePatternEdge) edge;
                TripPattern pattern = patternEdge.getPattern();
                patternForId.put(pattern.code, pattern);
            }
        }
        for (Vertex vertex : vertices) {
            vertexForId.put(vertex.getLabel(), vertex);
            if (vertex instanceof TransitStop) {
                TransitStop transitStop = (TransitStop) vertex;
                Stop stop = transitStop.getStop();
                stopForId.put(stop.getId(), stop);
                stopVertexForStop.put(stop, transitStop);
                stopsForParentStation.put(stop.getParentStation(), stop);
            }
        }
        stopSpatialIndex.setProjectionMeridian(vertices.iterator().next().getCoordinate().x);
        for (TransitStop stopVertex : stopVertexForStop.values()) {
            stopSpatialIndex.put(stopVertex.getCoordinate(), stopVertex);
        }
        for (TripPattern pattern : patternForId.values()) {
            patternsForAgency.put(pattern.route.getAgency(), pattern);
            patternsForRoute.put(pattern.route, pattern);
            for (Trip trip : pattern.getTrips()) {
                patternForTrip.put(trip, pattern);
                tripForId.put(trip.getId(), trip);
            }
            for (Stop stop: pattern.getStops()) {
                patternsForStop.put(stop, pattern);
            }
        }
        for (Route route : patternsForRoute.asMap().keySet()) {
            routeForId.put(route.getId(), route);
        }

        clusterStops();
        LOG.info("Creating a spatial index for stop clusters.");
        stopClusterSpatialIndex = new HashGrid<StopCluster>();
        for (StopCluster cluster : stopClusterForId.values()) {
            stopClusterSpatialIndex.put(new Coordinate(cluster.lon, cluster.lat), cluster);
        }

        // Copy these two service indexes from the graph until we have better ones.
        calendarService = graph.getCalendarService();
        serviceCodes = graph.serviceCodes;
        this.graph = graph;
        LOG.info("Done indexing graph.");
    }

    private void analyzeServices() {
        // This is a mess because CalendarService, CalendarServiceData, etc. are all in OBA.
        // TODO catalog days of the week and exceptions for each service day.
        // Make a table of which services are running on each calendar day.
        // Really the calendarService should be entirely replaced with a set
        // of simple indexes in GraphIndex.
    }

    private static DistanceLibrary distlib = new SphericalDistanceLibrary();

    /** Get all trip patterns running through any stop in the given stop cluster. */
    private Set<TripPattern> patternsForStopCluster(StopCluster sc) {
        Set<TripPattern> tripPatterns = Sets.newHashSet();
        for (Stop stop : sc.children) tripPatterns.addAll(patternsForStop.get(stop));
        return tripPatterns;
    }

    /**
     * Initialize transfer data needed for profile routing.
     * Find the best transfers between each pair of patterns that pass near one another.
     */
    public void initializeProfileTransfers() {
        transfersFromStopCluster = HashMultimap.create();
        final double TRANSFER_RADIUS = 500.0; // meters
        Map<P2<TripPattern>, ProfileTransfer.GoodTransferList> transfers = Maps.newHashMap();
        LOG.info("Finding transfers...");
        for (StopCluster sc0 : stopClusterForId.values()) {
            Set<TripPattern> tripPatterns0 = patternsForStopCluster(sc0);
            // Accounts for area-like (rather than point-like) nature of clusters
            Map<StopCluster, Double> nearbyStopClusters = findNearbyStopClusters(sc0, TRANSFER_RADIUS);
            for (StopCluster sc1 : nearbyStopClusters.keySet()) {
                double distance = nearbyStopClusters.get(sc1);
                Set<TripPattern> tripPatterns1 = patternsForStopCluster(sc1);
                for (TripPattern tp0 : tripPatterns0) {
                    for (TripPattern tp1 : tripPatterns1) {
                        if (tp0 == tp1) continue;
                        P2<TripPattern> pair = new P2<TripPattern>(tp0, tp1);
                        ProfileTransfer.GoodTransferList list = transfers.get(pair);
                        if (list == null) {
                            list = new ProfileTransfer.GoodTransferList();
                            transfers.put(pair, list);
                        }
                        list.add(new ProfileTransfer(tp0, tp1, sc0, sc1, (int)distance));
                    }
                }
            }
        }
        /* Now filter the transfers down to eliminate long series of transfers in shared trunks. */
        LOG.info("Filtering out long series of transfers on trunks shared between patterns.");
        for (P2<TripPattern> pair : transfers.keySet()) {
            ProfileTransfer.GoodTransferList list = transfers.get(pair);
            TripPattern fromPattern = pair.getFirst(); // TODO consider using second (think of express-local transfers in NYC)
            Map<StopCluster, ProfileTransfer> transfersByFromCluster = Maps.newHashMap();
            for (ProfileTransfer transfer : list.good) {
                transfersByFromCluster.put(transfer.sc1, transfer);
            }
            List<ProfileTransfer> retainedTransfers = Lists.newArrayList();
            boolean inSeries = false; // true whenever a transfer existed for the last stop in the stop pattern
            for (Stop stop : fromPattern.stopPattern.stops) {
                StopCluster cluster = this.stopClusterForStop.get(stop);
                //LOG.info("stop {} cluster {}", stop, cluster.id);
                ProfileTransfer transfer = transfersByFromCluster.get(cluster);
                if (transfer == null) {
                    inSeries = false;
                    continue;
                }
                if (inSeries) continue;
                // Keep this transfer: it's not preceded by another stop with a transfer in this stop pattern
                retainedTransfers.add(transfer);
                inSeries = true;
            }
            //LOG.info("patterns {}, {} transfers", pair, retainedTransfers.size());
            for (ProfileTransfer tr : retainedTransfers) {
                transfersFromStopCluster.put(tr.sc1, tr);
                //LOG.info("   {}", tr);
            }
        }
        /*
         * for (Stop stop : transfersForStop.keys()) { System.out.println("STOP " + stop); for
         * (Transfer transfer : transfersForStop.get(stop)) { System.out.println("    " +
         * transfer.toString()); } }
         */
        LOG.info("Done finding transfers.");
    }

    /**
     * Find transfer candidates for profile routing.
     * TODO replace with an on-street search using the existing profile router functions.
     */
    public Map<StopCluster, Double> findNearbyStopClusters (StopCluster sc, double radius) {
        Map<StopCluster, Double> ret = Maps.newHashMap();
        for (StopCluster cluster : stopClusterSpatialIndex.query(sc.lon, sc.lat, radius)) {
            // TODO this should account for area-like nature of clusters. Use size of bounding boxes.
            double distance = distlib.distance(sc.lat, sc.lon, cluster.lat, cluster.lon);
            if (distance < radius) ret.put(cluster, distance);
        }
        return ret;
    }

    /** An OBA Service Date is a local date without timezone, only year month and day. */
    public BitSet servicesRunning (ServiceDate date) {
        BitSet services = new BitSet(calendarService.getServiceIds().size());
        for (AgencyAndId serviceId : calendarService.getServiceIdsOnDate(date)) {
            int n = serviceCodes.get(serviceId);
            if (n < 0) continue;
            services.set(n);
        }
        return services;
    }

    /**
     * Wraps the other servicesRunning whose parameter is an OBA ServiceDate.
     * Joda LocalDate is a similar class.
     */
    public BitSet servicesRunning (LocalDate date) {
        return servicesRunning(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
    }

    /** Dynamically generate the set of Routes passing though a Stop on demand. */
    public Set<Route> routesForStop(Stop stop) {
        Set<Route> routes = Sets.newHashSet();
        for (TripPattern p : patternsForStop.get(stop)) {
            routes.add(p.route);
        }
        return routes;
    }

    /**
     * Fetch upcoming vehicle arrivals at a stop.
     * This is a rather convoluted process because all of the departure search functions currently assume the
     * existence of a State and a routing context. It would be nice to have another function that gives
     * all departures within a time window at a stop, being careful to get a mix of all patterns passing through
     * that stop. In fact, such a function could replace the current boarding logic if we want to allow boarding
     * more than one trip on the same route at once (return more than one state).
     * The current implementation is a sketch and does not adequately
     */
    public Collection<StopTimesInPattern> stopTimesForStop(Stop stop) {
        List<StopTimesInPattern> ret = Lists.newArrayList();
        RoutingRequest req = new RoutingRequest();
        req.setRoutingContext(graph, (Vertex)null, (Vertex)null);
        State state = new State(req);
        for (TripPattern pattern : patternsForStop.get(stop)) {
            StopTimesInPattern times = new StopTimesInPattern(pattern);
            // Should actually be getUpdatedTimetable
            Timetable table = pattern.scheduledTimetable;
            // A Stop may occur more than once in a pattern, so iterate over all Stops.
            int sidx = 0;
            for (Stop currStop : table.pattern.stopPattern.stops) {
                if (currStop != stop) continue;
                for (ServiceDay sd : req.rctx.serviceDays) {
                    TripTimes tt = table.getNextTrip(state, sd, sidx, true);
                    if (tt != null) {
                        times.times.add(new TripTimeShort(tt, sidx, stop));
                    }
                }
                sidx++;
            }
            if ( ! times.times.isEmpty()) ret.add(times);
        }
        return ret;
    }

    /**
     * FIXME OBA parentStation field is a string, not an AgencyAndId, so it has no agency/feed scope
     * But the DC regional graph has no parent stations pre-defined, so no use dealing with them for now.
     * However Trimet stops have "landmark" or Transit Center parent stations, so we don't use the parent stop field.
     *
     * Ideally in the future stop clusters will replicate and/or share implementation with GTFS parent stations.
     *
     * We can't use a similarity comparison, we need exact matches. This is because many street names differ by only
     * one letter or number, e.g. 34th and 35th or Avenue A and Avenue B.
     * Therefore normalizing the names before the comparison is essential.
     * The agency must provide either parent station information or a well thought out stop naming scheme to cluster
     * stops -- no guessing is reasonable without that information.
     */
    public void clusterStops() {
        int psIdx = 0; // unique index for next parent stop
        LOG.info("Clustering stops by geographic proximity and name...");
        // Each stop without a cluster will greedily claim other stops without clusters.
        Map<String, String> descriptionForStationId = Maps.newHashMap();
        for (Stop s0 : stopForId.values()) {
            if (stopClusterForStop.containsKey(s0)) continue; // skip stops that have already been claimed by a cluster
            String s0normalizedName = StopNameNormalizer.normalize(s0.getName());
            StopCluster cluster = new StopCluster(String.format("C%03d", psIdx++), s0normalizedName);
            // LOG.info("stop {}", s0normalizedName);
            // No need to explicitly add s0 to the cluster. It will be found in the spatial index query below.
            for (TransitStop ts1 : stopSpatialIndex.query(s0.getLon(), s0.getLat(), CLUSTER_RADIUS)) {
                Stop s1 = ts1.getStop();
                double geoDistance = SphericalDistanceLibrary.getInstance().fastDistance(s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
                if (geoDistance < CLUSTER_RADIUS) {
                    String s1normalizedName = StopNameNormalizer.normalize(s1.getName());
                    // LOG.info("   --> {}", s1normalizedName);
                    // LOG.info("       geodist {} stringdist {}", geoDistance, stringDistance);
                    if (s1normalizedName.equals(s0normalizedName)) {
                        // Create a bidirectional relationship between the stop and its cluster
                        cluster.children.add(s1);
                        stopClusterForStop.put(s1, cluster);
                    }
                }
            }
            cluster.computeCenter();
            stopClusterForId.put(cluster.id, cluster);
        }
//        LOG.info("Done clustering stops.");
//        for (StopCluster cluster : clusters) {
//            LOG.info("{} at {} {}", cluster.name, cluster.lat, cluster.lon);
//            for (Stop stop : cluster.children) {
//                LOG.info("   {}", stop.getName());
//            }
//        }
    }

}
