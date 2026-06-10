package org.opentripplanner.apis.support;

/**
 * Exception thrown when an API request contains invalid input values or
 * argument combinations from the client. Handled by
 * {@link org.opentripplanner.apis.support.graphql.OtpDataFetcherExceptionHandler}
 * as a client error ({@code BadRequestError} classification) rather than a server error.
 */
public class InvalidInputException extends RuntimeException {

  public InvalidInputException(String message) {
    super(message);
  }
}
