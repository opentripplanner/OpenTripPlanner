package org.opentripplanner.profile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import lombok.Getter;

import com.beust.jcommander.internal.Lists;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.routing.core.TraverseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The equivalent of a ride in an API response. Information degenerates to Strings and ints here.
 * TODO rename to TransitSegment
 */
public class Segment {

    private static final Logger LOG = LoggerFactory.getLogger(Segment.class);

    public static class SegmentPattern implements Comparable<SegmentPattern> {
        public String patternId;
        public int fromIndex;
        public int toIndex;
        public int nTrips;
        public SegmentPattern (PatternRide patternRide) {
            this.patternId = patternRide.pattern.getCode();
            this.fromIndex = patternRide.fromIndex;
            this.toIndex   = patternRide.toIndex;
            this.nTrips    = patternRide.stats.num; // this stats has time window applied
        }
        @Override
        public int compareTo (SegmentPattern other) {
            return other.nTrips - this.nTrips;
        }
    }

    // Use AgencyAndId instead of String to get both since we are now multi-feed
    @Getter AgencyAndId from;
    @Getter AgencyAndId to;
    @Getter int walkTime;
    @Getter int walkDistance;
    @Getter Stats waitStats;
    @Getter TraverseMode mode;
    @Getter String fromName;
    @Getter String toName;
    @Getter Stats rideStats;
    @Getter List<RouteShort> routes;
    @Getter List<SegmentPattern> segmentPatterns = Lists.newArrayList();

    public Segment (Ride ride) {
        Route route = ride.patternRides.get(0).pattern.getRoute();
        from = ride.from.getId();
        to = ride.to.getId();
        fromName = ride.from.getName();
        toName = ride.to.getName();
        rideStats = ride.rideStats;
        Set<Route> routes = Sets.newHashSet();
        for (PatternRide patternRide : ride.patternRides) {
            segmentPatterns.add(new SegmentPattern(patternRide));
            routes.add(patternRide.pattern.route);
        }
        Collections.sort(segmentPatterns);
        // Note that despite the fact that multiple patterns from different routes will appear in the same ride,
        // in practice all the patterns within a ride will be from the same operator and mode because they all pass
        // through the same stops.
        mode = ride.patternRides.get(0).pattern.mode;
        for (PatternRide pr : ride.patternRides) {
            if (pr.pattern.mode != mode) LOG.warn("Segment contains patterns using more than one mode.");
        }
        walkDistance = (int) ride.getTransferDistance();
        walkTime = (int) (ride.getTransferDistance()); // TODO / walkSpeed);
        waitStats = ride.waitStats;
        this.routes = RouteShort.list(routes);
    }

}
