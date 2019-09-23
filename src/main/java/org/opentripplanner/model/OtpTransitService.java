package org.opentripplanner.model;

import com.google.common.collect.Multimap;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Methods for accessing imported entities.
 */
public interface OtpTransitService {

    Collection<Agency> getAllAgencies();

    Collection<FareAttribute> getAllFareAttributes();

    Collection<FareRule> getAllFareRules();

    Collection<FeedInfo> getAllFeedInfos();

    /**
     * This is equivalent to a Transmodel Notice Assignments. The map key may reference entities ids of
     * any type (Serializable).
     */
    Multimap<Serializable, Notice> getNoticeAssignments();

    Collection<Pathway> getAllPathways();

    /**
     * @return all ids for both Calendars and CalendarDates merged into on list without duplicates.
     */
    Collection<FeedScopedId> getAllServiceIds();

    List<ShapePoint> getShapePointsForShapeId(FeedScopedId shapeId);

    Stop getStopForId(FeedScopedId id);

    List<Stop> getStopsForStation(Stop station);

    Collection<Stop> getAllStops();

    /**
     * @return the list of {@link StopTime} objects associated with the trip,
     * sorted by {@link StopTime#getStopSequence()}
     */
    List<StopTime> getStopTimesForTrip(Trip trip);

    Collection<Transfer> getAllTransfers();

    Collection<TripPattern> getTripPatterns();

    Collection<Trip> getAllTrips();
}
