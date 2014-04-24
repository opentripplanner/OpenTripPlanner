package org.opentripplanner.api.common;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class OTPExceptionMapper implements ExceptionMapper<Exception> {

    public Response toResponse(Exception ex) {
        // Show the exception in the server log
        ex.printStackTrace();
        // Return the short form message to the client
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.toString() + " " + ex.getMessage())
                .type("text/plain").build();
    }

}
