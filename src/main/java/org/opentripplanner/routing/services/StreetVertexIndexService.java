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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Collection;
import java.util.List;

public interface StreetVertexIndexService {

    /**
     * Returns the vertices intersecting with the specified envelope.
     * 
     * @param envelope
     * @return
     */
    public Collection<Vertex> getVerticesForEnvelope(Envelope envelope);

    /**
     * Return the edges whose geometry intersect with the specified envelope. Warning: edges w/o
     * geometry will not be indexed.
     * 
     * @param envelope
     * @return
     */
    public Collection<Edge> getEdgesForEnvelope(Envelope envelope);

    /**
     * Get the closest edges to this location are traversable given these preferences.
     * 
     * @param location
     * @param extraEdges Additional edges to consider, may be null
     * @param preferredEdges Edges which are preferred, may be null
     * @param possibleTransitLinksOnly Only include possible transit links.
     * @return
     */
    public CandidateEdgeBundle getClosestEdges(GenericLocation location,
            TraversalRequirements reqs, List<Edge> extraEdges, Collection<Edge> preferredEdges,
            boolean possibleTransitLinksOnly);

    /**
     * Get the closest edges to this location are traversable given these preferences.
     * 
     * Convenience wrapper for above.
     * 
     * @param location
     * @return
     */
    public CandidateEdgeBundle getClosestEdges(GenericLocation location,
            TraversalRequirements reqs);

    /**
     * @param coordinate
     * @param radiusMeters
     * @return The transit stops within a certain radius of the given location.
     */
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radiusMeters);

    /**
     * @param envelope
     * @return The transit stops within an envelope.
     */
    public List<TransitStop> getTransitStopForEnvelope(Envelope envelope);

    /**
     * Finds the appropriate vertex for this location.
     * 
     * @param place
     * @param options
     * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
     * @return
     */
    public Vertex getVertexForLocation(GenericLocation place, RoutingRequest options,
                                       boolean endVertex);

    /**
     * Finds the on-street coordinate closest to a given coordinate
     *
     * @param coordinate The coordinate to be found on the street network
     * @return The on-street {@link Coordinate} that's closest to the given input {@link Coordinate}
     */
    public Coordinate getClosestPointOnStreet(Coordinate coordinate);

	/** Get a vertex at a given coordinate, using the same logic as in Samples. Used in Analyst
	 * so that origins and destinations are linked the same way. */
	public Vertex getSampleVertexAt(Coordinate coordinate, boolean dest);
}
