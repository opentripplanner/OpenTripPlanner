package org.opentripplanner.model;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
public final class TripPattern extends TransitEntity implements Cloneable, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

    private static final long serialVersionUID = 1;

    private String name;

    private final Route route;

    /**
     * The stop-pattern help us reuse the same stops in several trip-patterns; Hence
     * saving memory. The field should not be accessible outside the class, and all access
     * is done through method delegation, like the {@link #numberOfStops()} and
     * {@link #canBoard(int)} methods.
     */
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

    /** The human-readable, unique name for this trip pattern. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The GTFS Route of all trips in this pattern.
     */
    public Route getRoute() {
        return route;
    }

    /**
     * Convenience method to get the route traverse mode, the mode for all trips in this pattern.
     */
    public TransitMode getMode() {
        return route.getMode();
    }

    public final String getNetexSubmode() {
        return route.getNetexSubmode();
    }

    public LineString getHopGeometry(int stopPosInPattern) {
        if (hopGeometries != null) {
            return CompactLineString.uncompactLineString(
                    hopGeometries[stopPosInPattern],
                    false
            );
        } else {
            return GeometryUtils.getGeometryFactory().createLineString(
                    new Coordinate[]{
                            coordinate(stopPattern.getStop(stopPosInPattern)),
                            coordinate(stopPattern.getStop(stopPosInPattern + 1))
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
        this.hopGeometries = new byte[numberOfStops() - 1][];

        // This accounts for the new TripPattern provided by a real-time update and the one that is
        // being replaced having a different number of stops. In that case the geometry will be
        // preserved up until the first mismatching stop, and a straight line will be used for
        // all segments after that.
        int sizeOfShortestPattern = Math.min(numberOfStops(), other.numberOfStops());

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
                            coordinate(stopPattern.getStop(i)),
                            coordinate(stopPattern.getStop(i + 1))
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

    public int numberOfStops() {
        return stopPattern.getSize();
    }

    public StopLocation getStop(int stopPosInPattern) {
        return stopPattern.getStop(stopPosInPattern);
    }

    public StopLocation firstStop() {
        return getStop(0);
    }

    public StopLocation lastStop() {
        return getStop(stopPattern.getSize()-1);
    }

    /** Read only list of stops */
    public List<StopLocation> getStops() {
        return stopPattern.getStops();
    }

    /**
     * Find the first stop position in pattern matching the given {@code stop}. The search start at
     * position {@code 0}. Return a negative number if not found. Use {@link
     * #findAlightStopPositionInPattern(StopLocation)} or {@link #findBoardingStopPositionInPattern(StopLocation)}
     * if possible.
     */
    public int findStopPosition(StopLocation stop) {
        return stopPattern.findStopPosition(stop);
    }

    /**
     * Find the first stop position in pattern matching the given {@code station} where it is
     * allowed to board. The search start at position {@code 0}. Return a negative number if not
     * found.
     */
    public int findBoardingStopPositionInPattern(Station station) {
        return stopPattern.findBoardingPosition(station);
    }

    /**
     * Find the first stop position in pattern matching the given {@code station} where it is
     * allowed to alight. The search start at position {@code 1}. Return a negative number if not
     * found.
     */
    public int findAlightStopPositionInPattern(Station station) {
        return stopPattern.findAlightPosition(station);
    }

    /**
     * Find the first stop position in pattern matching the given {@code stop} where it is allowed
     * to board. The search start at position {@code 0}. Return a negative number if not found.
     */
    public int findBoardingStopPositionInPattern(StopLocation stop) {
        return stopPattern.findBoardingPosition(stop);
    }

    /**
     * Find the first stop position in pattern matching the given {@code stop} where it is allowed
     * to alight. The search start at position {@code 1}. Return a negative number if not found.
     */
    public int findAlightStopPositionInPattern(StopLocation stop) {
        return stopPattern.findAlightPosition(stop);
    }

    /** Returns whether passengers can alight at a given stop */
    public boolean canAlight(int stopIndex) {
        return stopPattern.canAlight(stopIndex);
    }

    /** Returns whether passengers can board at a given stop */
    public boolean canBoard(int stopIndex) {
        return stopPattern.canBoard(stopIndex);
    }

    /**
     * Returns whether passengers can board at a given stop.
     * This is an inefficient method iterating over the stops, do not use it in routing.
     */
    public boolean canBoard(StopLocation stop) {
        return stopPattern.canBoard(stop);
    }

    /** Returns whether a given stop is wheelchair-accessible. */
    public boolean wheelchairAccessible(int stopIndex) {
        return stopPattern.getStop(stopIndex).getWheelchairBoarding() == WheelChairBoarding.POSSIBLE;
    }

    public PickDrop getAlightType(int stopIndex) {
        return stopPattern.getDropoff(stopIndex);
    }

    public PickDrop getBoardType(int stopIndex) {
        return stopPattern.getPickup(stopIndex);
    }

    public boolean isBoardAndAlightAt(int stopIndex, PickDrop value) {
        return getBoardType(stopIndex).is(value) && getAlightType(stopIndex).is(value);
    }

    public boolean stopPatternIsEqual(TripPattern other) {
        return stopPattern.equals(other.stopPattern);
    }

    public Trip getTrip(int tripIndex) {
        return scheduledTimetable.getTripTimes(tripIndex).getTrip();
    }

    /* METHODS THAT DELEGATE TO THE SCHEDULED TIMETABLE */

    // TODO: These should probably be deprecated. That would require grabbing the scheduled timetable,
    // and would avoid mistakes where real-time updates are accidentally not taken into account.

    /**
     * Add the given tripTimes to this pattern's scheduled timetable, recording the corresponding
     * trip as one of the scheduled trips on this pattern.
     */
    public void add(TripTimes tt) {
        // Only scheduled trips (added at graph build time, rather than directly to the timetable
        // via updates) are in this list.
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
        scheduledTimetable.getTripTimes().removeIf(tt -> removeTrip.test(tt.getTrip()));
    }

    public void setOriginalTripPattern(TripPattern originalTripPattern) {
        this.originalTripPattern = originalTripPattern;
    }

    /**
     * The direction for all the trips in this pattern.
     */
    public Direction getDirection() {
        return scheduledTimetable.getDirection();
    }

    /**
     * This pattern may have multiple Timetable objects, but they should all contain TripTimes
     * for the same trips, in the same order (that of the scheduled Timetable). An exception to
     * this rule may arise if unscheduled trips are added to a Timetable. For that case we need
     * to search for trips/TripIds in the Timetable rather than the enclosing TripPattern.
     */
    public Stream<Trip> scheduledTripsAsStream() {
        return scheduledTimetable.getTripTimes().stream().map(TripTimes::getTrip).distinct();
    }

    /**
     * This is the "original" timetable holding the scheduled stop times from GTFS, with no
     * realtime updates applied. If realtime stoptime updates are applied, next/previous departure
     * searches will be conducted using a different, updated timetable in a snapshot.
     */
    public Timetable getScheduledTimetable() {
        return scheduledTimetable;
    }

    /**
     * Has the TripPattern been created by a real-time update.
     */
    public boolean isCreatedByRealtimeUpdater() {
        return createdByRealtimeUpdater;
    }

    public void setCreatedByRealtimeUpdater() {
        createdByRealtimeUpdater = true;
    }

    private static String stopNameAndId (StopLocation stop) {
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
            Multimap<StopLocation, TripPattern> starts  = ArrayListMultimap.create();
            Multimap<StopLocation, TripPattern> ends    = ArrayListMultimap.create();
            Multimap<StopLocation, TripPattern> vias    = ArrayListMultimap.create();

            for (TripPattern pattern : routeTripPatterns) {
                StopLocation start = pattern.firstStop();
                StopLocation end   = pattern.lastStop();
                starts.put(start, pattern);
                ends.put(end, pattern);
                for (StopLocation stop : pattern.getStops()) {
                    vias.put(stop, pattern);
                }
            }
            PATTERN : for (TripPattern pattern : routeTripPatterns) {
                StringBuilder sb = new StringBuilder(routeName);

                /* First try to name with destination. */
                var end = pattern.lastStop();
                sb.append(" to " + stopNameAndId(end));
                if (ends.get(end).size() == 1) {
                    pattern.setName(sb.toString());
                    continue PATTERN; // only pattern with this last stop
                }

                /* Then try to name with origin. */
                var start = pattern.firstStop();
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
                for (var via : pattern.getStops()) {
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
                    Trip trip = null;
                    if (!pattern.scheduledTimetable.getTripTimes().isEmpty()) {
                        trip = pattern.scheduledTimetable.getTripTimes().get(0).getTrip();
                    } else if (!pattern.scheduledTimetable.getFrequencyEntries().isEmpty()) {
                        trip = pattern.scheduledTimetable.getFrequencyEntries().get(0).tripTimes.getTrip();
                    }

                    if (trip != null) {
                        sb.append(" like trip ").append(trip.getId());
                    }

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
        scheduledTripsAsStream().forEach (trip -> {
            FeedScopedId serviceId = trip.getServiceId();
            if (serviceCodes.containsKey(serviceId)) {
                services.set(serviceCodes.get(serviceId));
            }
            else {
                LOG.warn("Service " + serviceId + " not found in service codes not found.");
            }
        });
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
        return scheduledTimetable.getTripTimes(0).getTrip().getTripHeadsign();
    }

    public static boolean idsAreUniqueAndNotNull(Collection<TripPattern> tripPatterns) {
        Set<FeedScopedId> seen = new HashSet<>();
        return tripPatterns.stream()
            .map(TransitEntity::getId)
            .allMatch(t -> t != null && seen.add(t));
    }

    public boolean matchesModeOrSubMode(TransitMode mode, String transportSubmode) {
        return getMode().equals(mode) || (
                getNetexSubmode() != null && getNetexSubmode().equals(transportSubmode)
        );
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // The serialized graph contains cyclic references TripPattern <--> Timetable.
        // The Timetable must be indexed from here (rather than in its own readObject method)
        // to ensure that the stops field it uses in TripPattern is already deserialized.
        scheduledTimetable.finish();
    }

    private static Coordinate coordinate(StopLocation s) {
        return new Coordinate(s.getLon(), s.getLat());
    }
}
