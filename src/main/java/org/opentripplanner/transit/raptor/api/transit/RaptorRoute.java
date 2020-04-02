package org.opentripplanner.transit.raptor.api.transit;

/**
 * The {@link RaptorRoute} serve as the aggregate root for the transit model
 * needed by Raptor to perform the routing. From this class you should be able
 * to navigate to the time-table, the trip-pattern and access all trips within
 * the pattern in service defined by the time-table.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorRoute<T extends RaptorTripSchedule> {

    /**
     * Return a time-table. a list of all trips in service.
     */
    RaptorTimeTable<T> timetable();

    /**
     * Return the trip-pattern, the list of stops visited by this route.
     */
    RaptorTripPattern pattern();
}
