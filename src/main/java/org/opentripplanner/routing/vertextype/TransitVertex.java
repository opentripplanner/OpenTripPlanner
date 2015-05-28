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
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

/** Abstract base class for vertices in the GTFS layer of the graph. */
public abstract class TransitVertex extends Vertex {

    private static final long serialVersionUID = 53855622892837370L;

    private final Stop stop;

    public TransitVertex(Graph graph, String label, Stop stop) {
        super(graph, label, stop.getLon(), stop.getLat(), new NonLocalizedString(stop.getName()));
        this.stop = stop;
    }

    /** Get the stop at which this TransitVertex is located */
    public AgencyAndId getStopId() {
        return stop.getId();
    }

    /** The passenger-facing stop ID/Code (for systems like TriMet that have this feature). */
    public String getStopCode() {
        return stop.getCode();
    }

    /** The passenger-facing code/name indentifying the platform/quay */
    public String getPlatformCode() {
        return stop.getPlatformCode();
    }

    /** Stop information need by API */
    public Stop getStop() {
        return stop;
    }

}
