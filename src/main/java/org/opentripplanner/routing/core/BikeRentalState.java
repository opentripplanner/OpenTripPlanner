package org.opentripplanner.routing.core;

public enum BikeRentalState {
  /**
   * This is the state before any bike rental has been initiated.
   */
  BEFORE_RENTING,
  /**
   * A bike or scooter is being rented from a bike rental station. This means that the bike will
   * need to be dropped off at another station before the search terminates.
   */
  RENTING_FROM_STATION,
  /**
   * A floating bike or scooter is being rented. It will not need to be dropped off before
   * terminating the search, as it can be dropped off at any street.
   */
  RENTING_FLOATING,
  /**
   * After dropping off a bike or scooter, this state is entered.
   */
  HAVE_RENTED
}
