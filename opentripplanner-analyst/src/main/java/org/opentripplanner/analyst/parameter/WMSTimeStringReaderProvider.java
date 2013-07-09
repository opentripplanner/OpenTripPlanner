/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analyst.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.GregorianCalendar;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import com.sun.jersey.spi.StringReader;
import com.sun.jersey.spi.StringReaderProvider;

@SuppressWarnings("rawtypes")
@Provider
public class WMSTimeStringReaderProvider implements StringReaderProvider {
    
    @Override
    public StringReader getStringReader(Class type, Type genericType, Annotation[] annotations) {
        if (type == GregorianCalendar.class) {
            return new WMSTimeStringReader();
        } else {
            return null;
        }
    }
    

    private static class WMSTimeStringReader implements StringReader<GregorianCalendar> {
        
        @Override
        public GregorianCalendar fromString(String param) {
            // WMS spec annex D: time is specified in ISO8601:2000 extended
            // http://stackoverflow.com/questions/2201925/converting-iso8601-compliant-string-to-java-util-date
            try {
                return javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(param).toGregorianCalendar();
            } catch (Exception e) {
                throw new WebApplicationException(fail(param, e));
            }
        }

        // in abstract CustomStringReader
        // private abstract <T> convert (String param);
        
        protected Response fail(String param, Exception e) {
            return Response.status(Status.BAD_REQUEST)
                           .entity("parsing time " + param + ": " + e.getMessage())
                           .build();
        }

    }

}
