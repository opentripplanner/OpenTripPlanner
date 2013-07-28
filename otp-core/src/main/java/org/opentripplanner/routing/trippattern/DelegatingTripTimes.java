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

import org.onebusaway.gtfs.model.Trip;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/** 
 * Extend this class to wrap scheduled trip times, yielding updated/patched/modified ones. 
 */
@AllArgsConstructor
public abstract class DelegatingTripTimes extends TripTimes {

    @NonNull private final ScheduledTripTimes base;

    @Override public Trip getTrip() { return base.getTrip(); }

    @Override public ScheduledTripTimes getScheduledTripTimes() { return base.getScheduledTripTimes(); }

    @Override public int getNumHops() { return base.getNumHops(); }

    @Override public int getDepartureTime(int hop) { return base.getDepartureTime(hop); }

    @Override public int getArrivalTime(int hop) { return base.getArrivalTime(hop); }
    
    @Override public String getHeadsign(int hop) { return base.getHeadsign(hop); }
 
    @Override public String toString() { return base.toString(); }

}
