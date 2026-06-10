package org.opentripplanner.routing.error;

/**
 * Thrown when a routing request contains invalid input. The message is surfaced to the API client
 * as a bad-request error.
 */
public class InvalidRoutingInputException extends RuntimeException {

  public InvalidRoutingInputException(String message) {
    super(message);
  }
}
