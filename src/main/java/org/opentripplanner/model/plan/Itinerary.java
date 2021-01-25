package org.opentripplanner.model.plan;


import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary {

    /** Total duration of the itinerary in seconds */
    public final int durationSeconds;

    /**
     * How much time is spent on transit, in seconds.
     */
    public final int transitTimeSeconds;

    /**
     * The number of transfers this trip has.
     */
    public final int nTransfers;

    /**
     * How much time is spent waiting for transit to arrive, in seconds.
     */
    public final int waitingTimeSeconds;

    /**
     * How much time is spent walking/biking/driving, in seconds.
     */
    public int nonTransitTimeSeconds;

    /**
     * How far the user has to walk, bike and/or drive, in meters.
     */
    public final double nonTransitDistanceMeters;

    /**
     * Indicates that the walk/bike/drive limit distance has been exceeded for this itinerary.
     */
    public boolean nonTransitLimitExceeded = false;

    /**
     * How much elevation is lost, in total, over the course of the trip, in meters. As an example,
     * a trip that went from the top of Mount Everest straight down to sea level, then back up K2,
     * then back down again would have an elevationLost of Everest + K2.
     */

    public Double elevationLost = 0.0;
    /**
     * How much elevation is gained, in total, over the course of the trip, in meters. See
     * elevationLost.
     */

    public Double elevationGained = 0.0;

    /**
     * If a generalized cost is used in the routing algorithm, this should be the total
     * cost computed by the algorithm. This is relevant for anyone who want to debug an search
     * and tuning the system. The unit should be equivalent to the cost of "one second of transit".
     * <p>
     * -1 indicate that the cost is not set/computed.
     */
    public int generalizedCost = -1;

    /**
     * This itinerary has a greater slope than the user requested (but there are no possible
     * itineraries with a good slope).
     */
    public boolean tooSloped = false;

     /** TRUE if mode is WALK from start ot end (all legs are walking). */
    public final boolean walkOnly;

    /** TRUE if mode is a non transit move from start ot end (all legs are non-transit). */
    public final boolean streetOnly;


    /**
     * System notices is used to tag itineraries with system information. For example if you run the
     * itinerary-filter in debug mode, the filters would tag itineraries instead of deleting them
     * from the result. More than one filter might apply, so there can be more than one notice for
     * an itinerary. This is very handy, when tuning the system or debugging - looking for missing
     * expected trips.
     */
    public final List<SystemNotice> systemNotices = new ArrayList<>();


    /**
     * The cost of this trip
     */
    public Fare fare = new Fare();

    /**
     * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
     * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the
     * 6, then walks to their destination, has four legs.
     */
    public final List<Leg> legs;


    public Itinerary(List<Leg> legs) {
        this.legs = List.copyOf(legs);

        // Set aggregated data
        ItinerariesCalculateLegTotals totals = new ItinerariesCalculateLegTotals(legs);
        this.durationSeconds = totals.totalDurationSeconds;
        this.nTransfers = totals.transfers();
        this.transitTimeSeconds = totals.transitTimeSeconds;
        this.nonTransitTimeSeconds = totals.nonTransitTimeSeconds;
        this.nonTransitDistanceMeters = totals.nonTransitDistanceMeters;
        this.waitingTimeSeconds = totals.waitingTimeSeconds;
        this.walkOnly = totals.walkOnly;
        this.streetOnly = totals.streetOnly;
    }

    /**
     * Time that the trip departs.
     */
    public Calendar startTime() {
        return firstLeg().startTime;
    }

    /**
     * Time that the trip arrives.
     */
    public Calendar endTime() {
        return lastLeg().endTime;
    }

    /**
     * Reflects the departureDelay on the first Leg
     * Unit: seconds.
     */
    public int departureDelay() {
        return firstLeg().departureDelay;
    }
    /**
     * Reflects the arrivalDelay on the last Leg
     * Unit: seconds.
     */
    public int arrivalDelay() {
        return lastLeg().arrivalDelay;
    }


    /**
     * This is the amount of time used to travel. {@code waitingTime} is NOT
     * included.
     */
    public int effectiveDurationSeconds() {
        return transitTimeSeconds + nonTransitTimeSeconds;
    }

    /**
     * Total distance in meters.
     */
    public double distanceMeters() {
        return legs.stream().mapToDouble(it -> it.distanceMeters).sum();
    }

    /**
     * Return {@code true} if all legs are WALKING.
     */
    public boolean isWalkingAllTheWay() {
        return walkOnly;
    }

    /**
     * Return {@code true} if all legs are WALKING.
     */
    public boolean isOnStreetAllTheWay() {
        return streetOnly;
    }

    /** TRUE if alt least one leg is a transit leg. */
    public boolean hasTransit() {
        return transitTimeSeconds > 0;
    }

    public Leg firstLeg() {
        return legs.get(0);
    }

    public Leg lastLeg() {
        return legs.get(legs.size()-1);
    }

    /** Get the first transit leg if one exist */
    public Optional<Leg> firstTransitLeg() {
        return legs.stream().filter(Leg::isTransitLeg).findFirst();
    }

    /**
     * A itinerary can be tagged with a system notice. System notices should only be added to a
     * response if explicit asked for in the request.
     * <p>
     * For example when tuning or manually testing the itinerary-filter-chain it you can enable
     * the {@link org.opentripplanner.routing.api.request.RoutingRequest#debugItineraryFilter} and instead
     * of removing itineraries from the result the itineraries would be tagged by the filters
     * instead. This enable investigating, why an expected itinerary is missing from the result
     * set.
     */
    public void addSystemNotice(SystemNotice notice) {
        systemNotices.add(notice);
    }

    public void timeShiftToStartAt(Calendar afterTime) {
        Calendar startTimeFirstLeg = firstLeg().startTime;
        int adjustmentMilliSeconds =
            (int)(afterTime.getTimeInMillis() - startTimeFirstLeg.getTimeInMillis());
        timeShift(adjustmentMilliSeconds);
    }

    private void timeShift(int adjustmentMilliSeconds) {
        for (Leg leg : this.legs) {
            leg.startTime.add(Calendar.MILLISECOND, adjustmentMilliSeconds);
            leg.endTime.add(Calendar.MILLISECOND, adjustmentMilliSeconds);
        }
    }

    /**
     * Return {@code true} it the other object is the same object using the {@link
     * Object#equals(Object)}. An itinerary is a temporary object and the equals method should not
     * be used for comparision of 2 instances, only to check that to objects are the same instance.
     */
    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }

    /** @see #equals(Object) */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(Itinerary.class)
                .addStr("from", firstLeg().from.toStringShort())
                .addStr("to", lastLeg().to.toStringShort())
                .addCalTime("start", firstLeg().startTime)
                .addCalTime("end", lastLeg().endTime)
                .addNum("nTransfers", nTransfers, -1)
                .addDurationSec("duration", durationSeconds)
                .addNum("generalizedCost", generalizedCost)
                .addDurationSec("nonTransitTime", nonTransitTimeSeconds)
                .addDurationSec("transitTime", transitTimeSeconds)
                .addDurationSec("waitingTime", waitingTimeSeconds)
                .addNum("nonTransitDistance", nonTransitDistanceMeters, "m")
                .addBool("nonTransitLimitExceeded", nonTransitLimitExceeded)
                .addBool("tooSloped", tooSloped)
                .addNum("elevationLost", elevationLost, 0.0)
                .addNum("elevationGained", elevationGained, 0.0)
                .addCol("legs", legs)
                .addObj("fare", fare)
                .toString();
    }

    /**
     * Used to convert a list of itineraries to a SHORT human readable string.
     * @see #toStr()
     * <p>
     * It is great for comparing lists of itineraries in a test:
     * {@code assertEquals(toStr(List.of(it1)), toStr(result))}.
     */
    public static String toStr(List<Itinerary> list) {
        return list.stream().map(Itinerary::toStr).collect(Collectors.joining(", "));
    }

    /**
     * Used to convert an itinerary to a SHORT human readable string - including just a few of
     * the most important fields. It is much shorter and easier to read then the
     * {@link Itinerary#toString()}.
     * <p>
     * It is great for comparing to itineraries in a test:
     * {@code assertEquals(toStr(it1), toStr(it2))}.
     * <p>
     * Example: {@code A ~ Walk 2m ~ B ~ BUS 55 12:04 12:14 ~ C [cost: 1066]}
     * <p>
     * Reads: Start at A, walk 2 minutes to stop B, take bus 55, board at 12:04 and alight at
     * 12:14 ...
     */
    public String toStr() {
        PathStringBuilder buf = new PathStringBuilder();
        buf.stop(firstLeg().from.name);

        for (Leg leg : legs) {
            buf.sep();
            if(leg.isWalkingLeg()) {
                buf.walk((int)leg.getDuration());
            }
            else if(leg.isTransitLeg()) {
              buf.transit(leg.mode, leg.getTrip().logInfo(), leg.startTime, leg.endTime);
            }
            else {
                buf.other(leg.mode, leg.startTime, leg.endTime);
            }

            buf.sep();
            buf.stop(leg.to.name);
        }
        return buf.toString() + " [cost: " + generalizedCost + "]";
    }
}
