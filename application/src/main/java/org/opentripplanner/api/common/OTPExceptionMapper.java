package org.opentripplanner.api.common;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.http.OtpHttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class OTPExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOG = LoggerFactory.getLogger(OTPExceptionMapper.class);

  public Response toResponse(Exception ex) {
    if (ex instanceof WebApplicationException) {
      String header = "";
      if (ex instanceof BadRequestException) {
        header = "FOUR HUNDRED\n\n";
      } else if (ex instanceof NotFoundException) {
        header = "FOUR ZERO FOUR\n\n";
      }
      return Response.fromResponse(((WebApplicationException) ex).getResponse())
        .entity(header + ex.getMessage())
        .build();
    }
    if (ex instanceof OtpAppException) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity("OTP Application error: " + ex.getMessage())
        .type("text/plain")
        .build();
    }
    if (ex instanceof OTPRequestTimeoutException) {
      return Response.status(OtpHttpStatus.STATUS_UNPROCESSABLE_ENTITY)
        .entity("OTP API Processing Timeout")
        .type("text/plain")
        .build();
    }
    if (ex instanceof JsonParseException || ex instanceof MismatchedInputException) {
      return Response.status(Response.Status.BAD_REQUEST)
        .entity(ex.getMessage())
        .type("text/plain")
        .build();
    }

    // Show the exception in the server log
    LOG.error("Unhandled exception", ex);
    // Return the short form message to the client
    return Response.serverError()
      .entity(ex.toString() + " " + ex.getMessage())
      .type("text/plain")
      .build();
  }
}
