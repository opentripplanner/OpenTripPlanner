package org.opentripplanner.routing.algorithm.mapping._support.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
@Deprecated
public class ApiItinerary {

  /**
   * Duration of the trip on this itinerary, in seconds.
   */
  public Long duration = 0L;

  /**
   * Time that the trip departs.
   */
  public Calendar startTime = null;
  /**
   * Time that the trip arrives.
   */
  public Calendar endTime = null;

  /**
   * How much time is spent walking, in seconds.
   */
  public long walkTime = 0;
  /**
   * How much time is spent on transit, in seconds.
   */
  public long transitTime = 0;
  /**
   * How much time is spent waiting for transit to arrive, in seconds.
   */
  public long waitingTime = 0;

  /**
   * How far the user has to walk, in meters.
   */
  public Double walkDistance = 0.0;

  /**
   * Indicates that the walk limit distance has been exceeded for this itinerary when true.
   */
  public boolean walkLimitExceeded = false;

  /**
   * If a generalized cost is used in the routing algorithm, this should be the total cost computed
   * by the algorithm. This is relevant for anyone who want to debug an search and tuning the
   * system. The unit should be equivalent to the cost of "one second of transit".
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int generalizedCost = -1;

  /**
   * How much elevation is lost, in total, over the course of the trip, in meters. As an example, a
   * trip that went from the top of Mount Everest straight down to sea level, then back up K2, then
   * back down again would have an elevationLost of Everest + K2.
   */
  public Double elevationLost = 0.0;
  /**
   * How much elevation is gained, in total, over the course of the trip, in meters. See
   * elevationLost.
   */
  public Double elevationGained = 0.0;

  /**
   * The number of transfers this trip has.
   */
  public Integer transfers = 0;

  /**
   * The cost of this trip
   */
  public ApiItineraryFares fare = new ApiItineraryFares(Map.of(), Map.of(), null, null);

  /**
   * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
   * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6,
   * then walks to their destination, has four legs.
   */
  public List<ApiLeg> legs = new ArrayList<>();

  /**
   * This itinerary has a greater slope than the user requested (but there are no possible
   * itineraries with a good slope).
   */
  public boolean tooSloped = false;

  /**
   * If {@link RouteRequest#allowArrivingInRentalVehicleAtDestination}
   * is set than it is possible to end a trip without dropping off the rented bicycle.
   */
  public boolean arrivedAtDestinationWithRentedBicycle = false;

  /**
   * A sandbox feature for calculating a numeric score between 0 and 1 which indicates
   * how accessible the itinerary is as a whole. This is not a very scientific method but just
   * a rough guidance that expresses certainty or uncertainty about the accessibility.
   *
   * The intended audience for this score are frontend developers wanting to show a simple UI
   * rather than having to iterate over all the stops and trips.
   *
   * Note: the information to calculate this score are all available to the frontend, however
   * calculating them on the backend makes life a little easier and changes are automatically
   * applied to all frontends.
   */
  public Float accessibilityScore;
}
