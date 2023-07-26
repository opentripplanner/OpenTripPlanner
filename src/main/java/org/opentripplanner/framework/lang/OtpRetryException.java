package org.opentripplanner.framework.lang;

public class OtpRetryException extends RuntimeException {

  public OtpRetryException(String message, Throwable cause) {
    super(message, cause);
  }

  public OtpRetryException(Throwable cause) {
    super(cause);
  }
}
