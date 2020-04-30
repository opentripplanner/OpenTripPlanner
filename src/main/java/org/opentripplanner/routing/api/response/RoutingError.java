package org.opentripplanner.routing.api.response;

import java.util.List;

public class RoutingError {
  public final RoutingErrorCode code;
  public final List<InputField> inputFields;

  public RoutingError(RoutingErrorCode code, List<InputField> inputFields) {
    this.code = code;
    this.inputFields = inputFields;

  }
}
