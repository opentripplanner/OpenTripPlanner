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

import org.opentripplanner.routing.graph.Graph;

public class ExitVertex extends OsmVertex {
    
    private static final long serialVersionUID = -1403959315797898914L;
    private String exitName;
    
    public ExitVertex(Graph g, String label, double x, double y, long nodeId) {
        super(g, label, x, y, nodeId);
    }

    public String getExitName() {
        return exitName;
    }

    public void setExitName(String exitName) {
        this.exitName = exitName;
    }

    public String toString() {
        return "ExitVertex(" + super.toString() + ")";
    }
}
