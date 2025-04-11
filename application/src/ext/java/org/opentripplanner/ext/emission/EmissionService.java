package org.opentripplanner.ext.emission;

import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * A service for getting emission                                                                                                                                                                                                                                                               emissions information for routes.
 */
@Sandbox
public interface EmissionService {
  /**
   * Calculate the passenger emission for car for a given distance. The calculation is based on the
   * configured number of people in the car and the average car emissions.
   *
   * @return The emissions per passanger for the whole given distance. {@link Emission#ZERO} is
   * retuned if no emission exist.
   */
  Emission calculateCarPassengerEmission(double distanceMeters);

  /**
   * Calculate the passenger emissions for a specific [route and distance] or [trip, from stop/time,
   * to stop ]. The service implementation will deside witch of the two caclulation methods
   * is used.
   *
   * @return The emissions per passanger for the whole. {@link Emission#ZERO} is retuned if no
   * emission exist.
   */
  Emission calculateTransitPassengerEmissionForTripLeg(
    Trip trip,
    int boardStopPosInPattern,
    int alightStopPosInPattern,
    double distance_m
  );
}
