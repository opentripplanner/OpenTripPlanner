package org.opentripplanner.routing.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.apache.lucene.util.PriorityQueue;
import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.profile.ProfileTransfer;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.profile.StopNameNormalizer;
import org.opentripplanner.profile.StopTreeCache;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.TablePatternEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains all the transient indexes of graph elements -- those that are not
 * serialized with the graph. Caching these maps is essentially an optimization, but a big one.
 * The index is bootstrapped from the graph's list of edges.
 */
public class GraphIndex {

    private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);
    private static final int CLUSTER_RADIUS = 400; // meters

    /** maximum distance to walk after leaving transit in Analyst */
    public static final int MAX_WALK_METERS = 2000;

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
    final HashGridSpatialIndex<TransitStop> stopSpatialIndex = new HashGridSpatialIndex<TransitStop>();
    public final Map<Stop, StopCluster> stopClusterForStop = Maps.newHashMap();
    public final Map<String, StopCluster> stopClusterForId = Maps.newHashMap();

    /* Should eventually be replaced with new serviceId indexes. */
    private final CalendarService calendarService;
    private final Map<AgencyAndId,Integer> serviceCodes;

    /* Full-text search extensions */
    public LuceneIndex luceneIndex;

    /* Separate transfers for profile routing */
    public Multimap<StopCluster, ProfileTransfer> transfersFromStopCluster;
    private HashGridSpatialIndex<StopCluster> stopClusterSpatialIndex = null;

    /* Extra indices for applying realtime updates (lazy-initialized). */
    public Map<String, Route> routeForIdWithoutAgency = null;
    public Map<String, Trip> tripForIdWithoutAgency = null;
    public Map<String, Stop> stopForIdWithoutAgency = null;

    /* This is a workaround, and should probably eventually be removed. */
    public Graph graph;

    /** Used for finding first/last trip of the day. This is the time at which service ends for the day. */
    public final int overnightBreak = 60 * 60 * 2; // FIXME not being set, this was done in transitIndex

    /** Store distances from each stop to all nearby street intersections. Useful in speeding up analyst requests. */
    private transient StopTreeCache stopTreeCache = null;

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
        for (TransitStop stopVertex : stopVertexForStop.values()) {
            Envelope envelope = new Envelope(stopVertex.getCoordinate());
            stopSpatialIndex.insert(envelope, stopVertex);
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

        // Copy these two service indexes from the graph until we have better ones.
        calendarService = graph.getCalendarService();
        serviceCodes = graph.serviceCodes;
        this.graph = graph;
        LOG.info("Done indexing graph.");
    }

    /**
     * Stop clustering is slow to perform and only used in profile routing for the moment.
     * Therefore it is not done automatically, and any method requiring stop clusters should call this method
     * to ensure that the necessary indexes are lazy-initialized.
     */
    public synchronized void clusterStopsAsNeeded() {
        if (stopClusterSpatialIndex == null) {
            clusterStops();
            LOG.info("Creating a spatial index for stop clusters.");
            stopClusterSpatialIndex = new HashGridSpatialIndex<StopCluster>();
            for (StopCluster cluster : stopClusterForId.values()) {
                Envelope envelope = new Envelope(new Coordinate(cluster.lon, cluster.lat));
                stopClusterSpatialIndex.insert(envelope, cluster);
            }
        }
    }

    private void analyzeServices() {
        // This is a mess because CalendarService, CalendarServiceData, etc. are all in OBA.
        // TODO catalog days of the week and exceptions for each service day.
        // Make a table of which services are running on each calendar day.
        // Really the calendarService should be entirely replaced with a set
        // of simple indexes in GraphIndex.
    }

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
        LOG.info("Finding transfers between clusters...");
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
            TripPattern fromPattern = pair.first; // TODO consider using second (think of express-local transfers in NYC)
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
        Envelope env = new Envelope(new Coordinate(sc.lon, sc.lat));
        env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(radius, sc.lat),
                SphericalDistanceLibrary.metersToDegrees(radius));
        for (StopCluster cluster : stopClusterSpatialIndex.query(env)) {
            // TODO this should account for area-like nature of clusters. Use size of bounding boxes.
            double distance = SphericalDistanceLibrary.distance(sc.lat, sc.lon, cluster.lat, cluster.lon);
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
     * Fetch upcoming vehicle departures from a stop.
     * Fetches two departures for each pattern during the next 24 hours as default
     */
    public Collection<StopTimesInPattern> stopTimesForStop(Stop stop) {
        return stopTimesForStop(stop, System.currentTimeMillis()/1000, 24 * 60 * 60, 2);
    }

    /**
     * Fetch upcoming vehicle departures from a stop.
     * It goes though all patterns passing the stop for the previous, current and next service date.
     * It uses a priority queue to keep track of the next departures. The queue is shared between all dates, as services
     * from the previous service date can visit the stop later than the current service date's services. This happens
     * eg. with sleeper trains.
     *
     * TODO: Add frequency based trips
     * @param stop Stop object to perform the search for
     * @param startTime Start time for the search. Seconds from UNIX epoch
     * @param timeRange Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     * @return
     */
    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures) {

        if (startTime == 0) {
            startTime = System.currentTimeMillis() / 1000;
        }
        List<StopTimesInPattern> ret = new ArrayList<>();
        TimetableSnapshot snapshot = null;
        if (graph.timetableSnapshotSource != null) {
            snapshot = graph.timetableSnapshotSource.getTimetableSnapshot();
        }
        ServiceDate[] serviceDates = {new ServiceDate().previous(), new ServiceDate(), new ServiceDate().next()};

        for (TripPattern pattern : patternsForStop.get(stop)) {

            // Use the Lucene PriorityQueue, which has a fixed size
            PriorityQueue<TripTimeShort> pq = new PriorityQueue<TripTimeShort>(numberOfDepartures) {
                @Override
                protected boolean lessThan(TripTimeShort tripTimeShort, TripTimeShort t1) {
                    // Calculate exact timestamp
                    return (tripTimeShort.serviceDay + tripTimeShort.realtimeDeparture) >
                            (t1.serviceDay + t1.realtimeDeparture);
                }
            };

            // Loop through all possible days
            for (ServiceDate serviceDate : serviceDates) {
                ServiceDay sd = new ServiceDay(graph, serviceDate, calendarService, pattern.route.getAgency().getId());
                Timetable tt;
                if (snapshot != null){
                    tt = snapshot.resolve(pattern, serviceDate);
                } else {
                    tt = pattern.scheduledTimetable;
                }

                if (!tt.temporallyViable(sd, startTime, timeRange, true)) continue;

                int secondsSinceMidnight = sd.secondsSinceMidnight(startTime);
                int sidx = 0;
                for (Stop currStop : pattern.stopPattern.stops) {
                    if (currStop == stop) {
                        for (TripTimes t : tt.tripTimes) {
                            if (!sd.serviceRunning(t.serviceCode)) continue;
                            if (t.getDepartureTime(sidx) != -1 &&
                                    t.getDepartureTime(sidx) >= secondsSinceMidnight) {
                                pq.insertWithOverflow(new TripTimeShort(t, sidx, stop, sd));
                            }
                        }

                        // TODO: This needs to be adapted after #1647 is merged
                        for (FrequencyEntry freq : tt.frequencyEntries) {
                            if (!sd.serviceRunning(freq.tripTimes.serviceCode)) continue;
                            int departureTime = freq.nextDepartureTime(sidx, secondsSinceMidnight);
                            if (departureTime == -1) continue;
                            int lastDeparture = freq.endTime + freq.tripTimes.getArrivalTime(sidx) -
                                    freq.tripTimes.getDepartureTime(0);
                            int i = 0;
                            while (departureTime <= lastDeparture && i < numberOfDepartures) {
                                pq.insertWithOverflow(new TripTimeShort(freq.materialize(sidx, departureTime, true), sidx, stop, sd));
                                departureTime += freq.headway;
                                i++;
                            }
                        }
                    }
                    sidx++;
                }
            }

            if (pq.size() != 0) {
                StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
                while (pq.size() != 0) {
                    stopTimes.times.add(0, pq.pop());
                }
                ret.add(stopTimes);
            }
        }
        return ret;
    }

    /**
     * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when creating complete stop
     * timetables for a single day.
     *
     * @param stop Stop object to perform the search for
     * @param serviceDate Return all departures for the specified date
     * @return
     */
    public List<StopTimesInPattern> getStopTimesForStop(Stop stop, ServiceDate serviceDate) {
        List<StopTimesInPattern> ret = new ArrayList<>();
        TimetableSnapshot snapshot = null;
        if (graph.timetableSnapshotSource != null) {
            snapshot = graph.timetableSnapshotSource.getTimetableSnapshot();
        }
        Collection<TripPattern> patterns = patternsForStop.get(stop);
        for (TripPattern pattern : patterns) {
            StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
            Timetable tt;
            if (snapshot != null){
                tt = snapshot.resolve(pattern, serviceDate);
            } else {
                tt = pattern.scheduledTimetable;
            }
            ServiceDay sd = new ServiceDay(graph, serviceDate, calendarService, pattern.route.getAgency().getId());
            int sidx = 0;
            for (Stop currStop : pattern.stopPattern.stops) {
                if (currStop == stop) {
                    for (TripTimes t : tt.tripTimes) {
                        if (!sd.serviceRunning(t.serviceCode)) continue;
                        stopTimes.times.add(new TripTimeShort(t, sidx, stop, sd));
                    }
                }
                sidx++;
            }
            ret.add(stopTimes);
        }
        return ret;
    }

    /** Fetch a cache of nearby intersection distances for every transit stop in this graph, lazy-building as needed. */
    public StopTreeCache getStopTreeCache() {
        if (stopTreeCache == null) {
            synchronized (this) {
                if (stopTreeCache == null) {
                    stopTreeCache = new StopTreeCache(graph, MAX_WALK_METERS); // TODO make this max-distance variable
                }
            }
        }
        return stopTreeCache;
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
        for (Stop s0 : stopForId.values()) {
            if (stopClusterForStop.containsKey(s0)) continue; // skip stops that have already been claimed by a cluster
            String s0normalizedName = StopNameNormalizer.normalize(s0.getName());
            StopCluster cluster = new StopCluster(String.format("C%03d", psIdx++), s0normalizedName);
            // LOG.info("stop {}", s0normalizedName);
            // No need to explicitly add s0 to the cluster. It will be found in the spatial index query below.
            Envelope env = new Envelope(new Coordinate(s0.getLon(), s0.getLat()));
            env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(CLUSTER_RADIUS, s0.getLat()),
                    SphericalDistanceLibrary.metersToDegrees(CLUSTER_RADIUS));
            for (TransitStop ts1 : stopSpatialIndex.query(env)) {
                Stop s1 = ts1.getStop();
                double geoDistance = SphericalDistanceLibrary.fastDistance(
                        s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
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
//        for (StopCluster cluster : stopClusterForId.values()) {
//            LOG.info("{} at {} {}", cluster.name, cluster.lat, cluster.lon);
//            for (Stop stop : cluster.children) {
//                LOG.info("   {}", stop.getName());
//            }
//        }
    }

}
