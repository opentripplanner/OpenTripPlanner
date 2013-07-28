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

package org.opentripplanner.routing.trippattern;

/**
 * A CanceledTripTimes represents the cancellation of an entire trip by reporting that the vehicle
 * has already passed all stops.
 */
public class CanceledTripTimes extends DelegatingTripTimes {

    public CanceledTripTimes(ScheduledTripTimes sched) {
        super(sched);
    }

    @Override public int getDepartureTime(int hop) {
        return TripTimes.CANCELED;
    }
    
    @Override public int getArrivalTime(int hop) {
        return TripTimes.CANCELED;
    }
        
}
