package org.opentripplanner.routing.services;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StopVertex;

import java.util.Collection;
import java.util.List;

public interface StreetVertexIndexService {

    /**
     * Returns the vertices intersecting with the specified envelope.
     * 
     * @param envelope
     * @return
     */
    Collection<Vertex> getVerticesForEnvelope(Envelope envelope);

    /**
     * Return the edges whose geometry intersect with the specified envelope. Warning: edges w/o
     * geometry will not be indexed.
     * 
     * @param envelope
     * @return
     */
    Collection<Edge> getEdgesForEnvelope(Envelope envelope);

    /**
     * @param coordinate
     * @param radiusMeters
     * @return The transit stops within a certain radius of the given location.
     */
    List<StopVertex> getNearbyTransitStops(Coordinate coordinate, double radiusMeters);

    /**
     * @param envelope
     * @return The transit stops within an envelope.
     */
    List<StopVertex> getTransitStopForEnvelope(Envelope envelope);

    /**
     * Finds the appropriate vertex for this location.
     * 
     * @param place
     * @param options
     * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
     * @return
     */
    Vertex getVertexForLocation(GenericLocation place, RoutingRequest options, boolean endVertex);

}
