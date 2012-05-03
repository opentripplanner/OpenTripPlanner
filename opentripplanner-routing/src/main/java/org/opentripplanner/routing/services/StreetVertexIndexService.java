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

package org.opentripplanner.routing.services;

import java.util.Collection;
import java.util.List;

import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl.CandidateEdgeBundle;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public interface StreetVertexIndexService {

    public Vertex getClosestVertex(final Coordinate location, String name, RoutingRequest options);

    public Vertex getClosestVertex(final Coordinate location, String name, RoutingRequest options,
            List<Edge> extraEdges);

    public Collection<Vertex> getVerticesForEnvelope(Envelope envelope);

    public CandidateEdgeBundle getClosestEdges(Coordinate coordinate, RoutingRequest options,
            List<Edge> extraEdges, Collection<Edge> preferredEdges);

    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radius);

    Vertex getVertexForPlace(NamedPlace place, RoutingRequest options);

    Vertex getVertexForPlace(NamedPlace place, RoutingRequest options, Vertex other);

    boolean isAccessible(NamedPlace place, RoutingRequest options);
}
