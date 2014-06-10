package org.opentripplanner.profile;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.gtfs.GtfsLibrary;

/**
 * The equivalent of a ride in an API response. All information degenerates to Strings and ints here.
 */
public class Segment {

    public static class SegmentPattern implements Comparable<SegmentPattern> {
        public String patternId;
        public int fromIndex;
        public int toIndex;
        public int nTrips;
        // public Stats stats;
        public SegmentPattern (PatternRide patternRide) {
            this.patternId = patternRide.pattern.getCode();
            this.fromIndex = patternRide.fromIndex;
            this.toIndex   = patternRide.toIndex;
            this.nTrips    = patternRide.stats.num; // this stats has time window applied
            // this.stats     = patternRide.stats;
        }
        @Override
        public int compareTo (SegmentPattern other) {
            return other.nTrips - this.nTrips;
        }
    }
    
    @Getter int walkTime;
    @Getter int walkDistance;
    @Getter Stats waitStats;

    @Getter String route;
    @Getter String mode;
    @Getter String from;
    @Getter String to;
    @Getter String fromName;
    @Getter String toName;
    @Getter String routeShortName;
    @Getter String routeLongName;
    @Getter Stats rideStats;
    @Getter List<SegmentPattern> segmentPatterns = Lists.newArrayList();

    public Segment (Ride ride, TimeWindow window, double walkSpeed) {
        route = ride.route.getId().getId();
        mode = GtfsLibrary.getTraverseMode(ride.route).toString();
        routeShortName = ride.route.getShortName();
        routeLongName = ride.route.getLongName();
        from = ride.from.getId().getId();
        to = ride.to.getId().getId();
        fromName = ride.from.getName();
        toName = ride.to.getName();
        rideStats = ride.getStats();
        for (PatternRide patternRide : ride.patternRides) {
            segmentPatterns.add(new SegmentPattern(patternRide));
        }
        Collections.sort(segmentPatterns);
        walkDistance = (int) ride.getTransferDistance();
        walkTime = (int) (ride.getTransferDistance() / walkSpeed);
        /* At this point we know all patterns on rides. Calculate transfer time information. */
        waitStats = ride.statsForTransfer (window, walkSpeed);
    }

}
