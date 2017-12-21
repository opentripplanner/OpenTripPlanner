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

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.LineString;

import java.util.List;

/**
 * Represents a transfer between stops that does not take the street network into account.
 *
 * TODO these should really have a set of valid modes in case bike vs. walk transfers are different
 */
public class SimpleTransfer extends TransferEdge {
    private static final long serialVersionUID = 20171009L;

    private List<Edge> edges;

    public SimpleTransfer(TransitStop from, TransitStop to, double distance, LineString geometry, List<Edge> edges) {
        super(from, to, distance);
        setGeometry(geometry);
        this.edges = edges;
        if (edges != null) {
            setWheelchairAccessible(edges.stream().allMatch(Edge::isWheelchairAccessible));
        }
    }

    public SimpleTransfer(TransitStop from, TransitStop to, double distance, LineString geometry) {
        this(from, to, distance, geometry, null);
    }

    @Override
    public String getName() {
        return fromv.getName() + " => " + tov.getName();
    }

    public List<Edge> getEdges() { return this.edges; }

    @Override
    public String toString() {
        return "SimpleTransfer " + getName();
    }
}
