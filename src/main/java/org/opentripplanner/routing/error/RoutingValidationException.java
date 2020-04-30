package org.opentripplanner.routing.error;

import org.opentripplanner.routing.api.response.RoutingError;

public class RoutingValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final RoutingError routingError;

  /**
   * An error with the input data which results in itineraries not being returned for a type of
   * search.
   */
  public RoutingValidationException(RoutingError routingError) {
    this.routingError = routingError;
  }

  public RoutingError getRoutingError() {
    return routingError;
  }
}
