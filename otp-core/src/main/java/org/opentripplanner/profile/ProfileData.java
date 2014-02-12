package org.opentripplanner.profile;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;

import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.ws.analyst.SimpleIsochrone.MinMap;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Read-only data shared between all profile routers.
 */
public class ProfileData {

    public static final double WALK_RADIUS = 500; // meters
    private static final Logger LOG = LoggerFactory.getLogger(ProfileData.class);

    @AllArgsConstructor
    public static class Transfer implements Comparable<Transfer> {

        TableTripPattern tp1, tp2;
        Stop s1, s2;
        int distance; // meters

        @Override
        public int compareTo(Transfer that) {
            return this.distance - that.distance;
        }

        @Override
        public String toString() {
            return String.format("Transfer %s %s %s %s %d", tp1.getCode(), s1.getId(),
                    tp2.getCode(), s2.getId(), distance);
        }

    }

    private static SphericalDistanceLibrary distlib = new SphericalDistanceLibrary();
    private Graph graph;
    private List<TableTripPattern> patterns = Lists.newArrayList();
    Multimap<Stop, TableTripPattern> patternsForStop = HashMultimap.create();
    Multimap<Stop, Transfer> transfersForStop = HashMultimap.create();
    Multimap<Route, TableTripPattern> patternsForRoute = HashMultimap.create();

    /** An OBA Service Date is a local date without timezone, only year month and day. */
    public BitSet servicesRunning (ServiceDate date) {
        CalendarService cs = graph.getCalendarService();
        BitSet services = new BitSet(cs.getServiceIds().size());
        for (AgencyAndId serviceId : cs.getServiceIdsOnDate(date)) {
            int n = graph.serviceCodes.get(serviceId);
            if (n < 0) continue;
            services.set(n);
        }
        return services;
    }        

    /** Wraps the other version that accepts OBA ServiceDates. Joda LocalDate is similar. */
    public BitSet servicesRunning (LocalDate date) {
        return servicesRunning(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
    }

    /**
     * Here we are using stops rather than indices within a pattern, because we want to consider
     * stops that appear more than once at every index where they occur.
     */
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
     * We want a single stop for each route, so this is not the right place to convert to stop
     * indexes from stop objects.
     * 
     * @return transfers to all nearby patterns, with only one transfer per pattern (the closest
     *         one).
     */
    public Map<TableTripPattern, StopAtDistance> closestPatterns(double lon, double lat) {
        MinMap<TableTripPattern, StopAtDistance> closest = new MinMap<TableTripPattern, StopAtDistance>();
        for (StopAtDistance stopDist : findTransitStops(lon, lat, WALK_RADIUS)) {
            for (TableTripPattern pattern : patternsForStop.get(stopDist.stop)) {
                closest.putMin(pattern, stopDist);
            }
        }
        return closest;
    }

    private void findTransfers() {
        MinMap<P2<TableTripPattern>, Transfer> bestTransfers = new MinMap<P2<TableTripPattern>, ProfileData.Transfer>();
        LOG.info("Finding transfers...");
        for (Stop s0 : graph.getIndex().stopForId.values()) {
            Collection<TableTripPattern> ps0 = patternsForStop.get(s0);
            for (StopAtDistance sd : findTransitStops(s0.getLon(), s0.getLat(), WALK_RADIUS)) {
                Stop s1 = sd.stop;
                if (s0 == s1)
                    continue;
                Collection<TableTripPattern> ps1 = patternsForStop.get(s1);
                for (TableTripPattern p0 : ps0) {
                    for (TableTripPattern p1 : ps1) {
                        if (p0 == p1)
                            continue;
                        bestTransfers.putMin(new P2<TableTripPattern>(p0, p1), new Transfer(p0, p1,
                                s0, s1, sd.distance));
                    }
                }
            }
        }
        for (Transfer tr : bestTransfers.values()) {
            transfersForStop.put(tr.s1, tr);
        }
        /*
         * for (Stop stop : transfersForStop.keys()) { System.out.println("STOP " + stop); for
         * (Transfer transfer : transfersForStop.get(stop)) { System.out.println("    " +
         * transfer.toString()); } }
         */
        LOG.info("Done finding transfers.");
    }

    public void setup() {
        patternsForRoute = graph.getIndex().patternsForRoute;
        patterns.addAll(patternsForRoute.values()); // unnecessary
        LOG.info("Number of patterns is {}", patterns.size());
        patternsForStop = graph.getIndex().patternsForStop;
        /* find the best transfer point between each pair of patterns */
        findTransfers();
    }

    public List<StopAtDistance> findTransitStops(double lon, double lat, double radius) {
        List<StopAtDistance> ret = Lists.newArrayList();
        for (TransitStop tstop : graph.getIndex().stopSpatialIndex.query(lon, lat, radius)) {
            Stop stop = tstop.getStop();
            int distance = (int) distlib.distance(lat, lon, stop.getLat(), stop.getLon());
            if (distance < radius)
                ret.add(new StopAtDistance(stop, distance));
        }
        return ret;
    }

    public ProfileData(Graph graph) {
        this.graph = graph;
        setup();
    }
    
}
