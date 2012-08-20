package org.opentripplanner.routing.trippattern;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the arrival / departure times for a single trip in an ArrayTripPattern Also gets carried
 * along by States when routing to ensure that they have a consistent view of the trip when realtime
 * updates are being taken into account. 
 * All times are in seconds since midnight (as in GTFS). The array indexes are actually *hop* indexes,
 * not stop indexes, in the sense that 0 refers to the hop between stops 0 and 1, so arrival 0 is actually
 * an arrival at stop 1. 
 * By making indexes refer to stops not hops we could reuse the departures array for arrivals, 
 * at the cost of an extra entry. This seems more coherent to me (AMB) but would probably break things elsewhere.
 */
public class TripTimes implements Cloneable, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TripTimes.class);

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    public static final int PASSED = -1;
    public static final int CANCELED = -2;
    public static final int EXPIRED = -3;
    
    public final Trip trip;

    public final int index; // this is kind of ugly, but the headsigns are in the pattern not here
    
    @XmlElement
    public int[] departureTimes;

    // null means all dwells are 0-length, and arrival times are to be derived from departure times
    @XmlElement
    public int[] arrivalTimes; 

    public TripTimes(Trip trip, int index, List<StopTime> stopTimes) {
        // stopTimes are assumed to be pre-filtered / valid / monotonically increasing etc.
        this.trip = trip;
        this.index = index;
        int nStops = stopTimes.size();
        int nHops = nStops - 1;
        departureTimes = new int[nHops];
        arrivalTimes = new int[nHops];
        // this might be clearer if time array indexes were stops instead of hops
        for (int hop = 0; hop < nHops; hop++) {
            departureTimes[hop] = stopTimes.get(hop).getDepartureTime();
            arrivalTimes[hop] = stopTimes.get(hop+1).getArrivalTime();
        }
        // if all dwell times are 0, arrival times array is not needed. save some memory.
        this.compact();
    }
    
    public int getDepartureTime(int hop) {
        return departureTimes[hop];
    }

    public int getArrivalTime(int hop) {
        if (arrivalTimes == null)
            return departureTimes[hop + 1];
        return arrivalTimes[hop];
    }

    /**
     * The arrivals array may not be present, and the departures array may have grown by 1 due to
     * compaction, so we can't directly use array lengths as an indicator of number of hops. 
     */
    public int getNumHops() {
        if (arrivalTimes == null)
            return departureTimes.length - 1;
        else
            return arrivalTimes.length;
    }

    public int getRunningTime(int hop) {
        return getArrivalTime(hop) - getDepartureTime(hop);
    }

    public int getDwellTime(int hop) {
        // the dwell time of a hop is the dwell time *before* that hop.
        // Therefore it is undefined for hop 0, and at the end of a trip.
        // Add range checking and -1 error value?
        // see GTFSPatternHopFactory.makeTripPattern()
        int arrivalTime = getArrivalTime(hop-1);
        int departureTime = getDepartureTime(hop);
        return departureTime - arrivalTime;
    }
    
    public Trip getTrip() {
        return trip;
    }

    /** replace arrivals array with null if all dwell times are zero */
    public boolean compact() {
        if (arrivalTimes == null)
            return false;
        // always use arrivalTimes to determine number of hops because departureTimes may grow by 1
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
    
    public boolean decompact() {
        if (arrivalTimes != null)
            return false;
        int nHops = departureTimes.length;
        if (nHops < 1)
            throw new RuntimeException("improper array length in TripTimes");
        arrivalTimes = Arrays.copyOfRange(departureTimes, 1, nHops);
        return true;
    }

    public String toString() {
        return dumpTimes();
    }
    
    public String dumpTimes() {
        StringBuilder sb = new StringBuilder();
        int nHops = getNumHops();
        sb.append(arrivalTimes == null ? "C " : "U ");
        for (int hop=0; hop < nHops; hop++) {
            sb.append(hop); 
            sb.append(':');
            sb.append(getDepartureTime(hop)); 
            sb.append('-');
            sb.append(getArrivalTime(hop));
            sb.append(' ');
        }
        return sb.toString();
    }
        
    @Override
    public TripTimes clone() {
        TripTimes ret = null; 
        try {
            ret = (TripTimes) super.clone();
            if (arrivalTimes == null) // dwell times are all zero
                ret.arrivalTimes = null;
            else
                ret.arrivalTimes = this.arrivalTimes.clone();
            ret.departureTimes = this.departureTimes.clone();
        } catch (CloneNotSupportedException e) {
            // will not happen
        }
        return ret;
    }
    
    public static Comparator<TripTimes> getArrivalsComparator(final int hopIndex) {
        return new Comparator<TripTimes>() {
            @Override
            public int compare(TripTimes tt1, TripTimes tt2) {
                return tt1.getArrivalTime(hopIndex) - tt2.getArrivalTime(hopIndex);
            }
        };
    }

    public static Comparator<TripTimes> getDeparturesComparator(final int hopIndex) {
        return new Comparator<TripTimes>() {
            @Override
            public int compare(TripTimes tt1, TripTimes tt2) {
                return tt1.getDepartureTime(hopIndex) - tt2.getDepartureTime(hopIndex);
            }
        };
    }
    
    /**
     * Binary search method adapted from GNU Classpath Arrays.java (GPL). 
     * Range parameters and range checking removed.
     * Search across an array of TripTimes, looking only at a specific hop number.
     * 
     * @return the index at which the key was found, or the index of the first value higher than 
     * key if it was not found, or a.length if there is no such value. Note that this has been
     * changed from Arrays.binarysearch.
     */
    public static int binarySearchDepartures(TripTimes[] a, int hop, int key) {
        int low = 0;
        int hi = a.length - 1;
        int mid = 0;
        while (low <= hi) {
            mid = (low + hi) >>> 1;
            final int d = a[mid].getDepartureTime(hop);
            if (d == key)
                return mid;
            else if (d > key)
                hi = mid - 1;
            else
                // This gets the insertion point right on the last loop.
                low = ++mid;
        }
        return mid;
    }

    /**
     * Binary search method adapted from GNU Classpath Arrays.java (GPL). 
     * Range parameters and range checking removed.
     * Search across an array of TripTimes, looking only at a specific hop number.
     * 
     * @return the index at which the key was found, or the index of the first value *lower* than
     * key if it was not found, or -1 if there is no such value. Note that this has been changed
     * from Arrays.binarysearch: this is a mirror-image of the departure search algorithm.
     * 
     * TODO: I have worked through corner cases but should reverify with some critical distance.
     */
    public static int binarySearchArrivals(TripTimes[] a, int hop, int key) {
        int low = 0;
        int hi = a.length - 1;
        int mid = hi;
        while (low <= hi) {
            mid = (low + hi) >>> 1;
            final int d = a[mid].getArrivalTime(hop);
            if (d == key)
                return mid;
            else if (d < key)
                low = mid + 1;
            else
                // This gets the insertion point right on the last loop.
                hi = --mid;
        }
        return mid;
    }

    public void forcePositive() {
        // iterate over the new tripTimes, checking that dwells and hops are positive
        boolean found = false;
        int nHops = getNumHops();
        int prevArr = -1;
        for (int hop = 0; hop < nHops; hop++) {
            int dep = this.getDepartureTime(hop);
            int arr = this.getArrivalTime(hop);
            if (arr < dep) { // negative hop time
                LOG.error("negative hop time in updated TripTimes at index {}", hop);
                found = true;
            }
            if (prevArr > dep) { // negative dwell time before this hop
                found = true;
                LOG.error("negative dwell time in updated TripTimes at index {}", hop);
            }
            prevArr = arr;
        }
        if (found) {
            LOG.error(this.dumpTimes());
        }
    }

}
