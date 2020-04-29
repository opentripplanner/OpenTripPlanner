package org.opentripplanner.ext.siri;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.CallStatusEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.MonitoredCallStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityStructure;

import javax.xml.datatype.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;

public class TimetableHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TimetableHelper.class);

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param journey  SIRI-ET EstimatedVehicleJourney
     * @param timeZone time zone of trip update
     * @param tripId
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static TripTimes createUpdatedTripTimes(final Graph graph, Timetable timetable, EstimatedVehicleJourney journey, TimeZone timeZone, FeedScopedId tripId) {
        if (journey == null) {
            return null;
        }

        int tripIndex = timetable.getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.debug("tripId {} not found in pattern.", tripId);
            return null;
        }

        final TripTimes existingTripTimes = timetable.getTripTimes(tripIndex);
        TripTimes oldTimes = new TripTimes(existingTripTimes);

        if (journey.isCancellation() != null && journey.isCancellation()) {
            oldTimes.cancel();
            return oldTimes;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
        EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();

        if (journeyEstimatedCalls == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls = journeyEstimatedCalls.getEstimatedCalls();
        List<RecordedCall> recordedCalls;
        if (journeyRecordedCalls != null) {
            recordedCalls = journeyRecordedCalls.getRecordedCalls();
        } else {
            recordedCalls = new ArrayList<>();
        }

        boolean stopPatternChanged = false;

        Stop[] modifiedStops = timetable.pattern.stopPattern.stops;

        Trip trip = getTrip(tripId, timetable);

        List<StopTime> modifiedStopTimes = createModifiedStopTimes(timetable, oldTimes, journey, trip, new RoutingService(graph));
        if (modifiedStopTimes == null) {
            return null;
        }
        TripTimes newTimes = new TripTimes(trip, modifiedStopTimes, graph.deduplicator);

        //Populate missing data from existing TripTimes
        newTimes.serviceCode = oldTimes.serviceCode;

        int callCounter = 0;
        ZonedDateTime departureDate = null;
        Set<Object> alreadyVisited = new HashSet<>();

        boolean isJourneyPredictionInaccurate =  (journey.isPredictionInaccurate() != null && journey.isPredictionInaccurate());

        int departureFromPreviousStop = 0;
        int lastArrivalDelay = 0;
        int lastDepartureDelay = 0;
        for (Stop stop : modifiedStops) {
            boolean foundMatch = false;

            for (RecordedCall recordedCall : recordedCalls) {
                if (alreadyVisited.contains(recordedCall)) {
                    continue;
                }
                //Current stop is being updated
                foundMatch = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

                if (!foundMatch && stop.isPartOfStation()) {
                    Stop alternativeStop = graph.index.getStopForId(
                            new FeedScopedId(stop.getId().getFeedId(), recordedCall.getStopPointRef().getValue())
                    );
                    if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                        foundMatch = true;
                        stopPatternChanged = true;
                    }
                }

                if (foundMatch) {
                    if (departureDate == null) {
                        departureDate = recordedCall.getAimedDepartureTime();
                        if (departureDate == null) {
                            departureDate = recordedCall.getAimedArrivalTime();
                        }
                        if (oldTimes.getDepartureTime(0) > 86400) {
                            // The "departure-date" for this trip is set to "yesterday" (or before) even though it actually departs "today"

                            int dayOffsetCount = oldTimes.getDepartureTime(0)/86400; // calculate number of offset-days

                            departureDate = departureDate.minusDays(dayOffsetCount);
                        }
                    }

                    //Flag as recorded
                    newTimes.setRecorded(callCounter, true);

                    if (recordedCall.isCancellation() != null) {
                        newTimes.setCancelledStop(callCounter, recordedCall.isCancellation());
                    }

                    newTimes.setDropoffType(callCounter, timetable.pattern.stopPattern.dropoffs[callCounter]);
                    newTimes.setPickupType(callCounter, timetable.pattern.stopPattern.pickups[callCounter]);

                    int arrivalTime = newTimes.getArrivalTime(callCounter);
                    int realtimeArrivalTime = arrivalTime;
                    if (recordedCall.getActualArrivalTime() != null) {
                        realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getActualArrivalTime());
                    } else if (recordedCall.getExpectedArrivalTime() != null) {
                        realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getExpectedArrivalTime());
                    } else if (recordedCall.getAimedArrivalTime() != null) {
                        realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getAimedArrivalTime());
                    }
                    int arrivalDelay = realtimeArrivalTime - arrivalTime;
                    newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                    lastArrivalDelay = arrivalDelay;

                    int departureTime = newTimes.getDepartureTime(callCounter);
                    int realtimeDepartureTime = departureTime;
                    if (recordedCall.getActualDepartureTime() != null) {
                        realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getActualDepartureTime());
                    } else if (recordedCall.getExpectedDepartureTime() != null) {
                        realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getExpectedDepartureTime());
                    } else if (recordedCall.getAimedDepartureTime() != null) {
                        realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getAimedDepartureTime());
                    }
                    if (realtimeDepartureTime < realtimeArrivalTime) {
                        realtimeDepartureTime = realtimeArrivalTime;
                    }
                    int departureDelay = realtimeDepartureTime - departureTime;

                    newTimes.updateDepartureDelay(callCounter, departureDelay);
                    lastDepartureDelay = departureDelay;
                    departureFromPreviousStop = newTimes.getDepartureTime(callCounter);

                    alreadyVisited.add(recordedCall);
                    break;
                }
            }
            if (!foundMatch) {
                for (EstimatedCall estimatedCall : estimatedCalls) {
                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    //Current stop is being updated
                    foundMatch = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!foundMatch && stop.isPartOfStation()) {
                        Stop alternativeStop = graph.index
                            .getStopForId(
                                    new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue())
                            );
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            foundMatch = true;
                            stopPatternChanged = true;
                        }
                    }

                    if (foundMatch) {
                        if (departureDate == null) {
                            departureDate = estimatedCall.getAimedDepartureTime();
                            if (departureDate == null) {
                                departureDate = estimatedCall.getAimedArrivalTime();
                            }
                        }

                        if (estimatedCall.isCancellation() != null) {
                            newTimes.setCancelledStop(callCounter, estimatedCall.isCancellation());
                        }

                        boolean isCallPredictionInaccurate = estimatedCall.isPredictionInaccurate() != null && estimatedCall.isPredictionInaccurate();

                        // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
                        newTimes.setPredictionInaccurate(callCounter, (isJourneyPredictionInaccurate | isCallPredictionInaccurate));

                        // Update dropoff-/pickuptype only if status is cancelled
                        CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
                        if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
                            newTimes.setDropoffType(callCounter, PICKDROP_NONE);
                        }

                        CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
                        if (departureStatus == CallStatusEnumeration.CANCELLED) {
                            newTimes.setPickupType(callCounter, PICKDROP_NONE);
                        }

                        int arrivalTime = newTimes.getArrivalTime(callCounter);
                        int realtimeArrivalTime = -1;
                        if (estimatedCall.getExpectedArrivalTime() != null) {
                            realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getExpectedArrivalTime());
                        } else if (estimatedCall.getAimedArrivalTime() != null) {
                            realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getAimedArrivalTime());
                        }

                        int departureTime = newTimes.getDepartureTime(callCounter);
                        int realtimeDepartureTime = departureTime;
                        if (estimatedCall.getExpectedDepartureTime() != null) {
                            realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getExpectedDepartureTime());
                        } else if (estimatedCall.getAimedDepartureTime() != null) {
                            realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getAimedDepartureTime());
                        }

                        if (realtimeArrivalTime == -1) {
                            realtimeArrivalTime = realtimeDepartureTime;
                        }
                        if (realtimeDepartureTime < realtimeArrivalTime) {
                            realtimeDepartureTime = realtimeArrivalTime;
                        }

                        int arrivalDelay = realtimeArrivalTime - arrivalTime;
                        newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                        lastArrivalDelay = arrivalDelay;

                        int departureDelay = realtimeDepartureTime - departureTime;
                        newTimes.updateDepartureDelay(callCounter, departureDelay);
                        lastDepartureDelay = departureDelay;

                        departureFromPreviousStop = newTimes.getDepartureTime(callCounter);

                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }
            if (!foundMatch) {

                if (timetable.pattern.stopPattern.pickups[callCounter] == PICKDROP_NONE &&
                        timetable.pattern.stopPattern.dropoffs[callCounter] == PICKDROP_NONE) {
                    // When newTimes contains stops without pickup/dropoff - set both arrival/departure to previous stop's departure
                    // This necessary to accommodate the case when delay is reduced/eliminated between to stops with pickup/dropoff, and
                    // multiple non-pickup/dropoff stops are in between.
                    newTimes.updateArrivalTime(callCounter, departureFromPreviousStop);
                    newTimes.updateDepartureTime(callCounter, departureFromPreviousStop);
                } else {

                    int arrivalDelay = lastArrivalDelay;
                    int departureDelay = lastDepartureDelay;

                    if (lastArrivalDelay == 0 && lastDepartureDelay == 0) {
                        //No match has been found yet (i.e. still in RecordedCalls) - keep existing delays
                        arrivalDelay = existingTripTimes.getArrivalDelay(callCounter);
                        departureDelay = existingTripTimes.getDepartureDelay(callCounter);
                    }

                    newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                    newTimes.updateDepartureDelay(callCounter, departureDelay);
                }

                departureFromPreviousStop = newTimes.getDepartureTime(callCounter);
            }
            callCounter++;
        }

        if (stopPatternChanged) {
            // This update modified stopPattern
            newTimes.setRealTimeState(RealTimeState.MODIFIED);
        } else {
            // This is the first update, and StopPattern has not been changed
            newTimes.setRealTimeState(RealTimeState.UPDATED);
        }

        if (journey.isCancellation() != null && journey.isCancellation()) {
            LOG.debug("Trip is cancelled");
            newTimes.cancel();
        }

        if (!newTimes.timesIncreasing()) {
            LOG.info("TripTimes are non-increasing after applying SIRI delay propagation - LineRef {}, TripId {}.", journey.getLineRef().getValue(), tripId);
            return null;
        }

        if (newTimes.getNumStops() != timetable.pattern.stopPattern.stops.length) {
            return null;
        }

        LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
        return newTimes;
    }


    /**
     * Apply the SIRI ET to the appropriate TripTimes from this Timetable.
     * Calculate new stoppattern based on single stop cancellations
     *
     * @param journey    SIRI-ET EstimatedVehicleJourney
     * @param routingService
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static List<Stop> createModifiedStops(Timetable timetable, EstimatedVehicleJourney journey, RoutingService routingService) {
        if (journey == null) {
            return null;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
        EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();

        if (journeyEstimatedCalls == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls = journeyEstimatedCalls.getEstimatedCalls();

        List<RecordedCall> recordedCalls;
        if (journeyRecordedCalls != null) {
            recordedCalls = journeyRecordedCalls.getRecordedCalls();
        } else {
            recordedCalls = new ArrayList<>();
        }

        //Get all scheduled stops
        Stop[] stops = timetable.pattern.stopPattern.stops;

        List<Stop> modifiedStops = new ArrayList<>();

        for (int i = 0; i < stops.length; i++) {
            Stop stop = stops[i];

            boolean foundMatch = false;
            if (i < recordedCalls.size()) {
                for (RecordedCall recordedCall : recordedCalls) {

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        Stop alternativeStop = routingService
                            .getStopForId(new FeedScopedId(stop.getId().getFeedId(), recordedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stop = alternativeStop;
                        }
                    }

                    if (stopsMatchById) {
                        foundMatch = true;
                        modifiedStops.add(stop);
                        break;
                    }
                }
            } else {
                for (EstimatedCall estimatedCall : estimatedCalls) {

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        Stop alternativeStop = routingService
                            .getStopForId(new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stop = alternativeStop;
                        }
                    }

                    if (stopsMatchById) {
                        foundMatch = true;
                        modifiedStops.add(stop);
                        break;
                    }
                }
            }
            if (!foundMatch) {
                modifiedStops.add(stop);
            }
        }


        return modifiedStops;
    }

    /**
     * Apply the SIRI ET to the appropriate TripTimes from this Timetable.
     * Calculate new stoppattern based on single stop cancellations
     *
     * @param oldTimes
     * @param journey    SIRI-ET EstimatedVehicleJourney
     * @param trip
     * @param routingService
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static List<StopTime> createModifiedStopTimes(Timetable timetable, TripTimes oldTimes, EstimatedVehicleJourney journey, Trip trip, RoutingService routingService) {
        if (journey == null) {
            return null;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyCalls = journey.getEstimatedCalls();

        if (journeyCalls == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls = journeyCalls.getEstimatedCalls();

        List<Stop> stops = createModifiedStops(timetable, journey, routingService);

        List<StopTime> modifiedStops = new ArrayList<>();

        ZonedDateTime departureDate = null;
        int numberOfRecordedCalls = (journey.getRecordedCalls() != null && journey.getRecordedCalls().getRecordedCalls() != null) ? journey.getRecordedCalls().getRecordedCalls().size() : 0;
        Set<Object> alreadyVisited = new HashSet<>();
        // modify updated stop-times
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);

            final StopTime stopTime = new StopTime();
            stopTime.setStop(stop);
            stopTime.setTrip(trip);
            stopTime.setStopSequence(i);
            stopTime.setDropOffType(timetable.pattern.stopPattern.dropoffs[i]);
            stopTime.setPickupType(timetable.pattern.stopPattern.pickups[i]);
            stopTime.setArrivalTime(oldTimes.getScheduledArrivalTime(i));
            stopTime.setDepartureTime(oldTimes.getScheduledDepartureTime(i));
            stopTime.setStopHeadsign(oldTimes.getHeadsign(i));

            // TODO: Do we need to set the StopTime.id?
            //stopTime.setId(oldTimes.getStopTimeIdByIndex(i));

            boolean foundMatch = false;
            if (i >= numberOfRecordedCalls) {
                for (EstimatedCall estimatedCall : estimatedCalls) {
                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    if (departureDate == null) {
                        departureDate = (estimatedCall.getAimedDepartureTime() != null ? estimatedCall.getAimedDepartureTime() : estimatedCall.getAimedArrivalTime());
                    }

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        Stop alternativeStop = routingService
                            .getStopForId(new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stopTime.setStop(alternativeStop);
                        }

                    }

                    if (stopsMatchById) {
                        foundMatch = true;

                        CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
                        if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
                            stopTime.setDropOffType(PICKDROP_NONE);
                        } else if (estimatedCall.getArrivalBoardingActivity() == ArrivalBoardingActivityEnumeration.ALIGHTING) {
                            stopTime.setDropOffType(PICKDROP_SCHEDULED);
                        } else if (estimatedCall.getArrivalBoardingActivity() == ArrivalBoardingActivityEnumeration.NO_ALIGHTING) {
                            stopTime.setDropOffType(PICKDROP_NONE);
                        } else if (estimatedCall.getArrivalBoardingActivity() == null && i == 0) {
                            //First stop - default no pickup
                            stopTime.setDropOffType(PICKDROP_NONE);
                        }

                        CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
                        if (departureStatus == CallStatusEnumeration.CANCELLED) {
                            stopTime.setPickupType(PICKDROP_NONE);
                        } else if (estimatedCall.getDepartureBoardingActivity() == DepartureBoardingActivityEnumeration.BOARDING) {
                            stopTime.setPickupType(PICKDROP_SCHEDULED);
                        } else if (estimatedCall.getDepartureBoardingActivity() == DepartureBoardingActivityEnumeration.NO_BOARDING) {
                            stopTime.setPickupType(PICKDROP_NONE);
                        } else if (estimatedCall.getDepartureBoardingActivity() == null && i == (stops.size()-1)) {
                            //Last stop - default no dropoff
                            stopTime.setPickupType(PICKDROP_NONE);
                        }

                        if (estimatedCall.getDestinationDisplaies() != null && !estimatedCall.getDestinationDisplaies().isEmpty()) {
                            NaturalLanguageStringStructure destinationDisplay = estimatedCall.getDestinationDisplaies().get(0);
                            stopTime.setStopHeadsign(destinationDisplay.getValue());
                        }

                        modifiedStops.add(stopTime);
                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }

            if (!foundMatch) {
                modifiedStops.add(stopTime);
            }
        }

        return modifiedStops;
    }

    private static int calculateSecondsSinceMidnight(ZonedDateTime departureDate, ZonedDateTime dateTime) {

        int daysBetween = 0;
        if (departureDate.getDayOfMonth() != dateTime.getDayOfMonth()) {
            ZonedDateTime midnightOnDepartureDate = departureDate.withHour(0).withMinute(0).withSecond(0);
            ZonedDateTime midnightOnCurrentStop = dateTime.withHour(0).withMinute(0).withSecond(0);
            daysBetween = (int) ChronoUnit.DAYS.between(midnightOnDepartureDate, midnightOnCurrentStop);
        }
        // If first departure was 'yesterday' - add 24h
        int daysSinceDeparture = daysBetween * (24 * 60 * 60);

        return dateTime.toLocalTime().toSecondOfDay() + daysSinceDeparture;
    }

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param activity SIRI-VM VehicleActivity
     * @param timeZone time zone of trip update
     * @param tripId
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static TripTimes createUpdatedTripTimes(Timetable timetable, Graph graph, VehicleActivityStructure activity, TimeZone timeZone, FeedScopedId tripId) {
        if (activity == null) {
            return null;
        }

        MonitoredVehicleJourneyStructure mvj = activity.getMonitoredVehicleJourney();


        int tripIndex = timetable.getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.trace("tripId {} not found in pattern.", tripId);
            return null;
        }

        final TripTimes existingTripTimes = timetable.getTripTimes(tripIndex);
        TripTimes newTimes = new TripTimes(existingTripTimes);


        MonitoredCallStructure update = mvj.getMonitoredCall();
        if (update == null) {
            return null;
        }
        final List<Stop> stops = timetable.pattern.getStops();

        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();

        Duration delay = null;
        if (monitoredVehicleJourney != null) {
            delay = monitoredVehicleJourney.getDelay();
            int updatedDelay = 0;
            if (delay != null) {
                updatedDelay = delay.getSign() * (delay.getHours() * 3600 + delay.getMinutes() * 60 + delay.getSeconds());
            }

            MonitoredCallStructure monitoredCall = monitoredVehicleJourney.getMonitoredCall();
            if (monitoredCall != null && monitoredCall.getStopPointRef() != null) {
                boolean matchFound = false;

                int arrivalDelay = 0;
                int departureDelay = 0;

                for (int index = 0; index < newTimes.getNumStops(); ++index) {
                    if (!matchFound) {
                        // Delay is set on a single stop at a time. When match is found - propagate delay on all following stops
                        final Stop stop = stops.get(index);

                        matchFound = stop.getId().getId().equals(monitoredCall.getStopPointRef().getValue());

                        if (!matchFound && stop.isPartOfStation()) {
                            FeedScopedId alternativeId = new FeedScopedId(stop.getId().getFeedId(), monitoredCall.getStopPointRef().getValue());
                            Stop alternativeStop = graph.index.getStopForId(alternativeId);
                            if (alternativeStop != null && alternativeStop.isPartOfStation()) {
                                matchFound = stop.isPartOfSameStationAs(alternativeStop);
                            }
                        }


                        if (matchFound) {
                            arrivalDelay = departureDelay = updatedDelay;
                        } else {
                            /*
                             * If updated delay is less than previously set delay, the existing delay needs to be adjusted to avoid
                             * non-increasing times causing updates to be rejected. Will only affect historical data.
                             */
                            arrivalDelay = Math.min(existingTripTimes.getArrivalDelay(index), updatedDelay);
                            departureDelay =  Math.min(existingTripTimes.getDepartureDelay(index), updatedDelay);
                        }
                    }
                    newTimes.updateArrivalDelay(index, arrivalDelay);
                    newTimes.updateDepartureDelay(index, departureDelay);
                }
            }
        }

        if (!newTimes.timesIncreasing()) {
            LOG.info("TripTimes are non-increasing after applying SIRI delay propagation - delay: {}", delay);
            return null;
        }

        //If state is already MODIFIED - keep existing state
        if (newTimes.getRealTimeState() != RealTimeState.MODIFIED) {
            // Make sure that updated trip times have the correct real time state
            newTimes.setRealTimeState(RealTimeState.UPDATED);
        }

        return newTimes;
    }
    /**
     * @return the matching Trip in this particular Timetable
     */
    public static Trip getTrip(FeedScopedId tripId, Timetable timetable) {
        for (TripTimes tt : timetable.tripTimes) {
            if (tt.trip.getId().equals(tripId)) {
                return tt.trip;
            }
        }
        return null;
    }
}
