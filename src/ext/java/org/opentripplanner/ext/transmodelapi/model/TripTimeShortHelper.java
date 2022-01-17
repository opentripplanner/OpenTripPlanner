package org.opentripplanner.ext.transmodelapi.model;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.RoutingService;

public class TripTimeShortHelper {

    /** Utility class with private constructor to prevent instantiation. */
    private TripTimeShortHelper() { }

    /**
     * Find trip time short for the from place in transit leg, or null.
     */
    @Nullable
    public static TripTimeOnDate getTripTimeShortForFromPlace(Leg leg, RoutingService routingService) {
        if (!leg.isTransitLeg()) { return null; }
        if (leg.flexibleTrip) { return null; }

        ServiceDate serviceDate = leg.serviceDate;
        List<TripTimeOnDate> tripTimes = routingService.getTripTimesShort(leg.getTrip(), serviceDate);

        /* TODO OTP2 This method is only used for EstimatedCalls for from place. We have to decide
                     if EstimatedCalls are applicable to flex trips, and if that is the case, add
                     the necessary mappings.
        if (leg.isFlexible()) {
            TripTimeShort tripTimeShort = tripTimes.get(leg.from.stopSequence);
            tripTimeShort.scheduledDeparture = (int) startTimeSeconds;
            tripTimeShort.realtimeDeparture = (int) startTimeSeconds;
            return tripTimeShort;
        }
         */

        return tripTimes.get(leg.boardStopPosInPattern);
    }

    /**
     * Find trip time short for the to place in transit leg, or null.
     */
    @Nullable
    public static TripTimeOnDate getTripTimeShortForToPlace(Leg leg, RoutingService routingService) {
        if (!leg.isTransitLeg()) { return null; }
        if (leg.flexibleTrip) { return null; }

        ServiceDate serviceDate = leg.serviceDate;
        List<TripTimeOnDate> tripTimes = routingService.getTripTimesShort(leg.getTrip(), serviceDate);

        /* TODO OTP2 This method is only used for EstimatedCalls for to place. We have to decide
                     if EstimatedCalls are applicable to flex trips, and if that is the case, add
                     the necessary mappings.
        if (leg.isFlexible()) {
            TripTimeShort tripTimeShort = tripTimes.get(leg.to.stopSequence);
            tripTimeShort.scheduledArrival = (int) endTimeSeconds;
            tripTimeShort.realtimeArrival = (int) endTimeSeconds;
            return tripTimeShort;
        }
        */

        return tripTimes.get(leg.alightStopPosInPattern);
    }


    /**
     * Find trip time shorts for all stops for the full trip of a leg.
     */
    public static List<TripTimeOnDate> getAllTripTimeShortsForLegsTrip(Leg leg, RoutingService routingService) {
        if (!leg.isTransitLeg()) { return List.of(); }
        if (leg.flexibleTrip) { return List.of(); }

        ServiceDate serviceDate = leg.serviceDate;
        return routingService.getTripTimesShort(leg.getTrip(), serviceDate);
    }

    /**
     * Find trip time shorts for all intermediate stops for a leg.
     */
    public static List<TripTimeOnDate> getIntermediateTripTimeShortsForLeg(Leg leg, RoutingService routingService) {
        if (!leg.isTransitLeg()) { return List.of(); }
        if (leg.flexibleTrip) { return List.of(); }

        ServiceDate serviceDate = leg.serviceDate;

        List<TripTimeOnDate> tripTimes = routingService.getTripTimesShort(leg.getTrip(), serviceDate);
        return tripTimes.subList(leg.boardStopPosInPattern + 1, leg.alightStopPosInPattern);
    }
}
