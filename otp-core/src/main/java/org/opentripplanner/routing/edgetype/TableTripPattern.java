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

package org.opentripplanner.routing.edgetype;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import lombok.Getter;
import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

/**
 * Represents a class of trips distinguished by service id and list of stops. For each stop, there
 * is a list of departure times, running times, arrival times, dwell times, and wheelchair
 * accessibility information (one of each of these per trip per stop). An exemplar trip is also
 * included so that information such as route name can be found. Trips are assumed to be
 * non-overtaking, so that an earlier trip never arrives after a later trip.
 */
public class TableTripPattern implements TripPattern, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TableTripPattern.class);

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    
    public static final int FLAG_WHEELCHAIR_ACCESSIBLE = 1;
    public static final int MASK_PICKUP = 2|4;
    public static final int SHIFT_PICKUP = 1;
    public static final int MASK_DROPOFF = 8|16;
    public static final int SHIFT_DROPOFF = 3;
    public static final int NO_PICKUP = 1;
    public static final int FLAG_BIKES_ALLOWED = 32;

    /** 
     * The GTFS Route of all trips in this pattern. GTFS allows the same pattern to appear in more than one route, 
     * but we make the assumption that all trips with the same pattern belong to the same Route.  
     */
    @Getter 
    public final Route route;
    
    /**
     * All trips in this pattern call at this sequence of stops. This includes information about GTFS
     * pick-up and drop-off types.
     */
    @Getter
    public final StopPattern stopPattern;
    
    /** 
     * This timetable holds the 'official' stop times from GTFS. If realtime stoptime updates are 
     * applied, trips searches will be conducted using another timetable and this one will serve to 
     * find early/late offsets, or as a fallback if the other timetable becomes corrupted or
     * expires.
     */
    @Getter
    protected final Timetable scheduledTimetable = new Timetable(this);

    /** The human-readable, unique name for this trip pattern. */
    @Getter @Setter
    private String name;
    
    /** The short unique identifier for this trip pattern. */
    @Getter @Setter
    private String code;
    
    
    // redundant since tripTimes have a trip
    // however it's nice to have for order reference, since all timetables must have tripTimes
    // in this order, e.g. for interlining. 
    // potential optimization: trip fields can be removed from TripTimes?
    // TODO: this field can be removed, and interlining can be done differently?
    /**
     * This pattern may have multiple Timetable objects, but they should all contain TripTimes
     * for the same trips, in the same order (that of the scheduled Timetable). An exception to 
     * this rule may arise if unscheduled trips are added to a Timetable. For that case we need 
     * to search for trips/TripIds in the Timetable rather than the enclosing TripPattern.  
     */
    final ArrayList<Trip> trips = new ArrayList<Trip>();

    /**
     * An ordered list of related PatternHop. All trips in a pattern have the same stops and a
     * PatternHop apply to all those trips, so this array apply to every trip in every timetable in
     * this pattern. Please note that the array size is the number of stops minus 1. This also allow
     * to access the ordered list of stops.
     * 
     * This appears to only be used for on-board departure. TODO: stops can now be grabbed from stopPattern.
     */
    private PatternHop[] patternHops;

    /** Holds stop-specific information such as wheelchair accessibility and pickup/dropoff roles. */
    @XmlElement int[] perStopFlags;
    
    /** Optimized serviceId code. All trips in a pattern are by definition on the same service. */
    int serviceId; 
    
    public TableTripPattern(Route route, StopPattern stopPattern) {
        this.route = route;
        this.stopPattern = stopPattern;
        setStopsFromStopPattern(stopPattern);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // The serialized graph contains cyclic references TableTripPattern <--> Timetable.
        // The Timetable must be indexed from here (rather than in its own readObject method) 
        // to ensure that the stops field it uses in TableTripPattern is already deserialized.
        finish();
    }
            
    // TODO verify correctness after substitution of StopPattern for ScheduledStopPattern
    // also, maybe get rid of the per stop flags and just use the values in StopPattern, or an Enum
    private void setStopsFromStopPattern(StopPattern stopPattern) {
        patternHops = new PatternHop[stopPattern.size - 1];
        perStopFlags = new int[stopPattern.size];
        int i = 0;
        for (Stop stop : stopPattern.stops) {
            // Assume that stops can be boarded with wheelchairs by default (defer to per-trip data)
            if (stop.getWheelchairBoarding() != 2) {
                perStopFlags[i] |= FLAG_WHEELCHAIR_ACCESSIBLE;
            }
            perStopFlags[i] |= stopPattern.pickups[i] << SHIFT_PICKUP;
            perStopFlags[i] |= stopPattern.dropoffs[i] << SHIFT_DROPOFF;
            ++i;
        }
    }
    
    public Stop getStop(int stopIndex) {
        if (stopIndex == patternHops.length) {
            return patternHops[stopIndex - 1].getEndStop();
        } else {
            return patternHops[stopIndex].getBeginStop();
        }
    }

    public List<Stop> getStops() {
        /*
         * Dynamically build the list from the PatternHop list. Not super efficient but this method
         * is not called very often.
         */
        List<Stop> retval = new ArrayList<Stop>(patternHops.length + 1);
        for (int i = 0; i <= patternHops.length; i++)
            retval.add(getStop(i));
        return retval;
    }
    
    public List<PatternHop> getPatternHops() {
        return Arrays.asList(patternHops);
    }

    /* package private */
    void setPatternHop(int stopIndex, PatternHop patternHop) {
        patternHops[stopIndex] = patternHop;
    }

    @Override
    public int getHopCount() {
        return patternHops.length;
    }

    public Trip getTrip(int tripIndex) {
        return trips.get(tripIndex);
    }
    
    @XmlTransient
    public List<Trip> getTrips() {
        return trips;
    }

    public int getTripIndex(Trip trip) {
        return trips.indexOf(trip);
    }

    /** Returns whether passengers can alight at a given stop */
    public boolean canAlight(int stopIndex) {
        return getAlightType(stopIndex) != NO_PICKUP;
    }

    /** Returns whether passengers can board at a given stop */
    public boolean canBoard(int stopIndex) {
        return getBoardType(stopIndex) != NO_PICKUP;
    }

    /** Returns the zone of a given stop */
    public String getZone(int stopIndex) {
        return getStop(stopIndex).getZoneId();
    }

    @Override
    public int getAlightType(int stopIndex) {
        return (perStopFlags[stopIndex] & MASK_DROPOFF) >> SHIFT_DROPOFF;
    }

    @Override
    public int getBoardType(int stopIndex) {
        return (perStopFlags[stopIndex] & MASK_PICKUP) >> SHIFT_PICKUP;
    }

    /** 
     * Gets the number of scheduled trips on this pattern. Note that when stop time updates are
     * being applied, there may be other Timetables for this pattern which contain a larger number
     * of trips. However, all trips with indexes from 0 through getNumTrips()-1 will always 
     * correspond to the scheduled trips.
     */
    public int getNumScheduledTrips () {
        return trips.size();
    }
    
    // TODO: Lombokize all boilerplate... but lombok does not generate javadoc :/ 
    public int getServiceId() { 
        return serviceId;
    }
    
    /** 
     * Find the next (or previous) departure on this pattern at or after (respectively before) the 
     * specified time. This method will make use of any TimetableResolver present in the 
     * RoutingContext to redirect departure lookups to the appropriate updated Timetable, and will 
     * fall back on the scheduled timetable when no updates are available.
     * @param boarding true means find next departure, false means find previous arrival 
     * @return a TripTimes object providing all the arrival and departure times on the best trip.
     */
    public TripTimes getNextTrip(int stopIndex, int time, State state0, ServiceDay sd,
            boolean haveBicycle, boolean boarding) {
        RoutingRequest options = state0.getOptions();
        Timetable timetable = scheduledTimetable;
        TimetableResolver snapshot = options.rctx.timetableSnapshot;
        if (snapshot != null)
            timetable = snapshot.resolve(this, sd.getServiceDate());
        // check that we can even board/alight the given stop on this pattern with these options
        int mask = boarding ? MASK_PICKUP : MASK_DROPOFF;
        int shift = boarding ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex] & mask) >> shift == NO_PICKUP) {
            return null;
        }
        if (options.wheelchairAccessible && 
           (perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return null;
        }
        // so far so good, delegate to the timetable
        return timetable.getNextTrip(stopIndex, time, state0, sd, haveBicycle, boarding);
    }

    public TripTimes getResolvedTripTimes(int tripIndex, State state0) {
        ServiceDate serviceDate = state0.getServiceDay().getServiceDate();
        RoutingRequest options = state0.getOptions();
        Timetable timetable = scheduledTimetable;
        TimetableResolver snapshot = options.rctx.timetableSnapshot;
        if (snapshot != null) {
            timetable = snapshot.resolve(this, serviceDate);
        }
        return timetable.getTripTimes(tripIndex);
    }

    /* METHODS THAT DELEGATE TO THE SCHEDULED TIMETABLE */

    // TODO: These should probably be deprecated. That would require grabbing the scheduled timetable,
    // and would avoid mistakes where real-time updates are accidentally not taken into account.

    /**
     * Add a trip to this TableTripPattern.
     */
    public void addTrip(Trip trip, List<StopTime> stopTimes) {
        // Only scheduled trips (added via the pattern rather than directly to the timetable) are in the trips list.
        this.trips.add(trip);
        this.scheduledTimetable.addTrip(trip, stopTimes);
        // Check that all trips added to this pattern are on the initially declared route.
        if (this.route != trip.getRoute()){
            // Identity equality is valid on GTFS entity objects
            LOG.warn("The trip {} is on a different route than its stop pattern, which is on {}.", trip, route);
        }
    }

    /** Gets the running time after a given stop (i.e. for the given hop) on a given trip */
    public int getRunningTime(int stopIndex, int trip) {
        return scheduledTimetable.getRunningTime(stopIndex, trip);
    }

    /** @return the index of TripTimes for this Trip(Id) in this particular TableTripPattern */
    public int getTripIndex(AgencyAndId tripId) {
        return scheduledTimetable.getTripIndex(tripId);
    }

    public TripTimes getTripTimes(int tripIndex) {
        return scheduledTimetable.getTripTimes(tripIndex);
    }

    /** Gets the departure time for a given hop on a given trip */
    public int getDepartureTime(int hop, int trip) {
        return scheduledTimetable.getDepartureTime(hop, trip);
    }

    /** Gets the arrival time for a given hop on a given trip */
    public int getArrivalTime(int hop, int trip) {
        return scheduledTimetable.getArrivalTime(hop, trip);
    }

    /** Gets all the departure times at a given stop (not used in routing) */
    public Iterator<Integer> getDepartureTimes(int stopIndex) {
        return scheduledTimetable.getDepartureTimes(stopIndex);
    }

    /** Gets all the arrival times at a given stop (not used in routing) */
    public Iterator<Integer> getArrivalTimes(int stopIndex) {
        return scheduledTimetable.getArrivalTimes(stopIndex);
    }

    /** Returns the shortest possible running time for this stop */
    public int getBestRunningTime(int stopIndex) {
        return scheduledTimetable.getBestRunningTime(stopIndex);
    }

    /** Returns the shortest possible dwell time at this stop */
    public int getBestDwellTime(int stopIndex) {
        return scheduledTimetable.getBestDwellTime(stopIndex);
    }

    /**
     * Finish off a TableTripPattern once all TripTimes have been added to it.
     */
    public void finish() {
        scheduledTimetable.finish();
    }

    /* OTHER METHODS */
    
    /**
     * Rather than the scheduled timetable, get the one that has been updated with real-time updates.
     * The view is consistent across a single request, and depends on the routing context in the request.
     */
    public Timetable getUpdatedTimetable (RoutingRequest req) {
        return null;
    }
    
    private static String stopNameAndId (Stop stop) {
        return stop.getName() + " (" + stop.getId() + ")";
    }


    /**
     * Static method that creates unique human-readable names for a collection of TableTripPatterns.
     * Perhaps this should be in TripPattern, and apply to Frequency patterns as well. TODO: resove
     * this question: can a frequency and table pattern have the same stoppattern? If so should they
     * have the same "unique" name?
     * 
     * The names should be dataset unique, not just route-unique?
     * 
     * A TripPattern groups all trips visiting a particular pattern of stops on a particular route.
     * GFTS Route names are intended for very general customer information, but sometimes there is a
     * need to know where a particular trip actually goes. For example, the New York City N train
     * has at least four different variants: express (over the Manhattan bridge) and local (via
     * lower Manhattan and the tunnel), in two directions (to Astoria or to Coney Island). During
     * construction, a fifth variant sometimes appears: trains use the D line to Coney Island after
     * 59th St (or from Coney Island to 59th in the opposite direction).
     * 
     * TripPattern names are machine-generated on a best-effort basis. They are guaranteed to be
     * unique (among TripPatterns for a single Route) but not stable across graph builds, especially
     * when different versions of GTFS inputs are used. For instance, if a variant is the only
     * variant of the N that ends at Coney Island, the name will be "N to Coney Island". But if
     * multiple variants end at Coney Island (but have different stops elsewhere), that name would
     * not be chosen. OTP also tries start and intermediate stations ("from Coney Island", or "via
     * Whitehall", or even combinations ("from Coney Island via Whitehall"). But if there is no way
     * to create a unique name from start/end/intermediate stops, then the best we can do is to
     * create a "like [trip id]" name, which at least tells you where in the GTFS you can find a
     * related trip.
     */
    // TODO: pass in a transit index that contains a Multimap<Route, TripPattern> and derive all TableTripPatterns
    // TODO: use headsigns before attempting to machine-generate names
    // TODO: combine from/to and via in a single name. this could be accomplished by grouping the trips by destination,
    // then disambiguating in groups of size greater than 1.
    /*
     * Another possible approach: for each route, determine the necessity of each field (which
     * combination will create unique names). from, to, via, express. Then concatenate all necessary
     * fields. Express should really be determined from number of stops and/or run time of trips.
     */
    public static void generateUniqueNames (Collection<TableTripPattern> tableTripPatterns) {
        Set<String> usedRouteNames = Sets.newHashSet();
        Map<Route, String> uniqueRouteNames = Maps.newHashMap();

        /* Group TripPatterns by Route */
        Multimap<Route, TableTripPattern> patternsByRoute = ArrayListMultimap.create();
        for (TableTripPattern ttp : tableTripPatterns) {
            patternsByRoute.put(ttp.route, ttp);
        }

        /* Ensure we have a unique name for every Route */
        for (Route route : patternsByRoute.keys()) {
            String routeName = GtfsLibrary.getRouteName(route);
            if (usedRouteNames.contains(routeName)) {
                LOG.warn("Route has non-unique name. Generating one to ensure uniqueness of TripPattern names.");
                int i = 2;
                String generatedRouteName;
                do generatedRouteName = routeName + (i++);
                while (usedRouteNames.contains(generatedRouteName));
                routeName = generatedRouteName;
            }
            usedRouteNames.add(routeName);
            uniqueRouteNames.put(route, routeName);
        }
        
        /* Iterate over all routes, giving the patterns within each route unique names. */
        ROUTE : for (Route route : patternsByRoute.keys()) {
            Collection<TableTripPattern> tripPatterns = patternsByRoute.get(route);             
            String routeName = uniqueRouteNames.get(route);

            /* Simplest case: there's only one route variant, so we'll just give it the route's name. */
            if (tripPatterns.size() == 1) {
                tripPatterns.iterator().next().setName(routeName);
                continue;
            }

            /* Do the patterns within this Route have a unique start, end, or via Stop? */
            Multimap<String, TableTripPattern> starts = ArrayListMultimap.create();
            Multimap<String, TableTripPattern> ends   = ArrayListMultimap.create();
            Multimap<String, TableTripPattern> vias   = ArrayListMultimap.create();
            for (TableTripPattern pattern : tableTripPatterns) {
                List<Stop> stops = pattern.getStops();
                starts.put(stopNameAndId(stops.get(0)), pattern);
                ends.put(stopNameAndId(stops.get(stops.size() - 1)), pattern);
                for (Stop stop : stops) {
                    vias.put(stopNameAndId(stop), pattern);
                }
            }
            PATTERN : for (TableTripPattern pattern : tableTripPatterns) {
                List<Stop> stops = pattern.getStops();
                String start = stopNameAndId(stops.get(0));
                if (starts.get(start).size() == 1) {
                    pattern.setName(routeName + " from " + start);
                    continue PATTERN; // this is the only pattern with this first stop
                }
                String end = stopNameAndId(stops.get(stops.size() - 1));
                if (ends.get(end).size() == 1) {
                    pattern.setName(routeName + " to " + end);
                    continue PATTERN; // this is the only pattern with this last stop
                } 
                // TODO favor stops with the most transfers when choosing "via"
                for (Stop stop : stops) {
                    String via = stopNameAndId(stop);
                    if (vias.get(via).size() == 1) {
                        pattern.setName(routeName + " via " + via);
                        continue PATTERN; // this is the only pattern via this stop
                    }
                }
                /**
                 * now we have the case where no route has a unique start, stop, or via. This can happen
                 * if you have a single route which serves trips on an H-shaped alignment, where trips
                 * can start at A or B and end at either C or D, visiting the same sets of stops along
                 * the shared segments.
                 * 
                 * <pre>
                 *                    A      B
                 *                    |      |
                 *                    |------|
                 *                    |      |
                 *                    |      |
                 *                    C      D
                 * </pre>
                 * 
                 * First, we try unique start + end, then start + via + end, and if that doesn't work,
                 * we check for expresses, and finally we use a random trip's id.
                 * 
                 * It can happen if there is an express and a local version of a given line where the
                 * local starts and ends at the same place as the express but makes a strict superset of
                 * stops; the local version will get a "via", but the express will be doomed.
                 * 
                 * We can first check for the local/express situation by saying that if there are a
                 * subset of routes with the same start/end, and there is exactly one that can't be
                 * named with start/end/via, call it "express".
                 * 
                 * Consider the following three trips (A, B, C) along a route with four stops. A is the
                 * local, and gets "via stop 3"; B is a limited, and C is (logically) an express:
                 * 
                 * A,B,C -- A,B -- A -- A, B, C
                 * 
                 * Here, neither B nor C is nameable. If either were removed, the other would be called
                 * "express".
                 */

                /* Find all patterns on this route with the same first and last stop as this one. */
                Set<TableTripPattern> remainingPatterns = Sets.newHashSet();
                remainingPatterns.addAll(starts.get(start));
                remainingPatterns.retainAll(ends.get(end));
                if (remainingPatterns.size() == 1) {
                    pattern.setName(routeName + " from " + start + " to " + end);
                    continue PATTERN;
                }
                /* Still not unique; try (start, via, end) for each via. */
                for (Stop stop : stops) {
                    String via = stopNameAndId(stop);
                    if (via.equals(start) || via.equals(end)) continue;
                    boolean found = false;
                    boolean bad = false;
                    for (TableTripPattern viaPattern : vias.get(via)) {
                        if (remainingPatterns.contains(viaPattern)) {
                            if (found) {
                                bad = true;
                                break;
                            } else {
                                found = true;
                            }
                        }
                    }
                    if (found && !bad) {
                        String name = routeName + " from " + start + " to " + end + " via " + via;
                        pattern.setName(name);
                        continue PATTERN;
                    }
                }
                /* Still not unique; check for express. */
                if (remainingPatterns.size() == 2) {
                    // There are exactly two patterns sharing this start/end.
                    // The current one must be a subset of the other, because it has no unique via. 
                    // Therefore we call it the express.
                    String name = routeName + " from " + start + " to " + end + " express";
                    pattern.setName(name);
                } else {
                    /* The final fallback: reference a specific trip ID. */
                    pattern.setName(routeName + " like trip " + pattern.getTrips().get(0).getId());
                }
            } // END foreach PATTERN
        } // END foreach ROUTE

    }

}
