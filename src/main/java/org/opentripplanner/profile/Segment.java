package org.opentripplanner.profile;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.routing.core.TraverseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
            this.patternId = patternRide.pattern.code;
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
    public String from;
    public String to;
    public int walkTime;
    public int walkDistance;
    public Stats waitStats;
    public TraverseMode mode;
    public String fromName;
    public String toName;
    public Stats rideStats;
    public List<RouteShort> routes;
    public List<SegmentPattern> segmentPatterns = Lists.newArrayList();
    public String startTime;
    public String endTime;

    public Segment (Ride ride) {
        Route route = ride.patternRides.get(0).pattern.route;
        from = ride.from.id;
        to = ride.to.id;
        fromName = ride.from.name;
        toName = ride.to.name;
        rideStats = ride.rideStats;
        Set<Route> routes = Sets.newHashSet();
        //LOG.info(" Ride fom {} to {}", ride.from.id, ride.to.id);
        for (PatternRide patternRide : ride.patternRides) {
            segmentPatterns.add(new SegmentPattern(patternRide));
            routes.add(patternRide.pattern.route);
            //LOG.info("   pattern {} {} from {} to {}", patternRide.pattern.mode, patternRide.pattern.getCode(), patternRide.getFromStop(), patternRide.getToStop());
        }
        Collections.sort(segmentPatterns);
        // Note that despite the fact that multiple patterns from different routes will appear in the same ride,
        // in practice all the patterns within a ride will be from the same operator and mode because they all pass
        // through the same stops.
        mode = ride.patternRides.get(0).pattern.mode;
        for (PatternRide pr : ride.patternRides) {
            if (pr.pattern.mode != mode) LOG.warn("Segment contains patterns using more than one mode.");
        }
        walkTime = ride.accessStats.min; // FIXME this is assuming min=max for transfers, which they do for now...
        walkDistance = ride.accessDist;
        waitStats = ride.waitStats;
        this.routes = RouteShort.list(routes);
    }

}
