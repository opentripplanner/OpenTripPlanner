package org.opentripplanner.routing.api.response;

public class RoutingError {
  public final RoutingErrorCode code;
  public final InputField inputField;

  public RoutingError(RoutingErrorCode code, InputField inputField) {
    this.code = code;
    this.inputField = inputField;

  }
}
