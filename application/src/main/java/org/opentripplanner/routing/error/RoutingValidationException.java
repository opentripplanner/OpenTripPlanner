package org.opentripplanner.routing.error;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;

public class RoutingValidationException extends RuntimeException {

  private final List<RoutingError> routingErrors;

  /**
   * An error with the input data which results in itineraries not being returned for a type of
   * search.
   */
  public RoutingValidationException(List<RoutingError> routingErrors) {
    this.routingErrors = routingErrors;
  }

  public static void unwrapAndRethrowCompletionException(CompletionException e) {
    if (e.getCause() instanceof RuntimeException cause) {
      throw cause;
    }
    throw e;
  }

  public static void unwrapAndRethrowExecutionException(ExecutionException e) {
    if (e.getCause() instanceof RuntimeException cause) {
      throw cause;
    }
    throw new RuntimeException(e);
  }

  public List<RoutingError> getRoutingErrors() {
    return routingErrors;
  }

  @Override
  public String getMessage() {
    return routingErrors.stream().map(Objects::toString).collect(Collectors.joining("\n"));
  }

  public boolean isFromLocationNotFound() {
    return isLocationNotFound(InputField.FROM_PLACE);
  }

  public boolean isToLocationNotFound() {
    return isLocationNotFound(InputField.TO_PLACE);
  }

  public boolean isFromToLocationNotFound() {
    return isFromLocationNotFound() && isToLocationNotFound();
  }

  private boolean isLocationNotFound(InputField location) {
    return (
      routingErrors != null &&
      routingErrors
        .stream()
        .anyMatch(
          routingError ->
            routingError.code == RoutingErrorCode.LOCATION_NOT_FOUND &&
            routingError.inputField == location
        )
    );
  }
}
