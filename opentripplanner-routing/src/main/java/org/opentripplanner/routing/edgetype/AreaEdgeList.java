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

package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AreaEdgeList implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 969137349467214074L;
    private List<AreaEdge> edges = new ArrayList<AreaEdge>();

    public List<AreaEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<AreaEdge> edges) {
        this.edges = edges;
    }
    
    public void addEdge(AreaEdge edge) {
        this.edges.add (edge);
    }

    public void removeEdge(AreaEdge edge) {
        this.edges.remove(edge);
    }
    
}
