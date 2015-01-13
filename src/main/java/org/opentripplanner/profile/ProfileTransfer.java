package org.opentripplanner.profile;

import com.beust.jcommander.internal.Lists;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
* A transfer as used in profile routing. For now, only the best transfer between any two patterns.
*/
public class ProfileTransfer implements Comparable<ProfileTransfer> {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileTransfer.class);

    public TripPattern tp1, tp2;
    public StopCluster sc1, sc2;
    public int distance; // meters

    public ProfileTransfer(TripPattern tp1, TripPattern tp2, StopCluster s1, StopCluster s2, int distance) {
		this.tp1 = tp1;
		this.tp2 = tp2;
		this.sc1 = s1;
		this.sc2 = s2;
		this.distance = distance;
	}

	@Override
    public int compareTo(ProfileTransfer that) {
        return this.distance - that.distance;
    }

    @Override
    public String toString() {
        return String.format("Transfer %s %s %s %s %d", tp1.code, sc1.id,
                tp2.code, sc2.id, distance);
    }

    /** Keeps track of the best N transfers, including all those that are within the same stop cluster. */
    public static class GoodTransferList {
        private final double SLOP = 1.5;
        public List<ProfileTransfer> good = Lists.newArrayList();
        public void add(ProfileTransfer xfer) {
            boolean removed = false;
            Iterator<ProfileTransfer> iter = good.iterator();
            while (iter.hasNext()) {
                ProfileTransfer curr = iter.next();
                if (xfer.distance > curr.distance * SLOP) {
                    return; // this transfer is a lot worse than an existing one. bail out without saving it.
                }
                if (curr.distance > xfer.distance * SLOP) {
                    iter.remove(); // this transfer is a lot better than an existing one. remove the existing one.
                }
            }
            // The new transfer was not dominated by an existing one. Save it.
            good.add(xfer);
        }
    }

}
