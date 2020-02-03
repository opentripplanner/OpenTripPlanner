package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class TripTimeShortHelper {

    private GraphIndex index;

    public TripTimeShortHelper(GraphIndex index) {
        this.index = index;
    }

    public List<TripTimeShort> getTripTimesShort(Trip trip, ServiceDate serviceDate) {
        final ServiceDay serviceDay = new ServiceDay(index.graph, serviceDate,
                                                            index.graph.getCalendarService(), trip.getRoute().getAgency().getId());
        TimetableSnapshot timetableSnapshot = index.graph.getTimetableSnapshot();
        Timetable timetable = null;
        if (timetableSnapshot != null) {
            // Check if realtime-data is available for trip

            TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(
                    trip.getId(), serviceDate);
            if (pattern == null) {
                pattern = index.patternForTrip.get(trip);
            }
            timetable = timetableSnapshot.resolve(pattern, serviceDate);
        }
        if (timetable == null) {
            timetable = index.patternForTrip.get(trip).scheduledTimetable;
        }

        // This check is made here to avoid changing TripTimeShort.fromTripTimes
        TripTimes times = timetable.getTripTimes(timetable.getTripIndex(trip.getId()));
        if (!serviceDay.serviceRunning(times.serviceCode)) {
            return new ArrayList<>();
        }
        else {
            return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
        }
    }

    /**
     * Find trip time short for the from place in transit leg, or null.
     */
    public TripTimeShort getTripTimeShortForFromPlace(Leg leg) {
        Trip trip = index.tripForId.get(leg.tripId);
        if (trip == null) {
            return null;
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = getTripTimesShort(trip, serviceDate);
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
            return tripTimes.stream().filter(tripTime -> tripTime.realtimeDeparture == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId)).findFirst().orElse(null);
        }
        return tripTimes.stream().filter(tripTime -> tripTime.scheduledDeparture == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId)).findFirst().orElse(null);
    }

    /**
     * Find trip time short for the to place in transit leg, or null.
     */
    public TripTimeShort getTripTimeShortForToPlace(Leg leg) {
        Trip trip = index.tripForId.get(leg.tripId);
        if (trip == null) {
            return null;
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = getTripTimesShort(trip, serviceDate);
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
            return tripTimes.stream().filter(tripTime -> tripTime.realtimeArrival == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId)).findFirst().orElse(null);
        }
        return tripTimes.stream().filter(tripTime -> tripTime.scheduledArrival == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId)).findFirst().orElse(null);
    }


    /**
     * Find trip time shorts for all stops for the full trip of a leg.
     */
    public List<TripTimeShort> getAllTripTimeShortsForLegsTrip(Leg leg) {
        if (leg.tripId == null || leg.serviceDate == null) {
            return new ArrayList<>();
        }
        Trip trip = index.tripForId.get(leg.tripId);
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);
        return getTripTimesShort(trip, serviceDate);
    }

    /**
     * Find trip time shorts for all intermediate stops for a leg.
     */
    public List<TripTimeShort> getIntermediateTripTimeShortsForLeg(Leg leg) {
        Trip trip = index.tripForId.get(leg.tripId);

        if (trip == null) {
            return new ArrayList<>();
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = getTripTimesShort(trip, serviceDate);
        List<TripTimeShort> filteredTripTimes = new ArrayList<>();

        long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        boolean boardingStopFound = false;
        for (TripTimeShort tripTime : tripTimes) {

            long boardingTime = leg.realTime ? tripTime.realtimeDeparture : tripTime.scheduledDeparture;

            if (!boardingStopFound) {
                boardingStopFound |= boardingTime == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId);
                continue;
            }

            long arrivalTime = leg.realTime ? tripTime.realtimeArrival : tripTime.scheduledArrival;
            if (arrivalTime == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId)) {
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

    private boolean matchesQuayOrSiblingQuay(FeedScopedId quayId, FeedScopedId candidate) {
        boolean foundMatch = quayId.equals(candidate);
        if (!foundMatch) {
            //Check parentStops
            Stop stop = index.stopForId.get(quayId);
            if (stop != null && stop.getParentStation() != null) {
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
