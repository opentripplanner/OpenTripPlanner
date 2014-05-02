package org.opentripplanner.routing.graph;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
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
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.profile.ProfileTransfer;
import org.opentripplanner.profile.StopAtDistance;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TablePatternEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
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
    public final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    public final Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
    public final Multimap<String, Stop> stopsForParentStation = ArrayListMultimap.create();
    public final HashGrid<TransitStop> stopSpatialIndex = new HashGrid<TransitStop>();

    /* Should eventually be replaced with new serviceId indexes. */
    private final CalendarService calendarService;
    private final Map<AgencyAndId,Integer> serviceCodes;

    /* Full-text search extensions */
    public LuceneIndex luceneIndex;

    /* Separate transfers for profile routing */
    public Multimap<Stop, ProfileTransfer> transfersForStop;

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
                patternForId.put(pattern.getCode(), pattern);
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
        luceneIndex = new LuceneIndex(this, true);
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

    /**
     * Initialize transfer data needed for profile routing.
     * Find the best transfers between each pair of routes that pass near one another.
     */
    public void initializeProfileTransfers() {
        transfersForStop = HashMultimap.create();
        final double TRANSFER_RADIUS = 500.0; // meters
        SimpleIsochrone.MinMap<P2<TripPattern>, ProfileTransfer> bestTransfers = new SimpleIsochrone.MinMap<P2<TripPattern>, ProfileTransfer>();
        LOG.info("Finding transfers...");
        for (Stop s0 : stopForId.values()) {
            Collection<TripPattern> ps0 = patternsForStop.get(s0);
            for (StopAtDistance sd : findTransitStops(s0.getLon(), s0.getLat(), TRANSFER_RADIUS)) {
                Stop s1 = sd.stop;
                if (s0 == s1)
                    continue;
                Collection<TripPattern> ps1 = patternsForStop.get(s1);
                for (TripPattern p0 : ps0) {
                    for (TripPattern p1 : ps1) {
                        if (p0 == p1)
                            continue;
                        bestTransfers.putMin(new P2<TripPattern>(p0, p1), new ProfileTransfer(p0, p1,
                                s0, s1, sd.distance));
                    }
                }
            }
        }
        for (ProfileTransfer tr : bestTransfers.values()) {
            transfersForStop.put(tr.s1, tr);
        }
        /*
         * for (Stop stop : transfersForStop.keys()) { System.out.println("STOP " + stop); for
         * (Transfer transfer : transfersForStop.get(stop)) { System.out.println("    " +
         * transfer.toString()); } }
         */
        LOG.info("Done finding transfers.");
    }

    /**
     * For profile routing. Actually, now only used for finding transfers.
     * TODO replace with an on-street search.
     */
    public List<StopAtDistance> findTransitStops(double lon, double lat, double radius) {
        List<StopAtDistance> ret = Lists.newArrayList();
        for (TransitStop tstop : stopSpatialIndex.query(lon, lat, radius)) {
            Stop stop = tstop.getStop();
            int distance = (int) distlib.distance(lat, lon, stop.getLat(), stop.getLon());
            if (distance < radius)
                ret.add(new StopAtDistance(stop, distance));
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
}
