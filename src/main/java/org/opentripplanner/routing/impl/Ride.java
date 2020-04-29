package org.opentripplanner.routing.impl;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;

import java.util.HashSet;
import java.util.Set;

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

    long startTime;

    long endTime;

    // in DefaultFareServiceImpl classifier is just the TraverseMode
    // it can be used differently in custom fare services
    public Object classifier;

    public Stop firstStop;

    public Stop lastStop;

    public Ride() {
        zones = new HashSet<String>();
    }

    public FeedScopedId getAgency() {
        return agency;
    }

    public FeedScopedId getRoute() {
        return route;
    }

    public FeedScopedId getTrip() {
        return trip;
    }

    public Set<String> getZones() {
        return zones;
    }

    public String getStartZone() {
        return startZone;
    }

    public String getEndZone() {
        return endZone;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Object getClassifier() {
        return classifier;
    }

    public Stop getFirstStop() {
        return firstStop;
    }

    public Stop getLastStop() {
        return lastStop;
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
