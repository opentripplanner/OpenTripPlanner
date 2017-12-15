package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private static final int DAY_IN_SECONDS = 3600 * 24;

    private String currentHeadsign;

    public void mapTripPattern(JourneyPattern journeyPattern, OtpTransitServiceBuilder transitBuilder,
            NetexDao netexDao) {
        TripMapper tripMapper = new TripMapper();

        List<Trip> trips = new ArrayList<>();

        //find matching journey pattern
        Collection<ServiceJourney> serviceJourneys = netexDao.lookupServiceJourneysById(
                journeyPattern.getId());

        StopPattern stopPattern = null;

        Route route = netexDao.lookupRouteById(journeyPattern.getRouteRef().getRef());
        org.opentripplanner.model.Route otpRoute = transitBuilder.getRoutes()
                .get(FeedScopedIdFactory.createFeedScopedId(route.getLineRef().getValue().getRef()));

        if (serviceJourneys == null || serviceJourneys.isEmpty()) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId()
                    + " does not contain any serviceJourneys.");
            return;
        }

        for (ServiceJourney serviceJourney : serviceJourneys) {
            Trip trip = tripMapper.mapServiceJourney(serviceJourney, transitBuilder, netexDao);
            trips.add(trip);

            TimetabledPassingTimes_RelStructure passingTimes = serviceJourney.getPassingTimes();
            List<TimetabledPassingTime> timetabledPassingTimes = passingTimes.getTimetabledPassingTime();

            List<StopTime> stopTimes = mapToStopTimes(
                    journeyPattern, transitBuilder, netexDao, trip, timetabledPassingTimes
            );

            if (stopTimes != null) {
                transitBuilder.getStopTimesSortedByTrip().replace(trip, stopTimes);

                // Set first headsign as trip headsign
                if (stopTimes.size() > 0) {
                    trip.setTripHeadsign(stopTimes.stream().findFirst().get().getStopHeadsign());
                }
                else {
                    trip.setTripHeadsign("");
                }

                // We only generate a stopPattern for the first trip in the JourneyPattern.
                // We can do this because we assume the stopPatterrns are the same for all trips in a
                // JourneyPattern
                if (stopPattern == null) {
                    stopPattern = new StopPattern(transitBuilder.getStopTimesSortedByTrip().get(trip));
                }
            }
        }
        
        if (stopPattern == null || stopPattern.size == 0) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId()
                    + " does not contain a valid stop pattern.");
            return;
        }

        TripPattern tripPattern = new TripPattern(otpRoute, stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name =
                journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();

        Deduplicator deduplicator = new Deduplicator();

        for (Trip trip : trips) {
            if (transitBuilder.getStopTimesSortedByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip,
                        transitBuilder.getStopTimesSortedByTrip().get(trip), deduplicator);
                tripPattern.add(tripTimes);
                transitBuilder.getTrips().add(trip);
            }
        }

        transitBuilder.getTripPatterns().put(tripPattern.stopPattern, tripPattern);
    }

    private List<StopTime> mapToStopTimes(JourneyPattern journeyPattern, OtpTransitServiceBuilder transitBuilder, NetexDao netexDao, Trip trip, List<TimetabledPassingTime> timetabledPassingTimes) {
        List<StopTime> stopTimes = new ArrayList<>();

        int stopSequence = 0;

        for (TimetabledPassingTime passingTime : timetabledPassingTimes) {
            JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRef
                    = passingTime.getPointInJourneyPatternRef();
            String ref = pointInJourneyPatternRef.getValue().getRef();

            Stop quay = findQuay(ref, journeyPattern, netexDao, transitBuilder);

            if (quay != null) {
                StopPointInJourneyPattern stopPoint = findStopPoint(ref, journeyPattern);
                StopTime stopTime = mapToStopTime(trip, stopPoint, quay, passingTime, stopSequence, netexDao);
                stopTimes.add(stopTime);
                ++stopSequence;
            } else {
                LOG.warn("Quay not found for timetabledPassingTimes: " + passingTime.getId());
                return null;
            }
        }
        return stopTimes;
    }

    private StopTime mapToStopTime(Trip trip, StopPointInJourneyPattern stopPoint, Stop quay,
                                   TimetabledPassingTime passingTime, int stopSequence, NetexDao netexDao) {
        StopTime stopTime = new StopTime();
        stopTime.setTrip(trip);
        stopTime.setStopSequence(stopSequence);
        stopTime.setStop(quay);

        stopTime.setArrivalTime(
                calculateOtpTime(passingTime.getArrivalTime(), passingTime.getArrivalDayOffset(),
                        passingTime.getDepartureTime(), passingTime.getDepartureDayOffset()));

        stopTime.setDepartureTime(calculateOtpTime(passingTime.getDepartureTime(),
                passingTime.getDepartureDayOffset(), passingTime.getArrivalTime(),
                passingTime.getArrivalDayOffset()));

        if (stopPoint != null) {
            stopTime.setDropOffType(isFalse(stopPoint.isForAlighting()) ? 1 : 0);
            stopTime.setPickupType(isFalse(stopPoint.isForBoarding()) ? 1 : 0);
        }

        if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
            LOG.warn("Time missing for trip " + trip.getId());
        }

        if (stopPoint.getDestinationDisplayRef() != null) {
            DestinationDisplay value = netexDao.lookUpDestinationDisplayById(stopPoint.getDestinationDisplayRef().getRef());
            if (value != null) {
                currentHeadsign = value.getFrontText().getValue();
            }
        }

        if (currentHeadsign != null) {
            stopTime.setStopHeadsign(currentHeadsign);
        }

        return stopTime;
    }

    private Stop findQuay(String pointInJourneyPatterRef, JourneyPattern journeyPattern,
            NetexDao netexDao, OtpTransitServiceBuilder transitBuilder) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if (stop.getId().equals(pointInJourneyPatterRef)) {
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point)
                            .getScheduledStopPointRef();
                    String stopId = netexDao.lookupQuayIdByStopPointRef(
                            scheduledStopPointRef.getValue().getRef()
                    );
                    if (stopId == null) {
                        LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef
                                .getValue().getRef());
                    } else {
                        Stop quay = transitBuilder.getStops()
                                .get(FeedScopedIdFactory.createFeedScopedId(stopId));
                        if (quay == null) {
                            LOG.warn("Quay not found for " + scheduledStopPointRef.getValue()
                                    .getRef());
                        }
                        return quay;
                    }
                }
            }
        }

        return null;
    }

    private StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef,
            JourneyPattern journeyPattern) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) point;
                if (stopPoint.getId().equals(pointInJourneyPatterRef)) {
                    return stopPoint;
                }
            }
        }
        return null;
    }

    private static int calculateOtpTime(LocalTime time, BigInteger dayOffset,
            LocalTime fallbackTime, BigInteger fallbackDayOffset) {
        return time != null ?
                calculateOtpTime(time, dayOffset) :
                calculateOtpTime(fallbackTime, fallbackDayOffset);
    }

    static int calculateOtpTime(LocalTime time, BigInteger dayOffset) {
        int otpTime = time.toSecondOfDay();
        if (dayOffset != null) {
            otpTime += DAY_IN_SECONDS * dayOffset.intValue();
        }
        return otpTime;
    }

    private boolean isFalse(Boolean value) {
        return value != null && !value;
    }
}
