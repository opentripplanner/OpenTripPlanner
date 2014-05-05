package org.opentripplanner.standalone;

import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import java.security.Principal;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

/** 
 * This ContainerRequestFilter adds basic authentication support to the Grizzly + Jersey server.
 * Basic authentication is insecure by itself, but short of more complex solutions like oauth it is the best
 * option when coupled with transport-layer security (secure sockets).
 *
 * It seems wasteful to encrypt all communication with the server, but TLS uses very efficient symmetric-key
 * encryption on the messages themselves. Only the TLS handshake uses compute-intensive public key encryption,
 * which establishes a shared key.
 *
 * In Jersey 2 this filter should configure the SecurityContext which will be passed through to the web resources.
 */
@Priority(Priorities.AUTHENTICATION)
// Authentication priority comes before Authorization, which is handled by RolesAllowedDynamicFeature
public class AuthFilter implements ContainerRequestFilter {

    private final Map<String, String> passwords = Maps.newHashMap(); // roles are same as user names

    /* Case-sensitive. */
    public AuthFilter() {
        passwords.put("ROUTERS", "ultra_secret");
    }

    /* Throw an exception if a user is unauthenticated. requestContext.abortWith()? */
    private static void unauthenticated (String user) {
        String message = String.format("Incorrect password for OpenTripPlanner user '%s'", user);
        throw new WebApplicationException(Response.status(Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"OpenTripPlanner\"")
            .entity(message)
            .build());
    }

    /* Throw an exception if user attempts to do basic auth over an unencrypted connection. */
    private static void unencrypted () {
        throw new WebApplicationException(Response.status(Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"OpenTripPlanner\"")
                .entity("OpenTripPlanner refuses to do basic auth without transport layer security (HTTPS).")
                .build());
    }

    @Override
    public void filter(ContainerRequestContext containerRequest) throws WebApplicationException {

        // Get the authentication passed in HTTP headers parameters
        String auth = containerRequest.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null) {
            if (auth.startsWith("Basic ") || auth.startsWith("basic ")) {
                if ( ! containerRequest.getSecurityContext().isSecure()) unencrypted();
                auth = auth.replaceFirst("[Bb]asic ", "");
                String[] split = new String(BaseEncoding.base64().decode(auth)).split(":", 2);
                if (split.length != 2) return;
                String user = split[0];
                String pass = split[1];
                if (pass.equals(passwords.get(user))) {
                    containerRequest.setSecurityContext(makeSecurityContext(user, user));
                } else {
                    unauthenticated (user);
                }
            }
        }
    }

    public static SecurityContext makeSecurityContext (final String name, final String... roles) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return name;
                    }
                };
            }

            @Override
            public boolean isUserInRole(String role) {
                for (String r : roles) { // TODO make a real class with a Set
                    if (r.equals(role)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getAuthenticationScheme() { return SecurityContext.BASIC_AUTH; }

            @Override
            public boolean isSecure() {
                // Is this happening over a secure channel like HTTPS?
                // Yes, we already checked in the filter before creating a security context.
                return true;
            }
        };
    }
}

