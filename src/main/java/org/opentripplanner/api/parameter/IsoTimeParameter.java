package org.opentripplanner.api.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.GregorianCalendar;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@SuppressWarnings("rawtypes")
@Provider
public class IsoTimeParameter {

    public GregorianCalendar cal;

    public IsoTimeParameter (String param) {
        // WMS spec annex D: time is specified in ISO8601:2000 extended
        // http://stackoverflow.com/questions/2201925/converting-iso8601-compliant-string-to-java-util-date
        try {
            cal = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(param).toGregorianCalendar();
        } catch (Exception e) {
            throw new WebApplicationException(fail(param, e));
        }
    }

    protected Response fail(String param, Exception e) {
        return Response.status(Status.BAD_REQUEST)
                       .entity("parsing time " + param + ": " + e.getMessage())
                       .build();
    }

}
