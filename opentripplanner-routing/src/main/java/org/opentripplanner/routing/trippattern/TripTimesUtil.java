package org.opentripplanner.routing.trippattern;

import java.util.Comparator;

import lombok.AllArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * This class contains static utility methods that operate on or return objects of type TripTimes. 
 * By convention, the name of this class would be the plural of TripTimes, which is already plural.
 */
public class TripTimesUtil {

    private static final Logger LOG = LoggerFactory.getLogger(TripTimesUtil.class);

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

    /* TODO: CAN BE MOVED INTO ScheduledTimetable since it uses only methods to access arr/dep times. */
    /**
     * After applying updates to scheduled Triptimes, we could potentially end up with negative
     * hop or dwell times. We really don't want those being used in routing. Check that all times 
     * are increasing, and issue warnings if this is not the case.
     * @return whether the times were found to be increasing.
     */
    public static boolean timesIncreasing(TripTimes tt) {
        // iterate over the new tripTimes, checking that dwells and hops are positive
        boolean increasing = true;
        int nHops = tt.getNumHops();
        int prevArr = -1;
        for (int hop = 0; hop < nHops; hop++) {
            int dep = tt.getDepartureTime(hop);
            int arr = tt.getArrivalTime(hop);
            if (arr < dep) { // negative hop time
                LOG.error("negative hop time in TripTimes at index {}", hop);
                increasing = false;
            }
            if (prevArr > dep) { // negative dwell time before this hop
                LOG.error("negative dwell time in TripTimes at index {}", hop);
                increasing = false;
            }
            prevArr = arr;
        }
        if (!increasing) {
            LOG.error(tt.dumpTimes());
        }
        return increasing;
    }

}
