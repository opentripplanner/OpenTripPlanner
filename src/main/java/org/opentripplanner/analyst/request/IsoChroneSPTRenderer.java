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

package org.opentripplanner.analyst.request;

import java.util.List;

import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.routing.core.RoutingRequest;

/**
 * Compute isochrones out of a shortest path tree request.
 * 
 * @author laurent
 */
public interface IsoChroneSPTRenderer {

    /**
     * @param isoChroneRequest Contains a list of cutoff times, etc...
     * @param sptRequest Contains path computation parameters (origin, modes, max walk...)
     * @return A list of IsochroneData, one for each cutoff time in the request (same order).
     */
    List<IsochroneData> getIsochrones(IsoChroneRequest isoChroneRequest, RoutingRequest sptRequest);
}
