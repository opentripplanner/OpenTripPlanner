package org.opentripplanner.netex.mapping;

import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    public void mapTripPattern(JourneyPattern journeyPattern, OtpTransitServiceBuilder transitBuilder, NetexDao netexDao){
        TripMapper tripMapper = new TripMapper();

        List<Trip> trips = new ArrayList<>();

        //find matching journey pattern
        List<ServiceJourney> serviceJourneys = netexDao.getServiceJourneyById().get(journeyPattern.getId());

        StopPattern stopPattern = null;

        Route route = netexDao.getRouteById().get(journeyPattern.getRouteRef().getRef());
        org.opentripplanner.model.Route otpRoute = transitBuilder.getRoutes().get(
                FeedScopedIdFactory.createFeedScopedId(route.getLineRef().getValue().getRef()));

        if (serviceJourneys == null) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId() + " does not contain any serviceJourneys.");
            return;
        }

        for(ServiceJourney serviceJourney : serviceJourneys){
            Trip trip = tripMapper.mapServiceJourney(serviceJourney, transitBuilder, netexDao);
            trips.add(trip);

            TimetabledPassingTimes_RelStructure passingTimes = serviceJourney.getPassingTimes();
            List<TimetabledPassingTime> timetabledPassingTime = passingTimes.getTimetabledPassingTime();
            List<StopTime> stopTimes = new ArrayList<>();

            int stopSequence = 0;

            for(TimetabledPassingTime passingTime : timetabledPassingTime){
                JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRef = passingTime.getPointInJourneyPatternRef();
                String ref = pointInJourneyPatternRef.getValue().getRef();

                Stop quay = findQuay(ref, journeyPattern, netexDao, transitBuilder);

                if (quay == null) {
                    LOG.warn("Quay not found for timetabledPassingTime: " + passingTime.getId());
                    break;
                }

                StopTime stopTime = new StopTime();
                stopTime.setTrip(trip);
                stopTime.setStopSequence(stopSequence++);

                if(passingTime.getArrivalTime() != null){
                    int arrivalTime = passingTime.getArrivalTime().toSecondOfDay();
                    if(passingTime.getArrivalDayOffset() != null && passingTime.getArrivalDayOffset().intValue() == 1){
                        // TODO - TGR Tast and fix this
                        arrivalTime = arrivalTime + (3600 * 24);
                    }
                    stopTime.setArrivalTime(arrivalTime);
                }else if(passingTime.getDepartureTime() != null) {
                    int arrivalTime = passingTime.getDepartureTime().toSecondOfDay();
                    if(passingTime.getDepartureDayOffset() != null && passingTime.getDepartureDayOffset().intValue() == 1){
                        // TODO - TGR Tast and fix this
                        arrivalTime = arrivalTime + (3600 * 24);
                    }
                    stopTime.setArrivalTime(arrivalTime);
                }

                if(passingTime.getDepartureTime() != null) {
                    int departureTime = passingTime.getDepartureTime().toSecondOfDay();
                    if(passingTime.getDepartureDayOffset() != null && passingTime.getDepartureDayOffset().intValue() == 1){
                        // TODO - TGR Tast and fix this
                        departureTime = departureTime + (3600 * 24);
                    }
                    stopTime.setDepartureTime(departureTime);
                }
                else if(passingTime.getArrivalTime() != null) {
                    int departureTime = passingTime.getArrivalTime().toSecondOfDay();
                    if(passingTime.getArrivalDayOffset() != null && passingTime.getArrivalDayOffset().intValue() == 1){
                        // TODO - TGR Tast and fix this
                        departureTime = departureTime + (3600 * 24);
                    }
                    stopTime.setDepartureTime(departureTime);
                }
                StopPointInJourneyPattern stopPoint = findStopPoint(ref, journeyPattern);
                if(stopPoint != null){
                    stopTime.setDropOffType(stopPoint.isForAlighting() != null && !stopPoint.isForAlighting() ? 1 : 0);
                    stopTime.setPickupType(stopPoint.isForBoarding() != null && !stopPoint.isForBoarding() ? 1 : 0);
                }

                if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
                    LOG.warn("Time missing for trip " + trip.getId());
                }

                stopTime.setStop(quay);
                stopTimes.add(stopTime);
            }

            transitBuilder.getStopTimesSortedByTrip().replace(trip, stopTimes);

            // We only generate a stopPattern for the first trip in the JourneyPattern.
            // We can do this because we assume the stopPatterrns are the same for all trips in a
            // JourneyPattern
            if(stopPattern == null){
                stopPattern = new StopPattern(transitBuilder.getStopTimesSortedByTrip().get(trip));
            }
        }

        if (stopPattern.size == 0) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId() + " does not contain a valid stop pattern.");
            return;
        }

        TripPattern tripPattern = new TripPattern(otpRoute, stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name = journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();

        Deduplicator deduplicator = new Deduplicator();

        for(Trip trip : trips){
            if (transitBuilder.getStopTimesSortedByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip, transitBuilder.getStopTimesSortedByTrip().get(trip), deduplicator);
                tripPattern.add(tripTimes);
                transitBuilder.getTrips().add(trip);
            }
        }

        transitBuilder.getTripPatterns().put(tripPattern.stopPattern, tripPattern);
    }


    private Stop findStopPlace(String pointInJourneyPatterRef, ServiceJourneyPattern serviceJourneyPattern, NetexDao netexDao, OtpTransitServiceBuilder transitBuilder){
        List<PointInLinkSequence_VersionedChildStructure> points =
                serviceJourneyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if(stop.getId().equals(pointInJourneyPatterRef)){
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point).getScheduledStopPointRef();

                    String stopId = netexDao.getStopPointStopPlaceMap().get(scheduledStopPointRef.getValue().getRef());

                    if (stopId == null) {
                        LOG.warn("StopPlace not found for " + scheduledStopPointRef.getValue().getRef());
                    }
                    else {
                        Stop stopPlace = transitBuilder.getStops()
                                .get(FeedScopedIdFactory.createFeedScopedId(stopId));
                        if (stopPlace == null) {
                            LOG.warn("StopPlace not found for " + scheduledStopPointRef.getValue().getRef());
                        }
                        return stopPlace;
                    }
                }
            }
        }
        return null;
    }

    private Stop findQuay(String pointInJourneyPatterRef, JourneyPattern journeyPattern, NetexDao netexDao, OtpTransitServiceBuilder transitBuilder){
        List<PointInLinkSequence_VersionedChildStructure> points =
                journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if(stop.getId().equals(pointInJourneyPatterRef)){
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point).getScheduledStopPointRef();
                    String stopId = netexDao.getStopPointQuayMap().get(scheduledStopPointRef.getValue().getRef());
                    if (stopId == null) {
                        LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef.getValue().getRef());
                    }
                    else {
                        Stop quay = transitBuilder.getStops()
                                .get(FeedScopedIdFactory.createFeedScopedId(stopId));
                        if (quay == null) {
                            LOG.warn("Quay not found for " + scheduledStopPointRef.getValue().getRef());
                        }
                        return quay;
                    }
                }
            }
        }

        return null;
    }

    private StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef, JourneyPattern journeyPattern){
        List<PointInLinkSequence_VersionedChildStructure> points =
                journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) point;
                if(stopPoint.getId().equals(pointInJourneyPatterRef)){
                    return stopPoint;
                }
            }
        }
        return null;
    }
}
