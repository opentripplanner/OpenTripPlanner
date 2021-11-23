package org.opentripplanner.routing.fares.impl;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.model.StopLocation;

/**
 * A set of edges on a single route, with associated information. Used only in calculating fares.
 */
public class Ride {

    FeedScopedId agency; // route agency

    FeedScopedId route;

    FeedScopedId trip;

    Set<String> zones;

    String startZone;

    String endZone;

    ZonedDateTime startTime;

    ZonedDateTime endTime;

    // in DefaultFareServiceImpl classifier is just the TraverseMode
    // it can be used differently in custom fare services
    public Object classifier;

    public StopLocation firstStop;

    public StopLocation lastStop;

    public Ride() {
        zones = new HashSet<String>();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ride");
        if (startZone != null) {
            builder.append("(from zone ");
            builder.append(startZone);
        }
        if (endZone != null) {
            builder.append(" to zone ");
            builder.append(endZone);
        }
        builder.append(" on route ");
        builder.append(route);
        if (zones.size() > 0) {
            builder.append(" through zones ");
            boolean first = true;
            for (String zone : zones) {
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(zone);
            }
        }
        builder.append(" at ");
        builder.append(startTime);
        if (classifier != null) {
            builder.append(", classified by ");
            builder.append(classifier.toString());
        }
        builder.append(")");
        return builder.toString();
    }
}