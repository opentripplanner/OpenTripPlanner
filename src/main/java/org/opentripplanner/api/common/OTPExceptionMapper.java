package org.opentripplanner.api.common;

import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class OTPExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = LoggerFactory.getLogger(OTPExceptionMapper.class);

    public Response toResponse(Exception ex) {
        if(ex instanceof WebApplicationException) {
            String header = "";
            if(ex instanceof BadRequestException) {
                header = "FOUR HUNDRED\n\n";
            }
            else if(ex instanceof NotFoundException) {
                header = "FOUR ZERO FOUR\n\n";
            }
            return Response
                    .fromResponse(((WebApplicationException)ex).getResponse())
                    .entity(header + ex.getMessage())
                    .build();
        }
        if(ex instanceof OtpAppException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("OTP Application error: " + ex.getMessage())
                    .type("text/plain").build();
        }

        // Show the exception in the server log
        LOG.error("Unhandled exception", ex);
        // Return the short form message to the client
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.toString() + " " + ex.getMessage())
                .type("text/plain").build();
    }
}
