package org.opentripplanner.street.model.edge;

/**
 * Marker interface for edges which are not part of the street network, but are defined outside
 * the street module and represent a leg of their own - for example a ride on a flexible transit
 * trip. When a path is split into legs, each such edge is turned into a
 * {@link org.opentripplanner.street.model.path.ExternalEdgeLeg}.
 */
public interface ExternalLegEdge {}
