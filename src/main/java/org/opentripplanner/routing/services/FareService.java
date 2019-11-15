package org.opentripplanner.routing.services;

import com.conveyal.r5.otp2.api.path.Path;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.Fare;

/**
 * Computes a fare for a given sequence of Rides.
 *
 */
public interface FareService {
	Fare getCost(Path<TripSchedule> path);
}
