package org.onebusaway2.gtfs.services;

import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.FareAttribute;
import org.onebusaway2.gtfs.model.FareRule;
import org.onebusaway2.gtfs.model.FeedInfo;
import org.onebusaway2.gtfs.model.Frequency;
import org.onebusaway2.gtfs.model.Pathway;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.ShapePoint;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.Transfer;
import org.onebusaway2.gtfs.model.Trip;

import java.util.Collection;
import java.util.List;

/**
 * Methods for accessing imported entities.
 */
public interface OtpTransitDao {

    Collection<Agency> getAllAgencies();

    Collection<ServiceCalendar> getAllCalendars();

    ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId);

    Collection<ServiceCalendarDate> getAllCalendarDates();

    List<ServiceCalendarDate> getCalendarDatesForServiceId(AgencyAndId serviceId);

    /** @return all ids for both Calendars and CalendarDates merged into on list without duplicates */
    List<AgencyAndId> getAllServiceIds();

    Collection<FareAttribute> getAllFareAttributes();

    Collection<FareRule> getAllFareRules();

    Collection<FeedInfo> getAllFeedInfos();

    Collection<Frequency> getAllFrequencies();

    Collection<Pathway> getAllPathways();

    Collection<Route> getAllRoutes();

    Collection<ShapePoint> getAllShapePoints();

    List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId);

    Collection<Stop> getAllStops();

    Stop getStopForId(AgencyAndId id);

    Collection<StopTime> getAllStopTimes();

    /**
     * @return the list of {@link StopTime} objects associated with the trip,
     * sorted by {@link StopTime#getStopSequence()}
     */
    List<StopTime> getStopTimesForTrip(Trip trip);

    Collection<Transfer> getAllTransfers();

    Collection<Trip> getAllTrips();

    List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId);

    List<Stop> getStopsForStation(Stop station);
}
