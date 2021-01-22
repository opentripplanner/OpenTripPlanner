package org.opentripplanner.routing.edgetype;

/** Marker interface for request scoped edges. These edges should only be traversable by the
 * current routing request, and should be removed after the request has finished. */
public interface RequestScopedEdge { }
