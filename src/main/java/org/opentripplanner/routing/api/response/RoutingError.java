package org.opentripplanner.routing.api.response;

public class RoutingError {
  public final RoutingErrorCode code;
  public final String message;
  public final String paramValue;

  public RoutingError( RoutingErrorCode code, String message, String paramValue) {
    this.code = code;
    this.message = message;
    this.paramValue = paramValue;
  }
}
