package org.opentripplanner.api.servlet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/** 
 * This Jersey filter can be used to add basic authentication to the Grizzly + Jersey server.
 * A ContainerRequestFilter applies to the entire container rather than a single resource, and
 * filters requests rather than responses.
 * http://simplapi.wordpress.com/2013/01/24/jersey-jax-rs-implements-a-http-basic-auth-decoder/
 */
public class GrizzlyAuthFilter implements ContainerRequestFilter {

    /* The exception thrown if a user is unauthorized. */
    private final static WebApplicationException unauthorized = 
        new WebApplicationException(Response.status(Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"OTP\"")
            .entity("This OTP resource requires authentication.").build());

    public GrizzlyAuthFilter() {
        System.out.println("INSTANTIATING AUTH FILTER");
    }
    
    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) 
            throws WebApplicationException {

        // Automatically allow certain requests.
        String method = containerRequest.getMethod();
        String path = containerRequest.getPath(true);
        if (method.equals("GET")) // && path.endsWith("metadata")) // skip auth for now
            return containerRequest;

        // Get the authentication passed in HTTP headers parameters
        String auth = containerRequest.getHeaderValue("authorization");
        if (auth == null)
            throw unauthorized;
        if (auth.startsWith("Basic ") || auth.startsWith("Basic ")) {
            auth = auth.replaceFirst("[Bb]asic ", "");
            String userColonPass = Base64.base64Decode(auth);
            if (!userColonPass.equals("admin:admin"))
                throw unauthorized;
        } else {
            // fail on unrecognized auth type
            throw unauthorized;
        }
        return containerRequest;
    }
}
