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

package org.opentripplanner.api.resource;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.WorldEnvelope;
import java.io.Serializable;

/**
 * GraphMetada is first created after OSM is build.
 *
 * It has two envelopes. One with all of OSM vertices. This is used in {@link org.opentripplanner.graph_builder.module.StreetLinkerModule} and
 * {@link org.opentripplanner.graph_builder.module.TransitToTaggedStopsModule} to skip stops which are outside OSM data area.
 *
 * During GTFS reading second envelope is expanded to include each TransitStop with {@link #expandToInclude(double, double)}.
 * TransitStop modes are added to transitModes variables with help of
 */
public class GraphEnvelope implements Serializable {

    /**
     * This is envelope with all of OSM data and after {@link #updateEnvelope()} is called with all of data.
     */
    private WorldEnvelope envelope;

    /**
     * This envelope starts with from envelope but gets expanded with {@link #expandToInclude(double, double)} for each {@link org.opentripplanner.routing.vertextype.TransitStop}.
     *
     * In {@link #updateEnvelope()} replaces envelope.
     */
    private transient WorldEnvelope newEnvelope;

    public GraphEnvelope() {
    	// 0-arg constructor avoids com.sun.xml.bind.v2.runtime.IllegalAnnotationsException
    }

    public GraphEnvelope(Graph graph) {
        /* generate extents */
        envelope = new WorldEnvelope();

        for (Vertex v : graph.getVertices()) {
            Coordinate c = v.getCoordinate();
            envelope.expandToInclude(c);
        }

        newEnvelope = new WorldEnvelope(envelope);
    }

    /**
     * @return true if coordinate is contained in graph envelope
     */
    public boolean contains(Coordinate c) {
        return envelope.contains(c);
    }

    /**
     * Expands new envelope to include given point
     *
     * This doesn't change envelope used in {@link #contains(Coordinate)}
     * Because envelope needs to be unchanges because it is used in linking in
     * {@link org.opentripplanner.graph_builder.module.StreetLinkerModule} to see
     * if stops are inside OSM data area.
     *
     * @param  x  the value to lower the minimum x to or to raise the maximum x to
     * @param  y  the value to lower the minimum y to or to raise the maximum y to
     */
    public void expandToInclude(double x, double y) {
        newEnvelope.expandToInclude(x, y);
    }

    /**
     * This switches previous envelope (created from OSM data) with new one
     * which has data from TransitStops and modes
     */
    public void updateEnvelope() {
        envelope = newEnvelope;
    }

    public WorldEnvelope getEnvelope() {
        return envelope;
    }
}
