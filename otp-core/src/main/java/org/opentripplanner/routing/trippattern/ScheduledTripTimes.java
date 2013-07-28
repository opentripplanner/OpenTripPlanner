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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ScheduledTripTimes represents the standard published timetable for a trip provided by a transit
 * data feed. When real-time stop time updates are being applied, these scheduled TripTimes can be
 * wrapped in other TripTimes implementations which replace, cancel, or otherwise modify some of 
 * the timetable information.
 */
public class ScheduledTripTimes extends TripTimes implements Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTripTimes.class);
    
    @Getter private final Trip trip;

    /**
     * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS 
     * fields. If the headsigns array is null, we will report the trip_headsign (which may also
     * be null) at every stop on the trip. If all the stop_headsigns are the same as the 
     * trip_headsign we may also set the headsigns array to null to save space.
     */
    private final String[] headsigns;
    
    /** 
     * The time in seconds after midnight at which the vehicle begins traversing each inter-stop 
     * segment ("hop"). Field is non-final to support compaction.
     */ //@XmlElement
    private int[] departureTimes;

    /** 
     * The time in seconds after midnight at which the vehicle arrives at the end of each 
     * inter-stop segment ("hop"). A null value indicates that all dwells are 0-length, and arrival 
     * times are to be derived from the departure times array. Field is non-final to support 
     * compaction.
     */ //@XmlElement
    private int[] arrivalTimes; 

    /** The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing. */ 
    public ScheduledTripTimes(Trip trip, List<StopTime> stopTimes) {
        this.trip = trip;
        int nStops = stopTimes.size();
        int nHops = nStops - 1;
        departureTimes = new int[nHops];
        arrivalTimes = new int[nHops];
        // this might be clearer if time array indexes were stops instead of hops
        for (int hop = 0; hop < nHops; hop++) {
            departureTimes[hop] = stopTimes.get(hop).getDepartureTime();
            arrivalTimes[hop] = stopTimes.get(hop + 1).getArrivalTime();
        }
        this.headsigns = makeHeadsignsArray(stopTimes);
        // If all dwell times are 0, arrival times array is not needed. Attempt to save some memory.
        this.compact();
    }
    
    /** 
     * @return either an array of headsigns (one for each stop on this trip) or null if the 
     * headsign is the same at all stops (including null) and can be found in the Trip object. 
     */
    private String[] makeHeadsignsArray(List<StopTime> stopTimes) {
        String tripHeadsign = trip.getTripHeadsign();
        boolean useStopHeadsigns = false;
        if (tripHeadsign == null) {
            useStopHeadsigns = true;
        }
        else {
            for (StopTime st : stopTimes) {
                if ( ! (tripHeadsign.equals(st.getStopHeadsign()))) {
                    useStopHeadsigns = true;
                    break;
                }
            }
        }
        if ( ! useStopHeadsigns) { 
            return null; //defer to trip_headsign
        }
        boolean allNull = true;
        int i = 0;
        String[] hs = new String[stopTimes.size()];
        for (StopTime st : stopTimes) {
            String headsign = st.getStopHeadsign();
            hs[i++] = headsign;
            if (headsign != null)
                allNull = false;
        }
        if (allNull)
            return null;
        else
            return hs;
    }
    
    @Override
    public int getNumHops() {
        // The arrivals array may not be present, and the departures array may have grown by 1 due 
        // to compaction, so we can't directly use array lengths as an indicator of number of hops.
        if (arrivalTimes == null)
            return departureTimes.length - 1;
        else
            return arrivalTimes.length;
    }
    
    @Override
    public ScheduledTripTimes getScheduledTripTimes() {
        return this;
    }    
    
    @Override
    public int getDepartureTime(int hop) {
        return departureTimes[hop];
    }

    @Override
    public int getArrivalTime(int hop) {
        if (arrivalTimes == null) // add range checking?
            return departureTimes[hop + 1];
        return arrivalTimes[hop];
    }
    
    /** {@inheritDoc} Replaces the arrivals array with null if all dwell times are zero. */
    @Override
    public boolean compact() {
        if (arrivalTimes == null)
            return false;
        // use arrivalTimes to determine number of hops because departureTimes may have grown by 1
        // due to successive compact/decompact operations
        int nHops = arrivalTimes.length;
        // dwell time is undefined for hop 0, because there is no arrival for hop -1
        for (int hop = 1; hop < nHops; hop++) {
            if (this.getDwellTime(hop) != 0) {
                LOG.trace("compact failed: nonzero dwell time before hop {}", hop);
                return false;
            }
        }
        // extend departureTimes array by 1 to hold final arrival time
        departureTimes = Arrays.copyOf(departureTimes, nHops+1);
        departureTimes[nHops] = arrivalTimes[nHops-1];
        arrivalTimes = null;
        return true;
    }
    
    @SuppressWarnings("unused")
    private boolean decompact() {
        if (arrivalTimes != null)
            return false;
        int nHops = departureTimes.length;
        if (nHops < 1)
            throw new RuntimeException("improper array length in TripTimes");
        arrivalTimes = Arrays.copyOfRange(departureTimes, 1, nHops);
        return true;
    }

    public String toString() {
        return "ScheduledTripTimes\n" + dumpTimes();
    }
    
    @Override
    public String getHeadsign(int hop) {
        if (headsigns == null)
            return trip.getTripHeadsign();
        else
            return headsigns[hop];
    }

}
