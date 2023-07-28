package org.opentripplanner.framework.retry;

public class OtpRetryException extends RuntimeException {

  public OtpRetryException(String message, Throwable cause) {
    super(message, cause);
  }

  public OtpRetryException(Throwable cause) {
    super(cause);
  }
}
