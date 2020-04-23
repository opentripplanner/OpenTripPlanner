package org.opentripplanner.model.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.calendar.impl.CalendarServiceDataFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opentripplanner.model.impl.GenerateMissingIds.generateNoneExistentIds;

/**
 * This class is responsible for building a {@link OtpTransitService}. The instance returned by the
 * {@link #build()} method is read only, and this class provide a mutable collections to construct
 * a {@link OtpTransitService} instance.
 */
public class OtpTransitServiceBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(OtpTransitServiceBuilder.class);

    private final EntityById<FeedScopedId, Agency> agenciesById = new EntityById<>();

    private final List<ServiceCalendarDate> calendarDates = new ArrayList<>();

    private final List<ServiceCalendar> calendars = new ArrayList<>();

    private final List<FareAttribute> fareAttributes = new ArrayList<>();

    private final List<FareRule> fareRules = new ArrayList<>();

    private final List<FeedInfo> feedInfos = new ArrayList<>();

    private final List<Frequency> frequencies = new ArrayList<>();

    private final EntityById<FeedScopedId, GroupOfStations> groupsOfStationsById = new EntityById<>();

    private final EntityById<FeedScopedId, MultiModalStation> multiModalStationsById = new EntityById<>();

    private final Multimap<TransitEntity<?>, Notice> noticeAssignments = ArrayListMultimap.create();

    private final EntityById<FeedScopedId, Operator> operatorsById = new EntityById<>();

    private final List<Pathway> pathways = new ArrayList<>();

    private final EntityById<FeedScopedId, Route> routesById = new EntityById<>();

    private final Multimap<FeedScopedId, ShapePoint> shapePoints = ArrayListMultimap.create();

    private final EntityById<FeedScopedId, Station> stationsById = new EntityById<>();

    private final EntityById<FeedScopedId, Stop> stopsById = new EntityById<>();

    private final EntityById<FeedScopedId, Entrance> entrancesById = new EntityById<>();

    private final EntityById<FeedScopedId, PathwayNode> pathwayNodesById = new EntityById<>();

    private final EntityById<FeedScopedId, BoardingArea> boardingAreasById = new EntityById<>();

    private final TripStopTimes stopTimesByTrip = new TripStopTimes();

    private final List<Transfer> transfers = new ArrayList<>();

    private final EntityById<FeedScopedId, Trip> tripsById = new EntityById<>();

    private final Multimap<StopPattern, TripPattern> tripPatterns = ArrayListMultimap.create();


    public OtpTransitServiceBuilder() {
    }


    /* Accessors */

    public EntityById<FeedScopedId, Agency> getAgenciesById() {
        return agenciesById;
    }

    public List<ServiceCalendarDate> getCalendarDates() {
        return calendarDates;
    }

    public List<ServiceCalendar> getCalendars() {
        return calendars;
    }

    public List<FareAttribute> getFareAttributes() {
        return fareAttributes;
    }

    public List<FareRule> getFareRules() {
        return fareRules;
    }

    public List<FeedInfo> getFeedInfos() {
        return feedInfos;
    }

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public EntityById<FeedScopedId, GroupOfStations> getGroupsOfStationsById() {
        return groupsOfStationsById;
    }

    public EntityById<FeedScopedId, MultiModalStation> getMultiModalStationsById() {
        return multiModalStationsById;
    }

    /**
     * get multimap of Notices by the TransitEntity id (Multiple types; hence the Serializable). Entities
     * that might have Notices are Routes, Trips, Stops and StopTimes.
     */
    public Multimap<TransitEntity<?>, Notice> getNoticeAssignments() {
        return noticeAssignments;
    }

    public EntityById<FeedScopedId, Operator> getOperatorsById() {
        return operatorsById;
    }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public EntityById<FeedScopedId, Route> getRoutes() {
        return routesById;
    }

    public Multimap<FeedScopedId, ShapePoint> getShapePoints() {
        return shapePoints;
    }

    public EntityById<FeedScopedId, Station> getStations() {
        return stationsById;
    }

    public EntityById<FeedScopedId, Stop> getStops() {
        return stopsById;
    }

    public EntityById<FeedScopedId, Entrance> getEntrances() {
        return entrancesById;
    }

    public EntityById<FeedScopedId, PathwayNode> getPathwayNodes() {
        return pathwayNodesById;
    }

    public EntityById<FeedScopedId, BoardingArea> getBoardingAreas() {
        return boardingAreasById;
    }

    public TripStopTimes getStopTimesSortedByTrip() {
        return stopTimesByTrip;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public EntityById<FeedScopedId, Trip> getTripsById() {
        return tripsById;
    }

    public Multimap<StopPattern, TripPattern> getTripPatterns() {
        return tripPatterns;
    }


    /**
     * Find all serviceIds in both CalendarServices and CalendarServiceDates.
     */
    Set<FeedScopedId> findAllServiceIds() {
        Set<FeedScopedId> serviceIds = new HashSet<>();
        for (ServiceCalendar calendar : getCalendars()) {
            serviceIds.add(calendar.getServiceId());
        }
        for (ServiceCalendarDate date : getCalendarDates()) {
            serviceIds.add(date.getServiceId());
        }
        return serviceIds;
    }

    public CalendarServiceData buildCalendarServiceData() {
        return CalendarServiceDataFactoryImpl.createCalendarServiceData(
                getAgenciesById().values(),
                getCalendarDates(),
                getCalendars()
        );
    }

    public OtpTransitService build() {
        generateNoneExistentIds(feedInfos);
        return new OtpTransitServiceImpl(this);
    }

    /**
     * Limit the transit service to a time period removing calendar dates and services
     * outside the period. If a service is start before and/or ends after the period
     * then the service is modified to match the period.
     */
    public void limitServiceDays(ServiceDateInterval periodLimit) {
        if(periodLimit.isUnbounded()) {
            LOG.info("Limiting transit service is skipped, the period is unbounded.");
            return;
        }

        LOG.warn("Limiting transit service days to time period: {}", periodLimit);

        int orgSize = calendarDates.size();
        calendarDates.removeIf(c -> !periodLimit.include(c.getDate()));
        logRemove("ServiceCalendarDate", orgSize, calendarDates.size(), "Outside time period.");

        List<ServiceCalendar> keepCal = new ArrayList<>();
        for (ServiceCalendar calendar : calendars) {
            if(calendar.getPeriod().overlap(periodLimit)) {
                calendar.setPeriod(calendar.getPeriod().intersection(periodLimit));
                keepCal.add(calendar);
            }
        }

        orgSize = calendars.size();
        if(orgSize != keepCal.size()) {
            calendars.clear();
            calendars.addAll(keepCal);
            logRemove("ServiceCalendar", orgSize, calendars.size(), "Outside time period.");
        }
        removeEntitiesWithInvalidReferences();
        LOG.info("Limiting transit service days to time period complete.");
    }

    /**
     * Check all relations and remove entities witch reference none existing entries. This
     * may happen as a result of inconsistent data or by deliberate removal of elements in the
     * builder.
     */
    private void removeEntitiesWithInvalidReferences() {
        removeTripsWithNoneExistingServiceIds();
        removeStopTimesForNoneExistingTrips();
        fixOrRemovePatternsWhichReferenceNoneExistingTrips();
        removeTransfersForNoneExistingTrips();
    }

    /** Remove all trips witch reference none existing service ids */
    private void removeTripsWithNoneExistingServiceIds() {
        Set<FeedScopedId> serviceIds = findAllServiceIds();
        int orgSize = tripsById.size();
        tripsById.removeIf(t -> !serviceIds.contains(t.getServiceId()));
        logRemove("Trip", orgSize, tripsById.size(), "Trip service id does not exist.");
    }

    /** Remove all stopTimes witch reference none existing trips */
    private void removeStopTimesForNoneExistingTrips() {
        int orgSize = stopTimesByTrip.size();
        stopTimesByTrip.removeIf(t -> !tripsById.containsKey(t.getId()));
        logRemove("StopTime", orgSize, stopTimesByTrip.size(), "StopTime trip does not exist.");
    }

    /** Remove none existing trips from patterns and then remove empty patterns */
    private void fixOrRemovePatternsWhichReferenceNoneExistingTrips() {
        int orgSize = tripPatterns.size();
        List<Map.Entry<StopPattern, TripPattern>> removePatterns = new ArrayList<>();

        for (Map.Entry<StopPattern, TripPattern> e : tripPatterns.entries()) {
            TripPattern ptn = e.getValue();
            ptn.removeTrips(t -> !tripsById.containsKey(t.getId()));
            if(ptn.getTrips().isEmpty()) {
                removePatterns.add(e);
            }
        }
        for (Map.Entry<StopPattern, TripPattern> it : removePatterns) {
            tripPatterns.remove(it.getKey(), it.getValue());
        }
        logRemove("TripPattern", orgSize, tripPatterns.size(), "No trips for pattern exist.");
    }

    /** Remove all transfers witch reference none existing trips */
    private void removeTransfersForNoneExistingTrips() {
        int orgSize = transfers.size();
        transfers.removeIf(it -> noTripExist(it.getFromTrip()) || noTripExist(it.getToTrip()));
        logRemove("Trip", orgSize, transfers.size(), "Transfer to/from trip does not exist.");
    }

    /** Return true if the trip is a valid reference; {@code null} or exist. */
    private boolean noTripExist(Trip t) {
        return t != null && !tripsById.containsKey(t.getId());
    }

    private static void logRemove(String type, int orgSize, int newSize, String reason) {
        if(orgSize == newSize) { return; }
        LOG.info("{} of {} {}(s) removed. Reason: {}", orgSize - newSize, orgSize, type, reason);
    }
}
