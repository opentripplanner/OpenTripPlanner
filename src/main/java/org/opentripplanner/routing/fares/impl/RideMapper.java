package org.opentripplanner.routing.fares.impl;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    public static List<Ride> ridesForItinerary(Itinerary itinerary) {
        return itinerary.legs.stream()
                .filter(leg -> leg.isTransitLeg() || leg.isFlexibleTrip())
                .map(RideMapper::rideForTransitPathLeg)
                .collect(Collectors.toList());
    }

    public static Ride rideForTransitPathLeg(Leg leg) {
        Ride ride = new Ride();
        ride.firstStop = leg.getFrom().stop;
        ride.lastStop = leg.getTo().stop;

        ride.startZone = ride.firstStop.getFirstZoneAsString();
        ride.endZone = ride.lastStop.getFirstZoneAsString();

        var zones = leg.getIntermediateStops().stream()
                .map(stopArrival -> stopArrival.place.stop.getFirstZoneAsString())
                .collect(Collectors.toSet());

        zones.addAll(Stream.of(ride.startZone, ride.endZone)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        ride.zones = zones;
        ride.agency = leg.getRoute().getAgency().getId();
        ride.route = leg.getRoute().getId();
        ride.trip = leg.getTrip().getId();

        ride.startTime = toZonedDateTime(leg.getStartTime());
        ride.endTime = toZonedDateTime(leg.getEndTime());

        // In the default fare service, we classify rides by mode.
        ride.classifier = leg.getMode();
        return ride;
    }

    private static ZonedDateTime toZonedDateTime(Calendar time) {
        return time.toInstant().atZone(time.getTimeZone().toZoneId());
    }

}
