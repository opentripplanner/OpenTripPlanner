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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * A PatternInterlineDwell refers to "interlining", where a single physical vehicle carries out several logical trips
 * and a passenger is optionally allowed to remain on the vehicle as it passes from one trip to the other. Interlining
 * is inferred from block IDs in GTFS input. We do not represent cases where the user is not allowed to remain on the
 * vehicle, as these are only interesting from an operations perspective and not for passenger information.
 *
 * Interline dwell info could be stored in various places, with various advantages and disadvantages:
 *
 * A. In the dwell edge itself (where it is now located). This will get in the way of realtime updates and the
 *    eventual elimination of transit edges.
 * B. In TripTimes. This has the disadvantage of leaving null fields in every TripTimes where interlining does not
 *    happen, but the null field could be a pointer to a compound type so it doesn't waste too much space.
 * C. In Timetables. Like TripTimes, this option also allows for real-time updates to interlining information.
 * D. TripPatterns. This does not allow full real-time updates to block and interlining behavior.
 *
 * Another option is to store the Trip->Trip mapping at the Graph level, and use the source and target vertices of the
 * interline dwell edges to know which pattern should be used for resolving TripTimes. However, this will get in the
 * way of eventually eliminating transit edges. This could be a Guava BiMap<Trip, Trip>.
 *
 * Links to previous and next trips could be stored directly as fields in TripTimes, or in a map.
 * Previous/next TripTimes in fields will not work because real-time updates to any one TripTimes will
 * "infect" all other TripTimes in its block.
 * Previous/next (Pattern, Trip) tuples in fields will work because the realtime lookup can be performed on the fly.
 * In maps, the trips do not need to be grouped by service ID because each trip exists on only a single service ID.
 *
 * The "previous" and "next" targets in interline dwell information could be:
 * A. TripTimes. This requires refreshing the TripTimes pointers when committing realtime updates to ensure that they
 *    do not point to scheduled or outdated TripTimes. However, TripTimes do not currently store which TripPattern
 *    they belong to, which could interfere with realtime lookup.
 * B. Tuples of (TripPattern, Trip). This requires resolving the actual TripTimes object at runtime in case there are
 *    real-time updates.
 *
 * Storing pointers directly to other TripTimes in prev/next fields in TripTimes prevents those TripTimes from being
 * independent of one another. An update to one TripTimes will "infect" the entire block it belongs to because the
 * prev/next links are bidirectional.
 *
 * Interlining info (Patterns, Trips) can either be stored locally (in the TripPatterns/Timetables) or nonlocally (at the Graph level).
 */
public class PatternInterlineDwell extends Edge implements OnboardEdge {

    private static final Logger LOG = LoggerFactory.getLogger(PatternInterlineDwell.class);

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    /* Interlining relationships between trips. This could actually be a single Graph-wide BiMap. */
    final BiMap<Trip,Trip> trips = HashBiMap.create();

    public PatternInterlineDwell(TripPattern p0, TripPattern p1) {
        // The dwell actually connects the _arrival_ at the last stop of the first pattern
        // to the _departure_ of the first stop of the second pattern.
        // The last stop of the first pattern does not even have a _depart_ vertex, and
        // The first stop of the second pattern does not even have an _arrive_ vertex.
        super(p0.arriveVertices[p0.stopPattern.size - 1], p1.departVertices[0]);
    }

    @VisibleForTesting
    public PatternInterlineDwell(PatternArriveVertex fromv, PatternDepartVertex tov) {
        super(fromv, tov);
    }

    /**
     * Register the fact that a passenger may pass from Trip t1 to Trip t2
     * by staying on the same vehicle. Trip t1 must be on the TripPattern this
     * edge comes from, and trip t2 must be on the TripPattern this edge leads
     * to.
     */
    public void add(Trip t1, Trip t2) {
        trips.put(t1, t2);
    }

    @Override
    public String getName() {
        return "INTERLINE"; //GtfsLibrary.getRouteName(pattern.getRoute());
    }

    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    @Override
    public State optimisticTraverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(0); // FIXME too optimistic
        return s1.makeState();
    }
    
    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }
    
    @Override
    public double timeLowerBound(RoutingRequest options) {
        return 0; // FIXME overly optimistic
    }

    @Override
    public State traverse(State state0) {

        RoutingRequest options = state0.getOptions();
        Trip oldTrip = state0.getBackTrip();
        Trip newTrip = options.arriveBy ? trips.inverse().get(oldTrip) : trips.get(oldTrip);
        if (newTrip == null) return null;

        TripPattern newPattern;
        TripTimes newTripTimes;
        TripTimes oldTripTimes = state0.getTripTimes();
        int arrivalTime;
        int departureTime;
        AgencyAndId tripId = state0.getTripId();

        if (options.arriveBy) {
            // traversing backward
            newPattern = ((OnboardVertex) fromv).getTripPattern();
            newTripTimes = newPattern.getResolvedTripTimes(newTrip, state0);
            arrivalTime = newTripTimes.getArrivalTime(newTripTimes.getNumStops() - 1); // FIXME with getLastTime method
            departureTime = oldTripTimes.getDepartureTime(0);
        } else {
            // traversing forward
            newPattern = ((OnboardVertex) tov).getTripPattern();
            newTripTimes = newPattern.getResolvedTripTimes(newTrip, state0);
            arrivalTime = oldTripTimes.getArrivalTime(oldTripTimes.getNumStops() - 1); // FIXME with getLastTime method
            departureTime = newTripTimes.getDepartureTime(0);
        }

//        BannedStopSet banned = options.bannedTrips.get(newTrip.getId());
//        if (banned != null && banned.contains(0)) // i.e. if the first stop is banned.
//            return null;

        int dwellTime = departureTime - arrivalTime;
        if (dwellTime < 0) return null;

        StateEditor s1 = state0.edit(this);
        s1.incrementTimeInSeconds(dwellTime);
        s1.setTripId(newTrip.getId()); // TODO check meaning
        s1.setPreviousTrip(oldTrip);   // TODO check meaning
        s1.setTripTimes(newTripTimes);
        s1.incrementWeight(dwellTime);
        // Mode should not change.
        return s1.makeState();
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public String toString() {
        return "PatternInterlineDwell(" + super.toString() + ")";
    }

    @Override
    public int getStopIndex() {
        return -1; // special case: this edge is at a different stop on two different patterns.
    }

}
