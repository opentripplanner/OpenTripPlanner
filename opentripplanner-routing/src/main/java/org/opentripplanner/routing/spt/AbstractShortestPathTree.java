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

package org.opentripplanner.routing.spt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opentripplanner.routing.core.Vertex;

/**
 * Common base class for common {@link ShortestPathTree} functionality.
 * 
 * @author bdferris
 */
public abstract class AbstractShortestPathTree implements ShortestPathTree {

    @Override
    public GraphPath getPath(Vertex dest) {
        return getPath(dest, true);
    }

    @Override
    public List<GraphPath> getPaths(Vertex dest, boolean optimize) {
        GraphPath path = getPath(dest, optimize);
        if (path == null)
            return Collections.emptyList();
        return Arrays.asList(path);
    }

    /****
     * Protected Methods
     ****/

    protected GraphPath createPathForVertex(SPTVertex end, boolean optimize) {

        GraphPath ret = new GraphPath();

        SPTEdge prevEdge = null;

        while (true) {
            end = new SPTVertex(end);

            if (prevEdge != null)
                prevEdge.fromv = end;

            ret.vertices.add(0, end);
            if (end.incoming == null) {
                break;
            }
            SPTEdge edge = new SPTEdge(end.incoming);
            end.incoming = edge;
            edge.tov = end;
            ret.edges.add(0, edge);

            end = edge.fromv;
            prevEdge = edge;
        }

        if (optimize) {
            ret.optimize();
        }
        return ret;
    }

}
