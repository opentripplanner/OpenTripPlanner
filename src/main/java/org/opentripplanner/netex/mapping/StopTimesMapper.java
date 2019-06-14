package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;
import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;


// TODO Javadoc
// TODO Test
class StopTimesMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private static final int DAY_IN_SECONDS = 3600 * 24;

    private String currentHeadSign;

    private final HierarchicalMapById<DestinationDisplay> destinationDisplayById;

    private final EntityById<FeedScopedId, Stop> stopsById;

    private final HierarchicalMap<String, String> quayIdByStopPointRef;

    StopTimesMapper(
            HierarchicalMapById<DestinationDisplay> destinationDisplayById,
            EntityById<FeedScopedId, Stop> stopsById,
            HierarchicalMap<String, String> quayIdByStopPointRef
    ) {
        this.destinationDisplayById = destinationDisplayById;
        this.stopsById = stopsById;
        this.quayIdByStopPointRef = quayIdByStopPointRef;
    }

    List<StopTime> mapToStopTimes(
            JourneyPattern journeyPattern,
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
                    currentHeadSign = destinationDisplay.getFrontText().getValue();
                }
            }
        }

        if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
            LOG.warn("Time missing for trip " + trip.getId());
        }

        if (currentHeadSign != null) {
            stopTime.setStopHeadsign(currentHeadSign);
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

    private static boolean isFalse(Boolean value) {
        return value != null && !value;
    }
}
