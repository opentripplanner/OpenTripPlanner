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
