package org.opentripplanner.profile;

import com.beust.jcommander.internal.Maps;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Map;

/**
 * A fundamental tool for tracking arrival time distributions.
 *
 * What we want is a sparse NxM matrix of primitive integers which amounts to a histogram of arrivals a 0...M minutes
 * at each of N stops or stop clusters.
 *
 * This will lose some detail though since times are usually in seconds. So an alternative is to store bags of ints
 * per stop.
 *
 * We may also need to store the min and max entry for each bag to avoid constantly sorting them.
 * We may want to store the multiset as a parallel array of counts and values if there are a lot of duplicates.
 * That is less likely if we are working in seconds rather than minutes.
 */
public class TimeTracker {

    public Map<StopCluster, TIntList> timesForCluster = Maps.newHashMap();

    /**
     * Add a variable number of int arguments or an array of ints to the bag for a given stopcluster.
     * Optionally sort all the entries after a bulk add.
     */
    public void add(StopCluster stopCluster, boolean sort, int... time) {
        TIntList times = timesForCluster.get(stopCluster);
        if (times == null) {
            times = new TIntArrayList();
            timesForCluster.put(stopCluster, times);
        }
        times.add(time);
        if (sort) {
            times.sort();
        }
    }

    public void dump() {
        for (StopCluster stopCluster : timesForCluster.keySet()) {
            TIntList ints = timesForCluster.get(stopCluster);
            System.out.printf("%s --> %s \n", stopCluster.toString(), ints.toString());
        }
    }

}
