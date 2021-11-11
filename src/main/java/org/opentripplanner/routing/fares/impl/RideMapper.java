package org.opentripplanner.routing.fares.impl;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

/**
 * Convert an OTP2 Itinerary to a list of Ride objects used by the fare calculators.
 */
public class RideMapper {

    /**
     * This class cannot be instantiated, it's just a collection of static methods.
     */
    private RideMapper() {throw new UnsupportedOperationException();}

    /**
     * Convert transit legs in a Raptor Path into Rides, which are used by FareServices to calculate
     * fares. Adapted from from previously used method DefaultFareServiceImpl.createRides().
     */
    public static List<Ride> ridesForRaptorPath(Itinerary itinerary) {
        return itinerary.legs.stream()
                .filter(leg -> leg.isTransitLeg() || leg.flexibleTrip)
                .map(RideMapper::rideForTransitPathLeg)
                .collect(Collectors.toList());
    }

    public static Ride rideForTransitPathLeg(Leg leg) {
        Ride ride = new Ride();
        ride.firstStop = leg.from.stop;
        ride.lastStop = leg.to.stop;

        ride.startZone = ride.firstStop.getFirstZoneAsString();
        ride.endZone = ride.lastStop.getFirstZoneAsString();

        var zones = leg.intermediateStops.stream()
                .map(stopArrival -> stopArrival.place.stop.getFirstZoneAsString())
                .collect(Collectors.toSet());

        zones.addAll(List.of(ride.startZone, ride.endZone));

        ride.zones = zones;
        ride.agency = leg.getRoute().getAgency().getId();
        ride.route = leg.getRoute().getId();
        ride.trip = leg.getTrip().getId();

        ride.startTime = toLocalTime(leg.startTime, leg);
        ride.endTime = toLocalTime(leg.endTime, leg);

        // In the default fare service, we classify rides by mode.
        ride.classifier = leg.mode;
        return ride;
    }

    private static LocalTime toLocalTime(Calendar time, Leg leg) {
        return time.toInstant()
                .atZone(time.getTimeZone().toZoneId())
                .toLocalTime();
    }

}
