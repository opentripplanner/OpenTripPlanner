package org.opentripplanner.routing.services;

import com.conveyal.r5.otp2.api.path.Path;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.Fare;

import java.io.Serializable;

/**
 * Computes a fare for a given sequence of Rides. The FareService is serialized
 * as part of the Graph; Hence it should be {@link Serializable}.
 */
public interface FareService extends Serializable {
	/**
	 * @param path the OTP2 Raptor path for which we want to compute a fare
	 * @param transitLayer the TransitLayer that this Path references for stop numbers etc.
	 */
	Fare getCost(Path<TripSchedule> path, TransitLayer transitLayer);
}
