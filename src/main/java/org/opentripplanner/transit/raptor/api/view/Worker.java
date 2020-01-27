package org.opentripplanner.transit.raptor.api.view;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Collection;

/**
 * The worker perform the travel search. There are multiple implementation,
 * even some who do not return paths.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface Worker<T extends RaptorTripSchedule> {

    /**
     * Perform the reouting request.
     * @return All paths found. Am empty set is returned if no patha are forund or
     * the algorithm do not collect paths.
     */
    Collection<Path<T>> route();
}
