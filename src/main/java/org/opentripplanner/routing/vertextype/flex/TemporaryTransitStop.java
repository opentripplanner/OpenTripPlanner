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
package org.opentripplanner.routing.vertextype.flex;

import com.google.common.collect.Iterables;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryTransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TemporaryTransitStop extends TransitStop implements TemporaryVertex {

    // stop is *at* a street vertex
    private StreetVertex streetVertex;

    public TemporaryTransitStop(Graph graph, Stop stop, StreetVertex streetVertex) {
        super(graph, stop);
        this.streetVertex = streetVertex;
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }

    @Override
    public void dispose() {
        for (Object temp : Iterables.concat(getIncoming(), getOutgoing())) {
            ((TemporaryEdge) temp).dispose();
        }
    }

    public StreetVertex getStreetVertex() {
        return streetVertex;
    }

    @Override
    public boolean checkCallAndRideBoardAlightOk(State state) {
        return true;
    }
}
