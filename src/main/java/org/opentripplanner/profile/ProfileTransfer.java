package org.opentripplanner.profile;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* A transfer as used in profile routing. For now, only the best transfer between any two patterns.
*/
public class ProfileTransfer implements Comparable<ProfileTransfer> {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileTransfer.class);

    public TripPattern tp1, tp2;
    public Stop s1, s2;
    public int distance; // meters

    public ProfileTransfer(TripPattern tp1, TripPattern tp2, Stop s1, Stop s2, int distance) {
		this.tp1 = tp1;
		this.tp2 = tp2;
		this.s1 = s1;
		this.s2 = s2;
		this.distance = distance;
	}

	@Override
    public int compareTo(ProfileTransfer that) {
        return this.distance - that.distance;
    }

    @Override
    public String toString() {
        return String.format("Transfer %s %s %s %s %d", tp1.getCode(), s1.getId(),
                tp2.getCode(), s2.getId(), distance);
    }

}
