package org.opentripplanner.framework.http;

/**
 * Enumeration for HTTP status codes that are not otherwise listed in usual frameworks/APIs
 * like jakarta.ws.rs.core.Response.{@link jakarta.ws.rs.core.Response.Status}
 */
public enum OtpHttpStatus {
  STATUS_UNPROCESSABLE_ENTITY(422, "Unprocessable Content");

  private final int code;
  private final String reason;

  OtpHttpStatus(int statusCode, String reasonPhrase) {
    this.code = statusCode;
    this.reason = reasonPhrase;
  }

  public int statusCode() {
    return code;
  }

  public String reasonPhrase() {
    return reason;
  }
}
