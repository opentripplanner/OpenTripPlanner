package org.opentripplanner.routing.fares;

import java.io.Serializable;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Computes a fare for a given sequence of Rides. The FareService is serialized as part of the
 * Graph; Hence it should be {@link Serializable}.
 */
public interface FareService extends Serializable {
  /**
   * @param itinerary the OTP2 Itinerary for which we want to compute a fare
   */
  ItineraryFares calculateFares(Itinerary itinerary);
}
