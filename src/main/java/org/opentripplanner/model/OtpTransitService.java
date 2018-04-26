package org.opentripplanner.model;

import java.util.Collection;
import java.util.List;

/**
 * Methods for accessing imported entities.
 */
public interface OtpTransitService {

    Collection<Agency> getAllAgencies();

    Collection<ServiceCalendar> getAllCalendars();

    ServiceCalendar getCalendarForServiceId(FeedId serviceId);

    Collection<ServiceCalendarDate> getAllCalendarDates();

    List<ServiceCalendarDate> getCalendarDatesForServiceId(FeedId serviceId);

    /** @return all ids for both Calendars and CalendarDates merged into on list without duplicates */
    List<FeedId> getAllServiceIds();

    Collection<FareAttribute> getAllFareAttributes();

    Collection<FareRule> getAllFareRules();

    Collection<FeedInfo> getAllFeedInfos();

    Collection<Frequency> getAllFrequencies();

    Collection<Pathway> getAllPathways();

    Collection<Route> getAllRoutes();

    Collection<ShapePoint> getAllShapePoints();

    List<ShapePoint> getShapePointsForShapeId(FeedId shapeId);

    Collection<Stop> getAllStops();

    Stop getStopForId(FeedId id);

    Collection<StopTime> getAllStopTimes();

    /**
     * @return the list of {@link StopTime} objects associated with the trip,
     * sorted by {@link StopTime#getStopSequence()}
     */
    List<StopTime> getStopTimesForTrip(Trip trip);

    Collection<Transfer> getAllTransfers();

    Collection<Trip> getAllTrips();

    List<String> getTripAgencyIdsReferencingServiceId(FeedId serviceId);

    List<Stop> getStopsForStation(Stop station);
}
