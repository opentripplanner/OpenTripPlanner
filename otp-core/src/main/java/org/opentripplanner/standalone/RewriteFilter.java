package org.opentripplanner.standalone;

import java.net.URI;
import java.net.URISyntaxException;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/** 
 * Jersey filter that rewrites URIs to remove /ws/ if present.
 * Grizzly HTTPServer does not like multi-level context paths like 
 * /opentripplanner-api-webapp/ws/ so we have to register the handler only one level deep and 
 * ignore the second level. 
 */
public class RewriteFilter implements ContainerRequestFilter {

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        try {
            // Calling setURIs method clears the cached method, path etc.
            containerRequest.setUris(containerRequest.getBaseUri(), 
                    new URI(containerRequest.getRequestUri().toString().replace("/ws/", "/")));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return containerRequest;
    }
}
