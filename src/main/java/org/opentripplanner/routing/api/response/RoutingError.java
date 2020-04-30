package org.opentripplanner.routing.api.response;

import java.util.List;

public class RoutingError {
  public final RoutingErrorCode code;
  public final String message;
  public final List<InputField> inputFields;

  public RoutingError(RoutingErrorCode code, String message, List<InputField> inputFields) {
    this.code = code;
    this.message = message;
    this.inputFields = inputFields;

  }
}
