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
     * Each trip has a defined headsign, but this trip_headsign has been overridden by the 
     * stop_headsign field in stop_times.txt, the headsigns field will point to an array of
     * headsigns, one for each stop of the trip. If this field is set to null, this TripTimes 
     * will get its headsign from the Trip object for every stop.
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
    
    private String[] makeHeadsignsArray(List<StopTime> stopTimes) {
        String tripHeadsign = trip.getTripHeadsign();
        String[] hs = new String[stopTimes.size()];
        boolean useHeadsigns = false;
        int i = 0;
        for (StopTime st : stopTimes) {
            String stopHeadsign = st.getStopHeadsign(); 
            if (!(tripHeadsign.equals(stopHeadsign))) {
                useHeadsigns = true;
            }
            hs[i++] = stopHeadsign;
        }
        if (useHeadsigns)
            return hs;
        else
            return null;
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
