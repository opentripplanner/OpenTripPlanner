package org.opentripplanner.routing.error;

import java.util.concurrent.CompletionException;
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

  public static void unwrapAndRethrowCompletionException(CompletionException e) {
    if (e.getCause() instanceof RoutingValidationException) {
      throw (RoutingValidationException) e.getCause();
    } else if (e.getCause() instanceof RuntimeException) {
      throw (RuntimeException) e.getCause();
    }
    throw e;
  }
}
