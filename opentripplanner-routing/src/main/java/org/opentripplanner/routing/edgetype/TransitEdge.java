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

import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.vertextype.TransitVertex;

/*
 * An edge in the GTFS layer
 */
public abstract class TransitEdge extends AbstractEdge {

    private static final long serialVersionUID = -7352385580194507791L;

    public TransitEdge(TransitVertex v1, TransitVertex v2) {
        super(v1, v2);
    }

    /* get line, route, pattern */
    
}
