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

import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.LocationObservation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public interface StreetVertexIndexService {

    /**
     * Get the closest vertex that can be traversed by this request.
     * 
     * @param location the location around which to search
     * @param name the name to attach to the vertex
     * @param request
     * @return
     */
    public Vertex getClosestVertex(final Coordinate location, String name, RoutingRequest request);

    /**
     * Get the closest vertex that can be traversed by this request.
     * 
     * @param location the location around which to search
     * @param name the name to attach to the vertex
     * @param request
     * @param extraEdges edges not in the graph that should be included in the search
     * @return
     */
    public Vertex getClosestVertex(final Coordinate location, String name, RoutingRequest request,
            List<Edge> extraEdges);

    /**
     * Returns the vertices intersecting with the specified envelope.
     * 
     * @param envelope
     * @return
     */
    public Collection<Vertex> getVerticesForEnvelope(Envelope envelope);

    public Collection<StreetEdge> getEdgesForEnvelope(Envelope envelope);

    /**
     * Get the closest edges to this location are traversable given these preferences.
     * 
     * @param location
     * @param prefs Must be able to traverse these edges given these preferences.
     * @param extraEdges Additional edges to consider, may be null
     * @param preferredEdges Edges which are preferred, may be null
     * @param possibleTransitLinksOnly Only include possible transit links.
     * @return
     */
    public CandidateEdgeBundle getClosestEdges(LocationObservation location,
            TraversalRequirements reqs, List<Edge> extraEdges, Collection<Edge> preferredEdges,
            boolean possibleTransitLinksOnly);

    /**
     * Get the closest edges to this location are traversable given these preferences.
     * 
     * Convenience wrapper for above.
     * 
     * @param location
     * @param prefs
     * @return
     */
    public CandidateEdgeBundle getClosestEdges(LocationObservation location,
            TraversalRequirements reqs);

    /**
     * Get the closest edges to this location.
     * 
     * @param coordinate location around which to search.
     * @param options request which must be able to traverse the edges.
     * @param extraEdges additional edges to consider (may be null)
     * @param preferredEdges edges which are preferred (may be null)
     * @param possibleTransitLinksOnly only include possible transit links.
     * @return
     */
    public CandidateEdgeBundle getClosestEdges(Coordinate coordinate, RoutingRequest options,
            List<Edge> extraEdges, Collection<Edge> preferredEdges, boolean possibleTransitLinksOnly);

    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radius);

    public List<TransitStop> getNearbyTransitStops(Coordinate coordinateOne,
            Coordinate coordinateTwo);

    /**
     * Finds the appropriate vertex for this location.
     * 
     * @param location
     * @param options
     * @return
     */
    Vertex getVertexForLocation(GenericLocation location, RoutingRequest options);

    /**
     * Finds the appropriate vertex for this location.
     * 
     * @param place
     * @param options
     * @param other non-null when another vertex has already been found. Passed in so that any extra edges made when locating the previous vertex may
     *        be used to locate this one as well.
     * @return
     */
    Vertex getVertexForLocation(GenericLocation place, RoutingRequest options, Vertex other);
}
