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

import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

public class LabelBasicShortestPathTree implements ShortestPathTree {
    private static final long serialVersionUID = -3899613853043676031L;

    HashMap<String, SPTVertex> vertices;

    public LabelBasicShortestPathTree() {
        vertices = new HashMap<String, SPTVertex>();
    }

    public SPTVertex addVertex(Vertex vv, State ss, double weightSum, TraverseOptions options) {
        SPTVertex ret = this.vertices.get(vv.getLabel());
        if (ret == null) {
            ret = new SPTVertex(vv, ss, weightSum, options);
            this.vertices.put(vv.getLabel(), ret);
        } else {
            if (weightSum < ret.weightSum) {
                ret.weightSum = weightSum;
                ret.state = ss;
                ret.options = options;
            } else {
                return null;
            }
        }
        return ret;
    }

    public Collection<SPTVertex> getVertices() {
        return this.vertices.values();
    }

    public SPTVertex getVertex(Vertex vv) {
        return (SPTVertex) this.vertices.get(vv.getLabel());
    }

    public GraphPath getPath(Vertex dest) {
        return getPath(dest, true);
    }
    
    public GraphPath getPath(Vertex dest, boolean optimize) {
        SPTVertex end = this.getVertex(dest);
        if (end == null) {
            return null;
        }

        GraphPath ret = new GraphPath();
        while (true) {
            ret.vertices.add(0, end);
            if (end.incoming == null) {
                break;
            }
            ret.edges.add(0, end.incoming);
            end = end.incoming.fromv;
        }
        if (optimize) {
            ret.optimize();
        }
        return ret;
    }

    public String toString() {
        return "SPT " + this.vertices.size();
    }

    @Override
    public void removeVertex(SPTVertex vertex) {
        vertices.remove(vertex.mirror);
    }
}
