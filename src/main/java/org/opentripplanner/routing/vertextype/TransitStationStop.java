/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

/**
 * Common abstract superclass for Stations and Stops.
 * They come from the same table in GTFS, but we want to distinguish between them.
 */
public abstract class TransitStationStop extends OffboardVertex {
    private static final long serialVersionUID = 1L;

    public TransitStationStop(Graph graph, Stop stop) {
        super(graph, GtfsLibrary.convertIdToString(stop.getId()), stop);
    }
}
