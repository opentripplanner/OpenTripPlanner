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

package org.opentripplanner.routing.algorithm.strategies;

import java.io.Serializable;
import java.util.HashSet;

import org.opentripplanner.routing.graph.Vertex;

public class TransitLocalStreetService implements Serializable {

    private static final long serialVersionUID = -1720564501158183582L;

    private HashSet<Vertex> vertices;

    public TransitLocalStreetService(HashSet<Vertex> vertices) {
        this.vertices = vertices;
    }

    public boolean transferrable(Vertex v) {
        return vertices.contains(v);
    }

}
