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

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.transit_index.RouteVariant;

public interface TransitIndexService {
    public List<RouteVariant> getVariantsForRoute(AgencyAndId route);

    public RouteVariant getVariantForTrip(AgencyAndId trip);

    public Edge getPreboardEdge(AgencyAndId stop);

    public Edge getPrealightEdge(AgencyAndId stop);

    public Collection<String> getDirectionsForRoute(AgencyAndId route);

    public List<TraverseMode> getAllModes();
}
