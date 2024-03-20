package org.opentripplanner.framework.io;

public class OtpHttpClientStatusCodeException extends OtpHttpClientException {

  private final int statusCode;

  public OtpHttpClientStatusCodeException(int statusCode) {
    super("HTTP request failed with status code " + statusCode);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
