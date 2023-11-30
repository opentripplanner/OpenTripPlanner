package org.opentripplanner.transit.model.framework;

import org.opentripplanner.framework.error.OtpError;

/**
 * This class is used to throw a data validation exception. It holds an error which can be
 * inserted into build issue store, into the updater logs or returned to the APIs. The framework
 * to catch and handle this is NOT IN PLACE, see
 * <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/5070">Error code design #5070</a>.
 * <p>
 * MORE WORK ON THIS IS NEEDED!
 */
public class DataValidationException extends RuntimeException {

  private final OtpError error;

  public DataValidationException(OtpError error) {
    super();
    this.error = error;
  }

  public OtpError error() {
    return error;
  }

  @Override
  public String getMessage() {
    return error.message();
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
