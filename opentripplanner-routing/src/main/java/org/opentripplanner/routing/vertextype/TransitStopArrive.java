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

package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

public class TransitStopArrive extends OffboardVertex {

    private static final long serialVersionUID = 9213431651426739857L;
    private TransitStop stopVertex;

    public TransitStopArrive(Graph g, Stop stop, TransitStop stopVertex) {
        super(g, GtfsLibrary.convertIdToString(stop.getId()) + "_arrive", stop);
        this.stopVertex = stopVertex;
    }

    public TransitStop getStopVertex() {
        return stopVertex;
    }

    @Override
    public AgencyAndId getStopId() {
        return stopVertex.getStopId();
    }
}
