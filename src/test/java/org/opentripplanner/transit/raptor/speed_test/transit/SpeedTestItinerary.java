package org.opentripplanner.transit.raptor.speed_test.transit;

import org.opentripplanner.transit.raptor.speed_test.api.model.Itinerary;
import org.opentripplanner.transit.raptor.speed_test.api.model.Leg;
import org.opentripplanner.transit.raptor.util.TimeUtils;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;

import java.util.HashSet;
import java.util.Set;

import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrCompact;
import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrShort;

public class SpeedTestItinerary extends Itinerary {

    private int durationSeconds;
    private int walkDistanceMeters;
    private int endTimeSeconds;
    //private int modesHash;
    private int agenciesHash;

    void initParetoVector() {
        durationSeconds = this.duration.intValue();
        walkDistanceMeters = this.walkDistance.intValue();
        endTimeSeconds = (int) (this.endTime.getTimeInMillis() / 1000);

        //Set<String> modes = new HashSet<>();
        Set<String> agencies = new HashSet<>();

        double distanceLimit = 0;

        for (Leg leg : legs) {
            if (leg.isTransitLeg()) {
                distanceLimit += leg.distance;
            }
        }

        distanceLimit /= 3;

        for (Leg leg : legs) {
            if (leg.isTransitLeg()) {
                if (leg.distance > distanceLimit) {
                    //modes.add(leg.mode);
                    agencies.add(leg.agencyId);
                }
            }
        }
        //modesHash = modes.hashCode();
        agenciesHash = agencies.hashCode();
    }

    static ParetoComparator<org.opentripplanner.transit.raptor.speed_test.transit.SpeedTestItinerary> paretoDominanceFunctions() {
        return (l, r) ->
                l.transfers < r.transfers ||
                l.durationSeconds < r.durationSeconds ||
                l.walkDistanceMeters < r.walkDistanceMeters ||
                l.endTimeSeconds < r.endTimeSeconds ||
                //l.modesHash != r.modesHash ||
                l.agenciesHash != r.agenciesHash;
    }

    @Override
    public String toString() {
        return String.format(
                "Tr: %d, duration: %s, walkDist: %5.0f, start: %s, end: %s, Details: %s",
                transfers,
                TimeUtils.timeToStrCompact(duration.intValue()),
                walkDistance,
                TimeUtils.timeToStrLong(startTime),
                TimeUtils.timeToStrLong(endTime),
                legsAsCompactString(this)
        );
    }

    /**
     * Create a compact representation of all legs in the itinerary.
     * Example:
     * <pre>
     * WALK 7:12 - 37358 - NW180 09:30 10:20 - 34523 - WALK 0:10 - 86727 - NW130 10:30 10:40 - 3551 - WALK 3:10
     * </pre>
     */
    public static String legsAsCompactString(Itinerary itinerary) {
        Integer toStop = -1;

        StringBuilder buf = new StringBuilder();
        for (Leg it : itinerary.legs) {
            Integer fromStop = it.from.stopIndex;
            if (fromStop != null && fromStop != -1 && !fromStop.equals(toStop)) {
                buf.append("- ").append(fromStop).append(" - ");
            }

            if (it.isTransitLeg()) {
                buf.append(it.mode);
                buf.append(' ');
                buf.append(it.routeShortName);
                buf.append(' ');
                buf.append(timeToStrShort(it.startTime));
                buf.append(' ');
                buf.append(timeToStrShort(it.endTime));
                buf.append(' ');
            } else {
                buf.append(it.mode);
                buf.append(' ');
                buf.append(timeToStrCompact((int) it.getDuration()));
                buf.append(' ');
            }
            toStop = it.to.stopIndex;
            if (toStop != null) {
                buf.append("- ").append(toStop).append(" - ");
            }
        }
        return buf.toString().trim();
    }
}
