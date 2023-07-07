package org.opentripplanner.framework.io;

public class OtpHttpClientException extends RuntimeException {

  public OtpHttpClientException(Throwable cause) {
    super(cause);
  }

  public OtpHttpClientException(String message) {
    super(message);
  }
}
