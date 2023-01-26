package org.opentripplanner.street.search.state;

public enum VehicleRentalState {
  /**
   * This is the state before any vehicle rental has been initiated.
   */
  BEFORE_RENTING,
  /**
   * A vehicle is being rented from a rental station. This means that the vehicle will need to be
   * dropped off at another station before the search terminates.
   */
  RENTING_FROM_STATION,
  /**
   * A floating vehicle is being rented. It will not need to be dropped off before terminating the
   * search, as it can be dropped off at any street.
   */
  RENTING_FLOATING,
  /**
   * After dropping off a vehicle, this state is entered.
   */
  HAVE_RENTED,
}
