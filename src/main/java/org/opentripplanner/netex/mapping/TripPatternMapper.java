package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;
import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx JourneyPattern to OTP TripPattern. All ServiceJourneys in the same JourneyPattern contain the same
 * sequence of stops. This means that they can all use the same StopPattern. Each ServiceJourney contains
 * TimeTabledPassingTimes that are mapped to StopTimes.
 * <p>
 * Headsigns in NeTEx are only specified once and then valid for each subsequent TimeTabledPassingTime until a new
 * headsign is specified. This is accounted for in the mapper.
 */
class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private static final int DAY_IN_SECONDS = 3600 * 24;

    private String currentHeadsign;

    private TripMapper tripMapper = new TripMapper();

    void mapTripPattern(
            JourneyPattern journeyPattern,
            OtpTransitServiceBuilder transitBuilder,
            NetexImportDataIndex netexIndex
    ) {

        Collection<ServiceJourney> serviceJourneys = netexIndex.serviceJourneyByPatternId
                .lookup(journeyPattern.getId());

        if (serviceJourneys == null || serviceJourneys.isEmpty()) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId()
                    + " does not contain any serviceJourneys.");
            return;
        }

        List<Trip> trips = new ArrayList<>();
        for (ServiceJourney serviceJourney : serviceJourneys) {
            Trip trip = tripMapper.mapServiceJourney(
                    serviceJourney,
                    transitBuilder.getRoutes(),
                    netexIndex.routeById,
                    netexIndex.journeyPatternsById
            );

            List<StopTime> stopTimes = mapToStopTimes(
                    journeyPattern,
                    transitBuilder.getStops(),
                    netexIndex.quayIdByStopPointRef,
                    netexIndex.destinationDisplayById,
                    trip,
                    serviceJourney.getPassingTimes().getTimetabledPassingTime()
            );

            transitBuilder.getStopTimesSortedByTrip().put(trip, stopTimes);
            trip.setTripHeadsign(getHeadsign(stopTimes));
            trips.add(trip);
        }

        // Create StopPattern from any trip (since they are part of the same JourneyPattern)
        StopPattern stopPattern = new StopPattern(
                transitBuilder.getStopTimesSortedByTrip().get(trips.get(0))
        );

        org.opentripplanner.model.Route otpRoute = lookupRoute(
                journeyPattern,
                transitBuilder.getRoutes(),
                netexIndex.routeById);
        TripPattern tripPattern = new TripPattern(otpRoute, stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name = journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();
        transitBuilder.getTripPatterns().put(tripPattern.stopPattern, tripPattern);
        createTripTimes(transitBuilder, trips, tripPattern);
    }

    private org.opentripplanner.model.Route lookupRoute(
            JourneyPattern journeyPattern,
            EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById,
            HierarchicalMapById<Route> routeById
    ) {
        Route route = routeById.lookup(journeyPattern.getRouteRef().getRef());
        return otpRouteById.get(createFeedScopedId(route.getLineRef().getValue().getRef()));
    }

    private void createTripTimes(
            OtpTransitServiceBuilder transitBuilder,
            List<Trip> trips,
            TripPattern tripPattern
    ) {
        Deduplicator deduplicator = new Deduplicator();
        for (Trip trip : trips) {
            if (transitBuilder.getStopTimesSortedByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip,
                        transitBuilder.getStopTimesSortedByTrip().get(trip), deduplicator);
                tripPattern.add(tripTimes);
                transitBuilder.getTripsById().add(trip);
            }
        }
    }

    private String getHeadsign(List<StopTime> stopTimes) {
        if (stopTimes != null && stopTimes.size() > 0) {
            return stopTimes.stream().findFirst().get().getStopHeadsign();
        } else {
            return "";
        }
    }

    private List<StopTime> mapToStopTimes(
            JourneyPattern journeyPattern,
            EntityById<FeedScopedId, Stop> stopsById,
            HierarchicalMap<String, String> quayIdByStopPointRef,
            HierarchicalMapById<DestinationDisplay> destinationDisplayById,
            Trip trip,
            List<TimetabledPassingTime> timetabledPassingTimes
    ) {
        List<StopTime> stopTimes = new ArrayList<>();

        for (int i = 0; i < timetabledPassingTimes.size(); i++) {

            String pointInJourneyPattern =
                    timetabledPassingTimes.get(i).getPointInJourneyPatternRef().getValue().getRef();

            StopPointInJourneyPattern stopPoint = findStopPoint(pointInJourneyPattern, journeyPattern);
            Stop stop = lookupStop(stopPoint, quayIdByStopPointRef, stopsById);

            StopTime stopTime = mapToStopTime(
                    trip,
                    stopPoint,
                    stop,
                    timetabledPassingTimes.get(i),
                    i,
                    destinationDisplayById);

            stopTimes.add(stopTime);
        }
        return stopTimes;
    }

    private StopTime mapToStopTime(
            Trip trip,
            StopPointInJourneyPattern stopPoint,
            Stop stop,
            TimetabledPassingTime passingTime,
            int stopSequence,
            HierarchicalMapById<DestinationDisplay> destinationDisplayById
    ) {
        StopTime stopTime = new StopTime();
        stopTime.setTrip(trip);
        stopTime.setStopSequence(stopSequence);
        stopTime.setStop(stop);
        stopTime.setArrivalTime(
                calculateOtpTime(passingTime.getArrivalTime(), passingTime.getArrivalDayOffset(),
                        passingTime.getDepartureTime(), passingTime.getDepartureDayOffset()));
        stopTime.setDepartureTime(calculateOtpTime(passingTime.getDepartureTime(),
                passingTime.getDepartureDayOffset(), passingTime.getArrivalTime(),
                passingTime.getArrivalDayOffset()));

        if (stopPoint != null) {
            if (isFalse(stopPoint.isForAlighting())) {
                stopTime.setDropOffType(PICKDROP_NONE);
            } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
                stopTime.setDropOffType(PICKDROP_COORDINATE_WITH_DRIVER);
            } else {
                stopTime.setDropOffType(PICKDROP_SCHEDULED);
            }

            if (isFalse(stopPoint.isForBoarding())) {
                stopTime.setPickupType(PICKDROP_NONE);
            } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
                stopTime.setPickupType(PICKDROP_COORDINATE_WITH_DRIVER);
            } else {
                stopTime.setPickupType(PICKDROP_SCHEDULED);
            }

            if (stopPoint.getDestinationDisplayRef() != null) {
                DestinationDisplay destinationDisplay =
                        destinationDisplayById.lookup(stopPoint.getDestinationDisplayRef().getRef());
                if (destinationDisplay != null) {
                    currentHeadsign = destinationDisplay.getFrontText().getValue();
                }
            }
        }

        if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
            LOG.warn("Time missing for trip " + trip.getId());
        }

        if (currentHeadsign != null) {
            stopTime.setStopHeadsign(currentHeadsign);
        }

        return stopTime;
    }

    private Stop lookupStop(
            StopPointInJourneyPattern stopPointInJourneyPattern,
            HierarchicalMap<String, String> quayIdByStopPointRef,
            EntityById<FeedScopedId, Stop> stopsById
    ) {
        if (stopPointInJourneyPattern != null) {
            JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef
                    = stopPointInJourneyPattern.getScheduledStopPointRef();
            String stopId = quayIdByStopPointRef.lookup(scheduledStopPointRef.getValue().getRef());
            if (stopId == null) {
                LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef
                        .getValue().getRef());
            } else {
                Stop stop = stopsById.get(FeedScopedIdFactory.createFeedScopedId(stopId));
                if (stop == null) {
                    LOG.warn("Quay not found for " + scheduledStopPointRef.getValue()
                            .getRef());
                }
                return stop;
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
