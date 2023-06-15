package org.opentripplanner.framework.http;

import jakarta.ws.rs.core.Response;

/**
 * Enumeration for HTTP status codes that are not otherwise listed in usual frameworks/APIs
 * like jakarta.ws.rs.core.Response.{@link jakarta.ws.rs.core.Response.Status}
 */
public enum OtpHttpStatus implements Response.StatusType {
  STATUS_UNPROCESSABLE_ENTITY(422, "Unprocessable Content");

  private final int code;
  private final String reason;

  OtpHttpStatus(int statusCode, String reasonPhrase) {
    this.code = statusCode;
    this.reason = reasonPhrase;
  }

  @Override
  public int getStatusCode() {
    return code;
  }

  @Override
  public Response.Status.Family getFamily() {
    return Response.Status.Family.familyOf(code);
  }

  @Override
  public String getReasonPhrase() {
    return reason;
  }

  @Override
  public Response.Status toEnum() {
    throw new UnsupportedOperationException("A OtpHttpStatus code can not be cast to a Status.");
  }
}
