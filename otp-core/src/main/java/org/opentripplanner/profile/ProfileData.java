package org.opentripplanner.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.resource.analyst.SimpleIsochrone.MinMap;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Read-only data shared between all profile routers.
 */
public class ProfileData {

    public static final double WALK_RADIUS = 500; // meters
    private static final Logger LOG = LoggerFactory.getLogger(ProfileData.class);

    @AllArgsConstructor
    public static class Transfer implements Comparable<Transfer> {
        
        Pattern tp1, tp2;
        Stop s1, s2;
        int distance; // meters
        
        @Override
        public int compareTo(Transfer that) {
            return this.distance - that.distance;
        } 
        
        @Override
        public String toString() {
            return String.format("Transfer %s %s %s %s %d", tp1.route, tp2.route, 
                    s1.getCode(), s2.getCode(), distance);
        }
    
    }

    public static class Pattern {
        
        int[] min, avg, max;
        List<Stop> stops;
        Route route;
        
        public Pattern (Collection<TableTripPattern> ttps) {
            TableTripPattern first = ttps.iterator().next();
            stops = first.getStops();
            route = first.exemplar.getRoute(); // only _table_ trip patterns have exemplars
            int nStops = stops.size();
            min = new int[nStops];
            avg = new int[nStops];
            max = new int[nStops];
            for (int s = 0; s < nStops; ++s) min[s] = Integer.MAX_VALUE;
            for (TableTripPattern ttp : ttps) {
                if ( ! ttp.getStops().equals(stops)) {
                    LOG.error("mismatched stop pattern");
                    continue;
                }
                if ( ! ttp.exemplar.getRoute().equals(route)) {
                    LOG.error("mismatched route");
                    continue;
                }
                int nTrips = ttp.getTrips().size();
                for (int i = 0; i < nTrips; ++i) {
                    TripTimes tt = ttp.getTripTimes(i);
                    int t0 = tt.getDepartureTime(0); // storing relative times
                    for (int stop = 1; stop < nStops; ++stop) { // hops vs. stops
                        // TODO switch to running time rather than dwell time, effect on stats
                        int tn = tt.getArrivalTime(stop - 1); // note this is ignoring dwell times
                        tn -= t0;
                        if (tn < min[stop]) min[stop] = tn;
                        if (tn > max[stop]) max[stop] = tn;
                    }
                }                
            }
            // FIXME: horrific hack
            for (int s = 0; s < nStops; ++s) avg[s] = (min[s] + max[s]) / 2;                        
        }
        
        public String stopString() {
            StringBuilder sb = new StringBuilder();
            for (Stop stop : this.stops) {
                sb.append(stop.getCode());
                sb.append(" ");
            }
            return sb.toString();
        }
        
        public String toString() {
            return String.format("route %s toward %s", route.getShortName(), stops.get(stops.size() - 1).getCode());
        }
    }

    private static SphericalDistanceLibrary distlib = new SphericalDistanceLibrary();
    private Graph graph;
    STRtree stopTree = new STRtree();
    private List<Stop> stops = new ArrayList<Stop> ();
    private List<Pattern> patterns = Lists.newArrayList();
    Multimap<Stop, Pattern> patternsForStop = HashMultimap.create();
    Multimap<Stop, Transfer> transfersForStop = HashMultimap.create();
    Multimap<Route, Pattern> patternsForRoute = HashMultimap.create();
    //private Set<Route> routes = Sets.newHashSet();
    
    // move some of this functionality into the objects themselves

    @AllArgsConstructor
    public static class StopAtDistance implements Comparable<StopAtDistance> {
        Stop stop;
        int distance;
        @Override 
        public int compareTo(StopAtDistance that) {
            return that.distance - this.distance;
        }
        public String toString() {
            return String.format("stop %s at %dm", stop.getCode(), distance);
        }
    }

    /**
     * @return transfers to all nearby patterns, with only one transfer per pattern (the closest one). 
     */
    public Map<Pattern, StopAtDistance> closestPatterns(double lon, double lat) {
        MinMap<Pattern, StopAtDistance> closest = new MinMap<Pattern, StopAtDistance>();
        for (Stop stop : findTransitStops (lon, lat, WALK_RADIUS)) {
            int distance = (int) distlib.distance(lat, lon, stop.getLat(), stop.getLon());
            for (Pattern pattern : patternsForStop.get(stop)) {
                closest.putMin(pattern, new StopAtDistance(stop, distance));
            }
        }
        return closest;
    }
    
