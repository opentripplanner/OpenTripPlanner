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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *  WMS allows several layers and styles to be specified. We parse these parameters as if they 
 *  might contain a comma-separated list, but only use the first one in the WMS resource.
 *  This class also uppercases the query parameters to make sure they match enum constants.
 *  
 *  Type erasure makes a genericized EnumList impractical, so StyleList contains duplicate code.
 */
public class LayerList {
    List<Layer> layers = new ArrayList<Layer>(); 
    
    public LayerList(String v) {
        for (String s : v.split(",")) {
            try {
                layers.add(Layer.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("unknown layer name: " + s)
                    .build());
            }
        }
    }

    public Layer get(int index) {
        return layers.get(index);
    }
}

