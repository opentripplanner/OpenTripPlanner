package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.RoutingService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
        long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;

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

        if (leg.realTime) {
            return tripTimes.stream().filter(tripTime -> tripTime.getRealtimeDeparture() == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stop,
                tripTime.getStopId())).findFirst().orElse(null);
        }
        return tripTimes.stream().filter(tripTime -> tripTime.getScheduledDeparture() == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stop,
            tripTime.getStopId())).findFirst().orElse(null);
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
        long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;

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

        if (leg.realTime) {
            return tripTimes.stream().filter(tripTime -> tripTime.getRealtimeArrival() == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stop,
                tripTime.getStopId())).findFirst().orElse(null);
        }
        return tripTimes.stream().filter(tripTime -> tripTime.getScheduledArrival() == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stop,
            tripTime.getStopId())).findFirst().orElse(null);
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
        List<TripTimeOnDate> filteredTripTimes = new ArrayList<>();

        long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        boolean boardingStopFound = false;
        for (TripTimeOnDate tripTime : tripTimes) {

            long boardingTime = leg.realTime ? tripTime.getRealtimeDeparture() : tripTime.getScheduledDeparture();

            if (!boardingStopFound) {
                boardingStopFound = boardingTime == startTimeSeconds
                    && matchesQuayOrSiblingQuay(leg.from.stop, tripTime.getStopId());
                continue;
            }

            long arrivalTime = leg.realTime ? tripTime.getRealtimeArrival() : tripTime.getScheduledArrival();
            if (arrivalTime == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stop, tripTime.getStopId())) {
                break;
            }

            filteredTripTimes.add(tripTime);
        }

        return filteredTripTimes;
    }


    /* private methods */

    private static boolean matchesQuayOrSiblingQuay(StopLocation stop, FeedScopedId candidate) {
        if (stop == null) return false;
        boolean foundMatch = stop.getId().equals(candidate);
        if (!foundMatch && stop instanceof Stop) {
            if (((Stop) stop).isPartOfStation()) {
                Station parentStation = ((Stop) stop).getParentStation();
                for (Stop childStop : parentStation.getChildStops()) {
                    if (childStop.getId().equals(candidate)) {
                        return true;
                    }
                }
            }
        }
        return foundMatch;
    }
}
