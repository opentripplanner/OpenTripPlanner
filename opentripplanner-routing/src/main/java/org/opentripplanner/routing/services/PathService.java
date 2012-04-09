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

package org.opentripplanner.routing.services;

import java.util.List;

import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.spt.GraphPath;

public interface PathService {

    public List<GraphPath> getPaths(TraverseOptions options);

    /**
     * In the case of "arrive-by" routing, the origin state is actually the user's end location and
     * the target vertex is the user's start location.
     */

    /**
     * TODO: there was a separate method to handle intermediates; now the pathservice should just figure this out from the request.
     * 
     * Here we wish to plan a trip that starts at "fromPlace", travels through the intermediate
     * places in some arbitrary but hopefully optimal order, and eventually end up at "toPlace".
     */

    public boolean isAccessible(NamedPlace place, TraverseOptions options);

}
