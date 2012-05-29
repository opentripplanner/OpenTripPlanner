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

import java.util.Iterator;
import java.util.List;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingRequest;

/**
 * Represents a class of trips distinguished by service id and list of stops. For each stop, there
 * is a list of departure times, running times, arrival times, dwell times, and wheelchair
 * accessibility information (one of each of these per trip per stop). An exemplar trip is also
 * included so that information such as route name can be found. Trips are assumed to be
 * non-overtaking, so that an earlier trip never arrives after a later trip.
 */

public interface TripPattern {

    public static final int FLAG_WHEELCHAIR_ACCESSIBLE = 1;

    public static final int MASK_PICKUP = 2|4;
    public static final int SHIFT_PICKUP = 1;

    public static final int MASK_DROPOFF = 8|16;
    public static final int SHIFT_DROPOFF = 3;

    public static final int NO_PICKUP = 1;

    public static final int FLAG_BIKES_ALLOWED = 32;

    /** Get the index of the next trip that has a stop after afterTime at the stop at stopIndex
     * 
     * @param pickup whether the stop must be one that picks up passengers 
     */
    public TripTimes getNextTrip(int stopIndex, int afterTime, RoutingRequest options);

    /** Gets the running time after a given stop on a given trip */
    public int getRunningTime(int stopIndex, int tripIndex);

    /** Gets the departure time for a given stop on a given trip */
    public int getDepartureTime(int stopIndex, int tripIndex);

    /** Gets the index of the previous trip that has a stop before beforeTime at the stop at stopIndex 
     * 
     * @param pickup whether the stop must be one that picks up passengers
     */
    public TripTimes getPreviousTrip(int stopIndex, int beforeTime, RoutingRequest options);

    /** Gets the arrival time for a given stop on a given trip */
    public int getArrivalTime(int stopIndex, int trip);

    /** Gets the dwell time at a given stop on a given trip */
    public int getDwellTime(int stopIndex, int trip);

    /** Gets all the departure times at a given stop (not used in routing) */
    public Iterator<Integer> getDepartureTimes(int stopIndex);

    /** Gets the accessibility of a given stop on a given trip */ 
    public boolean getWheelchairAccessible(int stopIndex, int trip);

    /** Gets the bikability of a given trip */ 
    public boolean getBikesAllowed(int trip);

    /** Gets the Trip object for a given trip index */
    public Trip getTrip(int trip);

    /** Returns whether passengers can alight at a given stop */
    public boolean canAlight(int stopIndex);

    /** Returns whether passengers can board at a given stop */
    public boolean canBoard(int stopIndex);

    /** Returns the zone of a given stop */
    public String getZone(int stopIndex);

    /** Returns an arbitrary trip that uses this pattern */
    public Trip getExemplar();

    /** Returns the shortest possible running time for this stop */
    public int getBestRunningTime(int stopIndex);

    /** Returns the shortest possible dwell time at this stop */
    int getBestDwellTime(int stopIndex);

    public List<Trip> getTrips();

    /** Gets the current headsign as-of this stop, if it differs from the trip headsign */
    public String getHeadsign(int stopIndex, int trip);

    public int getAlightType(int stopIndex);

    public int getBoardType(int stopIndex);

    public TripTimes getTripTimes(int tripIndex);
}