    private void findTransfers () {
        MinMap<P2<Pattern>, Transfer> bestTransfers = new MinMap<P2<Pattern>, ProfileData.Transfer>();
        LOG.info("Finding transfers...");
        for (Stop s0 : stops) {
            Collection<Pattern> ps0 = patternsForStop.get(s0);
            for (Stop s1 : findTransitStops (s0.getLon(), s0.getLat(), WALK_RADIUS)) {
                if (s0 == s1) continue;
                double distance = distlib.distance(s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
                Collection<Pattern> ps1 = patternsForStop.get(s1);
                for (Pattern p0 : ps0) {
                    for (Pattern p1 : ps1) {
                        if (p0 == p1) continue;
                        bestTransfers.putMin(
                            new P2<Pattern>(p0, p1), 
                            new Transfer(p0, p1, s0, s1, (int) distance)
                        );
                    }
                }
            }
        }
        for (Transfer tr : bestTransfers.values()) {
            transfersForStop.put(tr.s1, tr);
        }
        LOG.info("Done finding transfers.");
    }

    /**
     * Graphs do not have a list of their trip patterns anywhere. You have to build it from stops and edges.
     */
    private Collection<TripPattern> getTripPatterns() {
        Set<TripPattern> tripPatterns = new HashSet<TripPattern> ();
        for (TransitStop ts : IterableLibrary.filter(graph.getVertices(), TransitStop.class)) {
            stops.add(ts.getStop());
            TransitStopDepart departVertex = null;
            for (Edge e : ts.getOutgoing()) {
                if (e.getToVertex() instanceof TransitStopDepart) {
                    departVertex = (TransitStopDepart) e.getToVertex();
                    break;
                }
            }
            if (departVertex != null) {
                for (Edge e : departVertex.getOutgoing()) {
                    if (e.getToVertex() instanceof PatternStopVertex) {
                        TripPattern tripPattern = ((PatternStopVertex) e.getToVertex()).getTripPattern();
                        tripPatterns.add(tripPattern);
                        //LOG.info("pattern: {}", tripPattern);
                    }
                }
            }
        }
        return tripPatterns;
    }
    
    public void setup () {
        /* bin table trip patterns by stop sequence */
        Multimap<List<Stop>, TableTripPattern> tpsBinned = HashMultimap.create();
        for (TableTripPattern ttp : IterableLibrary.filter(getTripPatterns(), TableTripPattern.class)) {
            tpsBinned.put(ttp.getStops(), ttp);
        }
        /* make one profile trip pattern per bin of table trip patterns */
        for (Collection<TableTripPattern> tpBin : tpsBinned.asMap().values()) {
            patterns.add(new Pattern(tpBin));
        }
        LOG.info("Number of patterns is {}", patterns.size());
        /* index trip patterns on the stops they contain */
        for (Pattern ptp : patterns) {
            for (Stop stop : ptp.stops) {
                patternsForStop.put(stop, ptp);
            }
        }
        /* index trip patterns on routes */
        for (Pattern ptp : patterns) {
            patternsForRoute.put(ptp.route, ptp);
        }
        Set<Route> routes = patternsForRoute.keySet();
        for (Route route : routes) {
            LOG.debug("Route {} {}", route.getShortName(), route.getLongName());
            for (Pattern pattern : patternsForRoute.get(route)) {
                LOG.debug("  {}", pattern.stopString());                
            }
        }
        /* index all stops spatially */
        for (Stop s : stops) {
            stopTree.insert(new Envelope(s.getLon(), s.getLat(), s.getLon(), s.getLat()), s);
        }
        stopTree.build();
        /* find the best transfer point between each pair of patterns */
        findTransfers();        
    }

    public List<Stop> findTransitStops(double lon, double lat, double radius) {
        Envelope envelope = distlib.bounds(lat, lon, radius, radius);
        List<?> stops = stopTree.query(envelope);
        List<Stop> out = Lists.newArrayList();
        for (Stop stop : IterableLibrary.filter(stops, Stop.class)) {
            if (distlib.fastDistance(lat, lon, stop.getLat(), stop.getLon()) < radius) {
                out.add(stop);
            }
        }
        return out;
    }

    public ProfileData (Graph graph) {
        this.graph = graph;
        setup ();
    }
}
