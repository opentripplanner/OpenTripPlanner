package org.opentripplanner.routing.algorithm.raptor.itinerary;

import com.conveyal.r5.otp2.util.TimeUtils;
import com.conveyal.r5.otp2.util.paretoset.ParetoComparator;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;

import java.util.HashSet;
import java.util.Set;

import static com.conveyal.r5.otp2.util.TimeUtils.timeToStrCompact;
import static com.conveyal.r5.otp2.util.TimeUtils.timeToStrShort;

public class ParetoItinerary extends Itinerary {

    private final int[] paretoValues = new int[5];

    public ParetoItinerary(Itinerary itinerary) {
        if (itinerary != null) {
            this.duration = itinerary.duration;
            this.startTime = itinerary.startTime;
            this.endTime = itinerary.endTime;
            this.walkTime = itinerary.walkTime;
            this.transitTime = itinerary.transitTime;
            this.waitingTime = itinerary.waitingTime;
            this.walkDistance = itinerary.walkDistance;
            this.walkLimitExceeded = itinerary.walkLimitExceeded;
            this.walkDistance = itinerary.walkDistance;
            this.elevationLost = itinerary.elevationLost;
            this.elevationGained = itinerary.elevationGained;
            this.transfers = itinerary.transfers;
            this.fare = itinerary.fare;
            this.legs = itinerary.legs;
            this.tooSloped = itinerary.tooSloped;
        }
    }

    public void initParetoVector() {
        int i = 0;
        paretoValues[i++] = this.transfers;
        paretoValues[i++] = this.duration.intValue();
        paretoValues[i++] = this.walkDistance.intValue();
        paretoValues[i++] = (int) (this.endTime.getTimeInMillis() / 1000);

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
        //paretoValues[i++] = modes.hashCode();
        paretoValues[i] = agencies.hashCode();
    }

    public static ParetoComparator<ParetoItinerary> paretoComperator() {
        return (l,r) -> l.paretoValue1() < r.paretoValue1() ||
                    l.paretoValue2() < r.paretoValue2() ||
                    l.paretoValue3() < r.paretoValue3() ||
                    l.paretoValue4() < r.paretoValue4() ||
                    l.paretoValue5() < r.paretoValue5();
    }

    private int paretoValue1() { return paretoValues[0]; }
    private int paretoValue2() { return paretoValues[1]; }
    private int paretoValue3() { return paretoValues[2]; }
    private int paretoValue4() { return paretoValues[3]; }
    private int paretoValue5() { return paretoValues[4]; }

    @Override
    public String toString() {
        return String.format(
                "Tr: %d, duration: %s, walkDist: %5.0f, start: %s, end: %s, Details: %s",
                transfers,
                timeToStrCompact(duration.intValue()),
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
