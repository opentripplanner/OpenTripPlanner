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

import java.util.Arrays;
import java.util.Collection;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class MIMEImageFormat {
    
    public static final Collection<String> acceptedTypes =
        Arrays.asList("png", "gif", "jpeg", "geotiff");
    
    public final String type;
            
    public MIMEImageFormat(String s) {
        String[] parts = s.split("/");
        if (parts.length == 2 && parts[0].equals("image")) {
            if (acceptedTypes.contains(parts[1])) {
                type = parts[1];
            } else {
                throw new WebApplicationException(Response
                        .status(Status.BAD_REQUEST)
                        .entity("unsupported image format: " + parts[1])
                        .build());
            }
        } else {
            throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("malformed image format mime type: " + s)
                    .build());
        }
    }
 
    public String toString() {
        return "image/" + type;
    }
}
