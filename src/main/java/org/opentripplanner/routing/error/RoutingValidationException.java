package org.opentripplanner.routing.error;

import org.opentripplanner.routing.api.response.RoutingError;

import java.util.List;

public class RoutingValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final List<RoutingError> routingErrors;

  /**
   * An error with the input data which results in itineraries not being returned for a type of
   * search.
   */
  public RoutingValidationException(List<RoutingError> routingErrors) {
    this.routingErrors = routingErrors;
  }

  public List<RoutingError> getRoutingErrors() {
    return routingErrors;
  }
}
