/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class GraphLibrary {

    public static Collection<Edge> getIncomingEdges(Graph graph, Vertex tov,
            Map<Vertex, List<Edge>> extraEdges) {

        if (extraEdges.containsKey(tov)) {
            Collection<Edge> ret = new ArrayList<Edge>(tov.getIncoming());
            ret.addAll(extraEdges.get(tov));
            return ret;
        } else {
            return tov.getIncoming();
        }

    }

    public static Collection<Edge> getOutgoingEdges(Graph graph, Vertex fromv,
            Map<Vertex, List<Edge>> extraEdges) {

        if (extraEdges.containsKey(fromv)) {
            Collection<Edge> ret = new ArrayList<Edge>(fromv.getOutgoing());
            ret.addAll(extraEdges.get(fromv));
            return ret;
        } else {
            return fromv.getOutgoing();
        }
    }

}
