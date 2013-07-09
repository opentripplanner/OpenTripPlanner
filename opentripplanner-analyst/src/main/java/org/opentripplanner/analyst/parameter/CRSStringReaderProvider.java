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
