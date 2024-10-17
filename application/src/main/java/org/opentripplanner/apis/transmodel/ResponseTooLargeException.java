package org.opentripplanner.apis.transmodel;

/**
 * Exception thrown when the API response exceeds a configurable limit.
 */
public class ResponseTooLargeException extends RuntimeException {

  public ResponseTooLargeException(String message) {
    super(message);
  }
}
