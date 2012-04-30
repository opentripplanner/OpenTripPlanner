package org.opentripplanner.analyst.rest.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.geotools.geometry.Envelope2D;
import com.sun.jersey.spi.StringReader;
import com.sun.jersey.spi.StringReaderProvider;

@SuppressWarnings("rawtypes")
@Provider
public class EnvelopeStringReaderProvider implements StringReaderProvider {
    
    @Override
    public StringReader getStringReader(Class type, Type genericType, Annotation[] annotations) {
        if (type == Envelope2D.class) {
            return new EnvelopeStringReader();
        } else {
            return null;
        }
    }

    private static class EnvelopeStringReader implements StringReader<Envelope2D> {

        @Override
        public Envelope2D fromString(String param) {
            String[] tokens = param.split(",");
            try {
                double minx = Double.parseDouble(tokens[0]);
                double miny = Double.parseDouble(tokens[1]);
                double maxx = Double.parseDouble(tokens[2]);
                double maxy = Double.parseDouble(tokens[3]);
                // null crs, set later from another parameter
                return new Envelope2D(null, minx, miny, maxx-minx, maxy-miny);
            } catch (Exception e) {
                throw new WebApplicationException(fail(param, e));
            }
        }

        protected Response fail(String param, Exception e) {
            return Response.status(Status.BAD_REQUEST).entity(param + ": " + e.getMessage()).build();
        }

    }

}
