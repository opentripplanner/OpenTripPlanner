package org.opentripplanner.standalone;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ContainerResponseWriter;

/**
 * The Same Origin Policy states that JavaScript code (or other scripts) running on a web page may
 * not interact with resources originating from sites with a different hostname, protocol, or port
 * number.
 * 
 * JSONP ("JSON with padding") is a way to get around this security policy, in which the requester
 * provides the name of a callback function as a query parameter, and the server returns the
 * response object wrapped in a call to the specified callback function. The request is made by
 * dynamically inserting a script tag into the client-side web page, since script tags are not held
 * to the same origin policy.
 * 
 * The OTP Leaflet client uses JSONP for its requests, and this filter allows our server to work
 * with this convention.
 * 
 * Despite being very common, this is of course a big hack to defeat a security policy. Modern
 * browsers have "Cross Origin Resource Sharing", so we should probably switch to that some day:
 * http://enable-cors.org/
 * 
 * This filter is also rather hackish. If it finds a "callback" query parameter in the request
 * object, it replaces the ResponseWriter in the response object with a wrapper that prepends the
 * JavaScript function name before the MessageBodyWriter ever gets ahold of the OutputStream, then
 * appends a closing parenthesis as the writing process is finished.
 * 
 * This could probably be more properly done with a MessageBodyWriter, since JSONP requests have the
 * Accept header set to media types like text/javascript, while our REST endpoints "produce" 
 * MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML.
 * 
 * @author abyrd
 * 
 */
public class JsonpFilter implements ContainerResponseFilter {

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        String callback = request.getQueryParameters().getFirst("callback");
        if (callback != null) {
            ContainerResponseWriter writer = response.getContainerResponseWriter();
            response.setContainerResponseWriter(new JsonpResponseWriter(writer, callback));
        }
        return response;
    }

    public static class JsonpResponseWriter implements ContainerResponseWriter {

        private ContainerResponseWriter writer;

        private String callback;

        private OutputStream os;

        public JsonpResponseWriter(ContainerResponseWriter writer, String callback) {
            this.writer = writer;
            this.callback = callback;
        }

        @Override
        public OutputStream writeStatusAndHeaders(long contentLength, ContainerResponse response)
                throws IOException {
            OutputStream os = writer.writeStatusAndHeaders(contentLength, response);
            os.write(callback.getBytes());
            os.write('(');
            this.os = os;
            return os;
        }

        @Override
        public void finish() throws IOException {
            os.write(')');
            writer.finish();
        }

    }
}
