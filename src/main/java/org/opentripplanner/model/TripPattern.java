package org.opentripplanner.model;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.CompactLineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NonUniqueRouteName;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a group of trips on a route, with the same direction id that all call at the same
 * sequence of stops. For each stop, there is a list of departure times, running times, arrival
 * times, dwell times, and wheelchair accessibility information (one of each of these per trip per
 * stop).
 * Trips are assumed to be non-overtaking, so that an earlier trip never arrives after a later trip.
 *
 * This is called a JOURNEY_PATTERN in the Transmodel vocabulary. However, GTFS calls a Transmodel JOURNEY a "trip",
 * thus TripPattern.
 * <p>
 * The {@code id} is a unique identifier for this trip pattern. For GTFS feeds this is generally
 * generated in the format FeedId:Agency:RouteId:DirectionId:PatternNumber. For NeTEx the
 * JourneyPattern id is used.
 */
public class TripPattern extends TransitEntity implements Cloneable, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

    private static final long serialVersionUID = 1;

    private String name;

    private final Route route;

    private final StopPattern stopPattern;

    private final Timetable scheduledTimetable = new Timetable(this);

    /**
     * Geometries of each inter-stop segment of the tripPattern.
     */
    private byte[][] hopGeometries = null;

    /**
     * The original TripPattern this replaces at least for one modified trip.
     */
    private TripPattern originalTripPattern = null;

    /**
     * Has the TripPattern been created by a real-time update.
     */
    private boolean createdByRealtimeUpdater = false;

    // TODO MOVE codes INTO Timetable or TripTimes
    private BitSet services;


    public TripPattern(FeedScopedId id, Route route, StopPattern stopPattern) {
        super(id);
        this.route = route;
        this.stopPattern = stopPattern;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Convinience method to get the route traverse mode, the mode for all trips in this pattern.
     */
    public final TransitMode getMode() {
        return route.getMode();
    }

    public LineString getHopGeometry(int stopIndex) {
        if (hopGeometries != null) {
            return CompactLineString.uncompactLineString(
                    hopGeometries[stopIndex],
                    false
            );
        } else {
            return GeometryUtils.getGeometryFactory().createLineString(
                    new Coordinate[]{
                            coordinate(stopPattern.getStops()[stopIndex]),
                            coordinate(stopPattern.getStops()[stopIndex + 1])
                    }
            );
        }
    }

    public void setHopGeometries(LineString[] hopGeometries) {
        this.hopGeometries = new byte[hopGeometries.length][];

        for (int i = 0; i < hopGeometries.length; i++) {
            setHopGeometry(i, hopGeometries[i]);
        }
    }

    public void setHopGeometry(int i, LineString hopGeometry) {
        this.hopGeometries[i] = CompactLineString.compactLineString(hopGeometry,false);
    }

    /**
     * This will copy the geometry from another TripPattern to this one. It checks if each hop is
     * between the same stops before copying that hop geometry. If the stops are different, a
     * straight-line hop-geometry will be used instead.
     *
     * @param other TripPattern to copy geometry from
     */
    public void setHopGeometriesFromPattern(TripPattern other) {
        this.hopGeometries = new byte[this.getStops().size() - 1][];

        // This accounts for the new TripPattern provided by a real-time update and the one that is
        // being replaced having a different number of stops. In that case the geometry will be
        // preserved up until the first mismatching stop, and a straight line will be used for
        // all segments after that.
        int sizeOfShortestPattern = Math.min(this.getStops().size(), other.getStops().size());

        for (int i = 0; i < sizeOfShortestPattern - 1; i++) {
            if (other.getHopGeometry(i) != null
                && other.getStop(i).equals(this.getStop(i))
                && other.getStop(i + 1).equals(this.getStop(i + 1))) {
                // Copy hop geometry from previous pattern
                this.setHopGeometry(i, other.getHopGeometry(i));
            } else {
                // Create new straight-line geometry for hop
                this.setHopGeometry(i,
                    GeometryUtils.getGeometryFactory().createLineString(
                        new Coordinate[]{
                            coordinate(getStopPattern().getStops()[i]),
                            coordinate(getStopPattern().getStops()[i + 1])
                        }
                    )
                );
            }
        }
    }

    public LineString getGeometry() {
        if(hopGeometries == null || hopGeometries.length==0) { return null; }

        List<LineString> lineStrings = new ArrayList<>();
        for (int i = 0; i < hopGeometries.length; i++) {
            lineStrings.add(getHopGeometry(i));
        }
        return GeometryUtils.concatenateLineStrings(lineStrings);
    }

    public int numHopGeometries() {
        return hopGeometries.length;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // The serialized graph contains cyclic references TripPattern <--> Timetable.
        // The Timetable must be indexed from here (rather than in its own readObject method)
        // to ensure that the stops field it uses in TripPattern is already deserialized.
        scheduledTimetable.finish();
    }

    public Stop getStop(int stopIndex) {
        return stopPattern.getStops()[stopIndex];
    }


    public int getStopIndex(Stop stop) {
        return Arrays.asList(stopPattern.getStops()).indexOf(stop);
    }

    public List<Stop> getStops() {
        return Arrays.asList(stopPattern.getStops());
    }

    public Trip getTrip(int tripIndex) {
        return getTrips().get(tripIndex);
    }

    public int getTripIndex(Trip trip) {
        return getTrips().indexOf(trip);
    }

    /** Returns whether passengers can alight at a given stop */
    public boolean canAlight(int stopIndex) {
        return stopPattern.getDropoff(stopIndex).isRoutable();
    }

    /** Returns whether passengers can board at a given stop */
    public boolean canBoard(int stopIndex) {
        return stopPattern.getPickup(stopIndex).isRoutable();
    }

    /** Returns whether a given stop is wheelchair-accessible. */
    public boolean wheelchairAccessible(int stopIndex) {
        return stopPattern.getStop(stopIndex).getWheelchairBoarding() == WheelChairBoarding.POSSIBLE;
    }

    /** Returns the zone of a given stop */
    public String getZone(int stopIndex) {
        return getStop(stopIndex).getFirstZoneAsString();
    }

    public PickDrop getAlightType(int stopIndex) {
        return stopPattern.getDropoff(stopIndex);
    }

    public PickDrop getBoardType(int stopIndex) {
        return stopPattern.getPickup(stopIndex);
    }

    /* METHODS THAT DELEGATE TO THE SCHEDULED TIMETABLE */

    // TODO: These should probably be deprecated. That would require grabbing the scheduled timetable,
    // and would avoid mistakes where real-time updates are accidentally not taken into account.

    /**
     * Add the given tripTimes to this pattern's scheduled timetable, recording the corresponding
     * trip as one of the scheduled trips on this pattern.
     */
    public void add(TripTimes tt) {
        // Only scheduled trips (added at graph build time, rather than directly to the timetable via updates) are in this list.
        getTrips().add(tt.getTrip());
        scheduledTimetable.addTripTimes(tt);
        // Check that all trips added to this pattern are on the initially declared route.
        // Identity equality is valid on GTFS entity objects.
        if (this.route != tt.getTrip().getRoute()) {
            LOG.warn("The trip {} is on route {} but its stop pattern is on route {}.",
                tt.getTrip(), tt.getTrip().getRoute(),
                route
            );
        }
    }

    /**
     * Add the given FrequencyEntry to this pattern's scheduled timetable, recording the corresponding
     * trip as one of the scheduled trips on this pattern.
     * TODO possible improvements: combine freq entries and TripTimes. Do not keep trips list in TripPattern
     * since it is redundant.
     */
    public void add(FrequencyEntry freq) {
        getTrips().add(freq.tripTimes.getTrip());
        scheduledTimetable.addFrequencyEntry(freq);
        if (this.getRoute() != freq.tripTimes.getTrip().getRoute()) {
            LOG.warn("The trip {} is on a different route than its stop pattern, which is on {}.",
                freq.tripTimes.getTrip(),
                route
            );
        }
    }

    /**
     * Remove all trips matching the given predicate.
     * @param removeTrip it the predicate returns true
     */
    public void removeTrips(Predicate<Trip> removeTrip) {
        getTrips().removeIf(removeTrip);
        if(getTrips().isEmpty()) {
            scheduledTimetable.getTripTimes().clear();
        }
        else {
            scheduledTimetable.getTripTimes().removeIf(tt -> removeTrip.test(tt.getTrip()));
        }
    }

    public TripPattern getOriginalTripPattern() {
        return originalTripPattern;
    }

    public void setOriginalTripPattern(TripPattern originalTripPattern) {
        this.originalTripPattern = originalTripPattern;
    }

    /**
     * The direction for all the trips in this pattern.
     */
    public Direction getDirection() {
        return getTrips().get(0).getDirection();
    }

    /**
     * This pattern may have multiple Timetable objects, but they should all contain TripTimes
     * for the same trips, in the same order (that of the scheduled Timetable). An exception to
     * this rule may arise if unscheduled trips are added to a Timetable. For that case we need
     * to search for trips/TripIds in the Timetable rather than the enclosing TripPattern.
     */
    public List<Trip> getTrips() {
        return scheduledTimetable.getTripTimes().stream().map(t -> t.getTrip()).collect(Collectors.toList());
    }

    /** The human-readable, unique name for this trip pattern. */
    public String getName() {
        return name;
    }

    /**
     * The GTFS Route of all trips in this pattern.
     */
    public Route getRoute() {
        return route;
    }

    /**
     * All trips in this pattern call at this sequence of stops. This includes information about GTFS
     * pick-up and drop-off types.
     */
    public StopPattern getStopPattern() {
        return stopPattern;
    }

    /**
     * This is the "original" timetable holding the scheduled stop times from GTFS, with no
     * realtime updates applied. If realtime stoptime updates are applied, next/previous departure
     * searches will be conducted using a different, updated timetable in a snapshot.
     */
    public Timetable getScheduledTimetable() {
        return scheduledTimetable;
    }

    boolean isCreatedByRealtimeUpdater() {
        return createdByRealtimeUpdater;
    }

    public void setCreatedByRealtimeUpdater() {
        createdByRealtimeUpdater = true;
    }

    private static String stopNameAndId (Stop stop) {
        return stop.getName() + " (" + stop.getId().toString() + ")";
    }

    /**
     * Static method that creates unique human-readable names for a collection of TableTripPatterns.
     * Perhaps this should be in TripPattern, and apply to Frequency patterns as well. TODO: resolve
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
    public static void generateUniqueNames (
            Collection<TripPattern> tableTripPatterns,
            DataImportIssueStore issueStore
    ) {
        LOG.info("Generating unique names for stop patterns on each route.");
        Set<String> usedRouteNames = Sets.newHashSet();
        Map<Route, String> uniqueRouteNames = Maps.newHashMap();

        /* Group TripPatterns by Route */
        Multimap<Route, TripPattern> patternsByRoute = ArrayListMultimap.create();
        for (TripPattern ttp : tableTripPatterns) {
            patternsByRoute.put(ttp.route, ttp);
        }

        /* Ensure we have a unique name for every Route */
        for (Route route : patternsByRoute.keySet()) {
            String routeName = route.getName();
            if (usedRouteNames.contains(routeName)) {
                int i = 2;
                String generatedRouteName;
                do generatedRouteName = routeName + " " + (i++);
                while (usedRouteNames.contains(generatedRouteName));
                issueStore.add(new NonUniqueRouteName(generatedRouteName));
                routeName = generatedRouteName;
            }
            usedRouteNames.add(routeName);
            uniqueRouteNames.put(route, routeName);
        }

        /* Iterate over all routes, giving the patterns within each route unique names. */
        ROUTE : for (Route route : patternsByRoute.keySet()) {
            Collection<TripPattern> routeTripPatterns = patternsByRoute.get(route);
            String routeName = uniqueRouteNames.get(route);

            /* Simplest case: there's only one route variant, so we'll just give it the route's name. */
            if (routeTripPatterns.size() == 1) {
                routeTripPatterns.iterator().next().setName(routeName);
                continue;
            }

            /* Do the patterns within this Route have a unique start, end, or via Stop? */
            Multimap<String, TripPattern> signs   = ArrayListMultimap.create(); // prefer headsigns
            Multimap<Stop, TripPattern> starts  = ArrayListMultimap.create();
            Multimap<Stop, TripPattern> ends    = ArrayListMultimap.create();
            Multimap<Stop, TripPattern> vias    = ArrayListMultimap.create();

            for (TripPattern pattern : routeTripPatterns) {
                List<Stop> stops = pattern.getStops();
                Stop start = stops.get(0);
                Stop end   = stops.get(stops.size() - 1);
                starts.put(start, pattern);
                ends.put(end, pattern);
                for (Stop stop : stops) vias.put(stop, pattern);
            }
            PATTERN : for (TripPattern pattern : routeTripPatterns) {
                List<Stop> stops = pattern.getStops();
                StringBuilder sb = new StringBuilder(routeName);

                /* First try to name with destination. */
                Stop end = stops.get(stops.size() - 1);
                sb.append(" to " + stopNameAndId(end));
                if (ends.get(end).size() == 1) {
                    pattern.setName(sb.toString());
                    continue PATTERN; // only pattern with this last stop
                }

                /* Then try to name with origin. */
                Stop start = stops.get(0);
                sb.append(" from " + stopNameAndId(start));
                if (starts.get(start).size() == 1) {
                    pattern.setName((sb.toString()));
                    continue PATTERN; // only pattern with this first stop
                }

                /* Check whether (end, start) is unique. */
                Collection<TripPattern> tripPatterns = starts.get(start);
                Set<TripPattern> remainingPatterns = new HashSet<>(tripPatterns);
                remainingPatterns.retainAll(ends.get(end)); // set intersection
                if (remainingPatterns.size() == 1) {
                    pattern.setName((sb.toString()));
                    continue PATTERN;
                }

                /* Still not unique; try (end, start, via) for each via. */
                for (Stop via : stops) {
                    if (via.equals(start) || via.equals(end)) continue;
                    Set<TripPattern> intersection = new HashSet<>();
                    intersection.addAll(remainingPatterns);
                    intersection.retainAll(vias.get(via));
                    if (intersection.size() == 1) {
                        sb.append(" via " + stopNameAndId(via));
                        pattern.setName((sb.toString()));
                        continue PATTERN;
                    }
                }

                /* Still not unique; check for express. */
                if (remainingPatterns.size() == 2) {
                    // There are exactly two patterns sharing this start/end.
                    // The current one must be a subset of the other, because it has no unique via.
                    // Therefore we call it the express.
                    sb.append(" express");
                } else {
                    // The final fallback: reference a specific trip ID.
                    sb.append(" like trip " + pattern.getTrips().get(0).getId());
                }
                pattern.setName((sb.toString()));
            } // END foreach PATTERN
        } // END foreach ROUTE

        if (LOG.isDebugEnabled()) {
            LOG.debug("Done generating unique names for stop patterns on each route.");
            for (Route route : patternsByRoute.keySet()) {
                Collection<TripPattern> routeTripPatterns = patternsByRoute.get(route);
                LOG.debug("Named {} patterns in route {}", routeTripPatterns.size(), uniqueRouteNames.get(route));
                for (TripPattern pattern : routeTripPatterns) {
                    LOG.debug("    {} ({} stops)", pattern.name, pattern.stopPattern.getSize());
                }
            }
        }
    }

    /**
     * A bit of a strange place to set service codes all at once when TripTimes are already added,
     * but we need a reference to the Graph or at least the codes map. This could also be
     * placed in the hop factory itself.
     */
    public void setServiceCodes (Map<FeedScopedId, Integer> serviceCodes) {
        setServices(new BitSet());
        for (Trip trip : getTrips()) {
            FeedScopedId serviceId = trip.getServiceId();
            if (serviceCodes.containsKey(serviceId)) {
                services.set(serviceCodes.get(serviceId));
            }
            else {
                LOG.warn("Service " + serviceId + " not found in service codes not found.");
            }
        }
        scheduledTimetable.setServiceCodes (serviceCodes);
    }

    /**
     * A set of serviceIds with at least one trip in this pattern.
     * Trips in a pattern are no longer necessarily running on the same service ID.
     */ /**
     * @return bitset of service codes
     */
    public BitSet getServices() {
        return services;
    }

    /**
     * @param services bitset of service codes
     */
    public void setServices(BitSet services) {
        this.services = services;
    }

    public String getTripHeadsign() {
        return getTrips().get(0).getTripHeadsign();
    }

    public static boolean idsAreUniqueAndNotNull(Collection<TripPattern> tripPatterns) {
        Set<FeedScopedId> seen = new HashSet<>();
        return tripPatterns.stream()
            .map(TransitEntity::getId)
            .allMatch(t -> t != null && seen.add(t));
    }

    public String toString () {
        return String.format("<TripPattern %s>", this.getId());
    }

    /**
     * In most cases we want to use identity equality for Trips.
     * However, in some cases we want a way to consistently identify trips across versions of a GTFS feed, when the
     * feed publisher cannot ensure stable trip IDs. Therefore we define some additional hash functions.
     * Hash collisions are theoretically possible, so these identifiers should only be used to detect when two
     * trips are the same with a high degree of probability.
     * An example application is avoiding double-booking of a particular bus trip for school field trips.
     * Using Murmur hash function. see http://programmers.stackexchange.com/a/145633 for comparison.
     *
     * @param trip a trip object within this pattern, or null to hash the pattern itself independent any specific trip.
     * @return the semantic hash of a Trip in this pattern as a printable String.
     *
     * TODO deal with frequency-based trips
     */
    public String semanticHashString(Trip trip) {
        HashFunction murmur = Hashing.murmur3_32();
        BaseEncoding encoder = BaseEncoding.base64Url().omitPadding();
        StringBuilder sb = new StringBuilder(50);
        sb.append(encoder.encode(stopPattern.semanticHash(murmur).asBytes()));
        if (trip != null) {
            TripTimes tripTimes = scheduledTimetable.getTripTimes(trip);
            if (tripTimes == null) { return null; }
            sb.append(':');
            sb.append(encoder.encode(tripTimes.semanticHash(murmur).asBytes()));
        }
        return sb.toString();
    }

    public TripPattern clone () {
        try {
            return (TripPattern) super.clone();
        } catch (CloneNotSupportedException e) {
            /* cannot happen */
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the feed id this trip pattern belongs to.
     *
     * @return feed id for this trip pattern
     */
    public String getFeedId() {
        // The feed id is the same as the agency id on the route, this allows us to obtain it from there.
        return route.getId().getFeedId();
    }

    private static Coordinate coordinate(Stop s) {
        return new Coordinate(s.getLon(), s.getLat());
    }
}
