package org.opentripplanner.analyst.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sun.jersey.spi.StringReader;
import com.sun.jersey.spi.StringReaderProvider;

@SuppressWarnings("rawtypes")
@Provider
public class CRSStringReaderProvider implements StringReaderProvider {
    
    @Override
    public StringReader getStringReader(Class type, Type genericType, Annotation[] annotations) {
        if (type == CoordinateReferenceSystem.class) {
            return new CRSStringReader();
        } else {
            return null;
        }
    }

    private static class CRSStringReader implements StringReader<CoordinateReferenceSystem> {

        @Override
        public CoordinateReferenceSystem fromString(String crsName) {
            try {
                return CRS.decode(crsName, true);
            } catch (Exception e) {
                throw new WebApplicationException(onError(crsName, e));
            }
        }

        protected Response onError(String param, Throwable e) {
            return Response.status(Status.BAD_REQUEST).entity(getErrorMessage(param, e)).build();
        }

        protected String getErrorMessage(String param, Throwable e) {
            return String.format("<H1>400 Bad Request</H1> " +
                    "While parsing parameter %s as %s: <BR> %s",
                    param, CoordinateReferenceSystem.class, e.getMessage());
        }

    }

}
