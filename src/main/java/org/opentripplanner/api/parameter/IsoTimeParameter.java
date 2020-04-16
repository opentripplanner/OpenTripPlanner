package org.opentripplanner.api.parameter;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.Provider;
import java.util.GregorianCalendar;

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
            throw new BadRequestException("parsing time " + param + ": " + e.getMessage(), e);
        }
    }
}
