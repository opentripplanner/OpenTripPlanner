package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.model.plan.Leg;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class TripTimeShortHelper {

    /**
     * Find trip time short for the from place in transit leg, or null.
     */
    public TripTimeShort getTripTimeShortForFromPlace(Leg leg, RoutingService routingService) {
        Trip trip = routingService.getTripForId().get(leg.tripId);
        if (trip == null) {
            return null;
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = routingService.getTripTimesShort(trip, serviceDate);
        long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;

        /* TODO OTP2
        if (leg.isFlexible()) {
            TripTimeShort tripTimeShort = tripTimes.get(leg.from.stopSequence);
            tripTimeShort.scheduledDeparture = (int) startTimeSeconds;
            tripTimeShort.realtimeDeparture = (int) startTimeSeconds;
            return tripTimeShort;
        }
         */

        if (leg.realTime) {
            return tripTimes.stream().filter(tripTime -> tripTime.realtimeDeparture == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId, routingService)).findFirst().orElse(null);
        }
        return tripTimes.stream().filter(tripTime -> tripTime.scheduledDeparture == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId, routingService)).findFirst().orElse(null);
    }

    /**
     * Find trip time short for the to place in transit leg, or null.
     */
    public TripTimeShort getTripTimeShortForToPlace(Leg leg, RoutingService routingService) {
        Trip trip = routingService.getTripForId().get(leg.tripId);
        if (trip == null) {
            return null;
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = routingService.getTripTimesShort(trip, serviceDate);
        long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;

        /* TODO OTP2
        if (leg.isFlexible()) {
            TripTimeShort tripTimeShort = tripTimes.get(leg.to.stopSequence);
            tripTimeShort.scheduledArrival = (int) endTimeSeconds;
            tripTimeShort.realtimeArrival = (int) endTimeSeconds;
            return tripTimeShort;
        }
        */

        if (leg.realTime) {
            return tripTimes.stream().filter(tripTime -> tripTime.realtimeArrival == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId, routingService)).findFirst().orElse(null);
        }
        return tripTimes.stream().filter(tripTime -> tripTime.scheduledArrival == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId, routingService)).findFirst().orElse(null);
    }


    /**
     * Find trip time shorts for all stops for the full trip of a leg.
     */
    public List<TripTimeShort> getAllTripTimeShortsForLegsTrip(Leg leg, RoutingService routingService) {
        if (leg.tripId == null || leg.serviceDate == null) {
            return new ArrayList<>();
        }
        Trip trip = routingService.getTripForId().get(leg.tripId);
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);
        return routingService.getTripTimesShort(trip, serviceDate);
    }

    /**
     * Find trip time shorts for all intermediate stops for a leg.
     */
    public List<TripTimeShort> getIntermediateTripTimeShortsForLeg(Leg leg, RoutingService routingService) {
        Trip trip = routingService.getTripForId().get(leg.tripId);

        if (trip == null) {
            return new ArrayList<>();
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = routingService.getTripTimesShort(trip, serviceDate);
        List<TripTimeShort> filteredTripTimes = new ArrayList<>();

        long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        boolean boardingStopFound = false;
        for (TripTimeShort tripTime : tripTimes) {

            long boardingTime = leg.realTime ? tripTime.realtimeDeparture : tripTime.scheduledDeparture;

            if (!boardingStopFound) {
                boardingStopFound |= boardingTime == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId, routingService);
                continue;
            }

            long arrivalTime = leg.realTime ? tripTime.realtimeArrival : tripTime.scheduledArrival;
            if (arrivalTime == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId, routingService)) {
                break;
            }

            filteredTripTimes.add(tripTime);
        }

        return filteredTripTimes;
    }


    private ServiceDate parseServiceDate(String serviceDateString) {
        ServiceDate serviceDate;
        try {
            serviceDate = ServiceDate.parseString(serviceDateString);
        } catch (ParseException pe) {
            throw new RuntimeException("Unparsable service date: " + serviceDateString, pe);
        }
        return serviceDate;
    }

    private boolean matchesQuayOrSiblingQuay(FeedScopedId quayId, FeedScopedId candidate, RoutingService routingService) {
        boolean foundMatch = quayId.equals(candidate);
        if (!foundMatch) {
            //Check parentStops
            Stop stop = routingService.getStopForId(quayId);
            if (stop != null && stop.isPartOfStation()) {
                Station parentStation = stop.getParentStation();
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
