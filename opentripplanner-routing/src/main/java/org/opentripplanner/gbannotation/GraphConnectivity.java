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

package org.opentripplanner.gbannotation;

import lombok.AllArgsConstructor;

import org.opentripplanner.routing.graph.Vertex;

@AllArgsConstructor
public class GraphConnectivity extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Removed/depedestrianized disconnected subgraph containing: %s at: (%f, %f), with %d";

    final Vertex vertex;
    final int size;

    @Override
    public String getMessage() {
        return String.format(FMT, vertex, vertex.getCoordinate().x, vertex.getCoordinate().y, size);
    }

    @Override
    public Vertex getReferencedVertex() {
        return vertex;
    }

}
