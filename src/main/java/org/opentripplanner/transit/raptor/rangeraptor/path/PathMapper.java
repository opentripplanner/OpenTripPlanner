package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Responsible for mapping between the domain of routing to the domain of result paths.
 * Especially a regular forward search and a reverse search have different internal
 * data representations (latest possible arival times vs. arrival times); Hence one mapper
 * for each.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@FunctionalInterface
public interface PathMapper<T extends RaptorTripSchedule> {
    /**
     * Build a path from a destination arrival - this maps between the domain of routing
     * to the domain of result paths. All values not needed for routing is computed as part of this mapping.
     */
     Path<T> mapToPath(final DestinationArrival<T> destinationArrival);
}
