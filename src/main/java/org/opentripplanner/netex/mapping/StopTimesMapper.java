package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.util.OTPFeature;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;

/**
 * This maps a list of TimetabledPassingTimes to a list of StopTimes. It also makes sure the StopTime has a reference
 * to the correct stop. DestinationDisplay is mapped to HeadSign. There is logic to take care of the the fact that
 * DestinationsDisplay is also valid for each subsequent TimeTabledPassingTime, while HeadSign has to be explicitly
 * defined for each StopTime.
 */
class StopTimesMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private static final int DAY_IN_SECONDS = 3600 * 24;

    private final FeedScopedIdFactory idFactory;

    private final ReadOnlyHierarchicalMap<String, DestinationDisplay> destinationDisplayById;

    private final EntityById<Stop> stopsById;

    private final EntityById<FlexStopLocation> flexibleStopLocationsById;

    private final ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef;

    private final ReadOnlyHierarchicalMap<String, String> flexibleStopPlaceIdByStopPointRef;

    private final ReadOnlyHierarchicalMap<String, Route> routeByid;

    private final ReadOnlyHierarchicalMapById<FlexibleLine> flexibleLinesById;

    private String currentHeadSign;

    StopTimesMapper(
            FeedScopedIdFactory idFactory,
            EntityById<Stop> stopsById,
            EntityById<FlexStopLocation> flexStopLocationsById,
            ReadOnlyHierarchicalMap<String, DestinationDisplay> destinationDisplayById,
            ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef,
            ReadOnlyHierarchicalMap<String, String> flexibleStopPlaceIdByStopPointRef,
            ReadOnlyHierarchicalMapById<FlexibleLine> flexibleLinesById,
            ReadOnlyHierarchicalMap<String, Route> routeById
    ) {
        this.idFactory = idFactory;
        this.destinationDisplayById = destinationDisplayById;
        this.stopsById = stopsById;
        this.flexibleStopLocationsById = flexStopLocationsById;
        this.quayIdByStopPointRef = quayIdByStopPointRef;
        this.flexibleStopPlaceIdByStopPointRef = flexibleStopPlaceIdByStopPointRef;
        this.flexibleLinesById = flexibleLinesById;
        this.routeByid = routeById;
    }

    /**
     * @return a map of stop-times indexed by the TimetabledPassingTime id.
     */
    @Nullable
    MappedStopTimes mapToStopTimes(
            JourneyPattern journeyPattern,
            Trip trip,
            List<TimetabledPassingTime> passingTimes,
            ServiceJourney serviceJourney
    ) {
        MappedStopTimes result = new MappedStopTimes();

        for (int i = 0; i < passingTimes.size(); i++) {

            TimetabledPassingTime currentPassingTime = passingTimes.get(i);

            String pointInJourneyPattern = currentPassingTime.getPointInJourneyPatternRef().getValue().getRef();

            StopPointInJourneyPattern stopPoint = findStopPoint(pointInJourneyPattern, journeyPattern);

            StopLocation stop = lookUpStopLocation(stopPoint);
            if (stop == null) {
                LOG.warn("Stop with id {} not found for StopPoint {} in JourneyPattern {}. "
                        + "Trip {} will not be mapped.",
                    stopPoint != null && stopPoint.getScheduledStopPointRef() != null
                        ? stopPoint.getScheduledStopPointRef().getValue().getRef()
                        : "null"
                    , stopPoint != null ? stopPoint.getId() : "null"
                    , journeyPattern.getId()
                    , trip.getId());
                return null;
            }

            StopTime stopTime = mapToStopTime(trip, stopPoint, stop, currentPassingTime, i);
            if (stopTime == null) {
                return null;
            }

            stopTime.setBookingInfo(BookingInfoMapper.map(
                stopPoint,
                serviceJourney,
                lookUpFlexibleLine(serviceJourney, journeyPattern)
            ));

            result.add(currentPassingTime.getId(), stopTime);
        }

        if (OTPFeature.FlexRouting.isOn()) {
            // TODO This is a temporary mapping of the UnscheduledTrip format, until we decide on how
            //      this should be harmonized between GTFS and NeTEx
            modifyDataForUnscheduledFlexTrip(result);
        }

        return result;
    }

    static class MappedStopTimes {
        Map<String, StopTime> stopTimeByNetexId = new HashMap<>();
        List<StopTime> stopTimes = new ArrayList<>();

        void add(String netexId, StopTime stopTime) {
            stopTimeByNetexId.put(netexId, stopTime);
            stopTimes.add(stopTime);
        }
    }

    private StopTime mapToStopTime(
            Trip trip,
            StopPointInJourneyPattern stopPoint,
            StopLocation stop,
            TimetabledPassingTime passingTime,
            int stopSequence
    ) {
        StopTime stopTime = new StopTime();
        stopTime.setTrip(trip);
        stopTime.setStopSequence(stopSequence);
        stopTime.setStop(stop);
        // TODO PassingTimes containing EarliestArrivalTime or LatestDepartureTime only not yet
        //      supported
        if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
            return null;
        }
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

    @Nullable
    private StopLocation lookUpStopLocation(StopPointInJourneyPattern stopPointInJourneyPattern) {
        if (stopPointInJourneyPattern == null) { return null; }

        String stopPointRef = stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef();

        String stopId = quayIdByStopPointRef.lookup(stopPointRef);
        String flexibleStopPlaceId = flexibleStopPlaceIdByStopPointRef.lookup(stopPointRef);

        if (stopId == null && flexibleStopPlaceId == null) {
            LOG.warn("No passengerStopAssignment found for " + stopPointRef);
            return null;
        }

        StopLocation stopLocation;
        if (stopId != null) {
            stopLocation = stopsById.get(idFactory.createId(stopId));
        } else {
            stopLocation = flexibleStopLocationsById.get(idFactory.createId(flexibleStopPlaceId));
        }

        if (stopLocation == null) {
            LOG.warn("No Quay or FlexibleStopPlace found for " + stopPointRef);
        }

        return stopLocation;
    }

    @Nullable
    private static StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef,
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

    // TODO This is a temporary mapping of the UnscheduledTrip format, until we decide on how
    //      this should be harmonized between GTFS and NeTEx
    private static void modifyDataForUnscheduledFlexTrip(MappedStopTimes result) {
        List<StopTime> stopTimes = result.stopTimes;
        if (stopTimes.size() == 2 && stopTimes
            .stream()
            .allMatch(s -> s.getStop() instanceof FlexStopLocation)) {

            int departureTime = stopTimes.get(0).getDepartureTime();
            int arrivalTime = stopTimes.get(1).getArrivalTime();

            for (StopTime stopTime : stopTimes) {
                stopTime.clearArrivalTime();
                stopTime.clearDepartureTime();
                stopTime.setFlexWindowStart(departureTime);
                stopTime.setFlexWindowEnd(arrivalTime);
            }
        }
    }

    private FlexibleLine lookUpFlexibleLine(ServiceJourney serviceJourney, JourneyPattern journeyPattern) {
        if (serviceJourney == null) { return null; }
        String lineRef = null;
        // Check for direct connection to Line
        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        if (lineRefStruct != null){
            // Connect to Line referenced directly from ServiceJourney
            lineRef = lineRefStruct.getValue().getRef();
        } else if(serviceJourney.getJourneyPatternRef() != null){
            // Connect to Line referenced through JourneyPattern->Route
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = routeByid.lookup(routeRef).getLineRef().getValue().getRef();
        }
        return flexibleLinesById.lookup(lineRef);
    }
}
