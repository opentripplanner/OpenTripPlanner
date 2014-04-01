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

package org.opentripplanner.api.parameter;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.geotools.geometry.Envelope2D;

/** order is minx,miny,maxx,maxy */
public class EnvelopeParameter {

    public Envelope2D env;


    public EnvelopeParameter(String param) {

        String[] tokens = param.split(",");
        try {
            double minx = Double.parseDouble(tokens[0]);
            double miny = Double.parseDouble(tokens[1]);
            double maxx = Double.parseDouble(tokens[2]);
            double maxy = Double.parseDouble(tokens[3]);
            // null crs, set later from another parameter
            env = new Envelope2D(null, minx, miny, maxx-minx, maxy-miny);
        } catch (Exception e) {
            throw new WebApplicationException(fail(param, e));
        }
    }

    protected Response fail(String param, Exception e) {
        return Response.status(Status.BAD_REQUEST).entity(param + ": " + e.getMessage()).build();
    }

}
