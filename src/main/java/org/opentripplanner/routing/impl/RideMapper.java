package org.opentripplanner.routing.impl;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert OTP2 Paths to a list of Ride objects used by the fare calculators.
 */
public class RideMapper {

    /** This class cannot be instantiated, it's just a collection of static methods. */
    private RideMapper () { throw new UnsupportedOperationException(); }

    /**
     * Convert transit legs in a Raptor Path into Rides, which are used by FareServices to calculate
     * fares. Adapted from from previously used method DefaultFareServiceImpl.createRides().
     */
    public static List<Ride> ridesForRaptorPath(Path path, TransitLayer transitLayer) {
        List<Ride> rides = new ArrayList<>();
        for (PathLeg leg = path.accessLeg().nextLeg(); !leg.isEgressLeg(); leg = leg.nextLeg()) {
            if (leg.isTransitLeg()) {
                rides.add(rideForTransitPathLeg(leg.asTransitLeg(), transitLayer));
            }
        }
        return rides;
    }

    public static Ride rideForTransitPathLeg(TransitPathLeg leg, TransitLayer transitLayer) {
        TransitPathLeg<TripSchedule> transitPathLeg = leg.asTransitLeg();
        TripSchedule tripSchedule = transitPathLeg.trip();
        Ride ride = new Ride();
        TripPattern tripPattern = tripSchedule.getOriginalTripPattern();
        ride.firstStop = transitLayer.getStopByIndex(transitPathLeg.fromStop());
        ride.lastStop = transitLayer.getStopByIndex(transitPathLeg.toStop());
        ride.startZone = ride.firstStop.getZone();
        ride.endZone = ride.lastStop.getZone();
        // In almost all cases (except some loop routes) this should get the right set of zones passed through.
        // We don't have the position of the stops within the pattern so can't readily get more accurate than this.
        boolean onBoard = false;
        for (Stop stop : tripPattern.getStops()) {
            if (stop == ride.firstStop) {
                onBoard = true;
            }
            if (onBoard) {
                ride.zones.add(stop.getZone());
                if (stop == ride.lastStop) break;
            }
        }
        ride.agency = tripPattern.route.getAgency().getId();
        ride.route = tripPattern.route.getId();
        ride.trip = tripSchedule.getOriginalTripTimes().trip.getId();
        // TODO verify that times are in seconds after midnight
        ride.startTime = transitPathLeg.fromTime();
        ride.endTime  = transitPathLeg.toTime();
        // In the default fare service, we classify rides by mode.
        ride.classifier = tripPattern.getMode();
        return ride;
    }

}
