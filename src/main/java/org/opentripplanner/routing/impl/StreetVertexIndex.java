package org.opentripplanner.routing.impl;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.I18NString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * 
 * Creates a TemporaryStreetLocation representing a location on a street that's not at an
 * intersection, based on input latitude and longitude. Instantiating this class is expensive,
 * because it creates a spatial index of all of the intersections in the graph.
 */
public class StreetVertexIndex {

    private Graph graph;

    /**
     * Contains only instances of {@link StreetEdge}
     */
    private SpatialIndex edgeTree;
    private SpatialIndex transitStopTree;
    private SpatialIndex verticesTree;

    // If a point is within MAX_CORNER_DISTANCE, it is treated as at the corner.
    private static final double MAX_CORNER_DISTANCE_METERS = 10;

    private SimpleStreetSplitter simpleStreetSplitter;

    public StreetVertexIndex(Graph graph) {
        this(graph, true);
    }

    public StreetVertexIndex(Graph graph, boolean hashGrid) {
        this.graph = graph;
        if (hashGrid) {
            edgeTree = new HashGridSpatialIndex<>();
            transitStopTree = new HashGridSpatialIndex<>();
            verticesTree = new HashGridSpatialIndex<>();
        } else {
            edgeTree = new STRtree();
            transitStopTree = new STRtree();
            verticesTree = new STRtree();
        }
        postSetup();
        if (!hashGrid) {
            ((STRtree) edgeTree).build();
            ((STRtree) transitStopTree).build();
            simpleStreetSplitter = new SimpleStreetSplitter(
                    this.graph,
                    null,
                    null,
                    false,
                    new DataImportIssueStore(
                            false)
            );
        } else {
            simpleStreetSplitter = new SimpleStreetSplitter(
                    this.graph,
                    (HashGridSpatialIndex<Edge>) edgeTree,
                    transitStopTree,
                    false,
                    new DataImportIssueStore(false)
            );
        }

    }

    /**
     * Creates a TemporaryStreetLocation on the given street (set of PlainStreetEdges). How far
     * along is controlled by the location parameter, which represents a distance along the edge
     * between 0 (the from vertex) and 1 (the to vertex).
     *
     * @param graph
     *
     * @param label
     * @param name
     * @param edges A collection of nearby edges, which represent one street.
     * @param nearestPoint
     *
     * @return the new TemporaryStreetLocation
     */
    public static TemporaryStreetLocation createTemporaryStreetLocation(Graph graph, String label,
            I18NString name, Iterable<StreetEdge> edges, Coordinate nearestPoint, boolean endVertex) {
        boolean wheelchairAccessible = false;

        TemporaryStreetLocation location = new TemporaryStreetLocation(label, nearestPoint, name, endVertex);

        for (StreetEdge street : edges) {
            Vertex fromv = street.getFromVertex();
            Vertex tov = street.getToVertex();
            wheelchairAccessible |= street.isWheelchairAccessible();

            /* forward edges and vertices */
            Vertex edgeLocation;
            if (SphericalDistanceLibrary.distance(nearestPoint, fromv.getCoordinate()) < 1) {
                // no need to link to area edges caught on-end
                edgeLocation = fromv;

                if (endVertex) {
                    new TemporaryFreeEdge(edgeLocation, location);
                } else {
                    new TemporaryFreeEdge(location, edgeLocation);
                }
            } else if (SphericalDistanceLibrary.distance(nearestPoint, tov.getCoordinate()) < 1) {
                // no need to link to area edges caught on-end
                edgeLocation = tov;

                if (endVertex) {
                    new TemporaryFreeEdge(edgeLocation, location);
                } else {
                    new TemporaryFreeEdge(location, edgeLocation);
                }
            } else {
                // location is somewhere in the middle of the edge.
                edgeLocation = location;

                // creates links from street head -> location -> street tail.
                createHalfLocation(location, name,
                        nearestPoint, street, endVertex);
            }
        }
        location.setWheelchairAccessible(wheelchairAccessible);
        return location;

    }

    private static void createHalfLocation(TemporaryStreetLocation base, I18NString name,
                Coordinate nearestPoint, StreetEdge street, boolean endVertex) {
        StreetVertex tov = (StreetVertex) street.getToVertex();
        StreetVertex fromv = (StreetVertex) street.getFromVertex();
        LineString geometry = street.getGeometry();

        P2<LineString> geometries = getGeometry(street, nearestPoint);

        double totalGeomLength = geometry.getLength();
        double lengthRatioIn = geometries.first.getLength() / totalGeomLength;

        double lengthIn = street.getDistanceMeters() * lengthRatioIn;
        double lengthOut = street.getDistanceMeters() * (1 - lengthRatioIn);

        if (endVertex) {
            TemporaryPartialStreetEdge temporaryPartialStreetEdge = new TemporaryPartialStreetEdge(
                    street, fromv, base, geometries.first, name, lengthIn);

            temporaryPartialStreetEdge.setNoThruTraffic(street.isNoThruTraffic());
            temporaryPartialStreetEdge.setStreetClass(street.getStreetClass());
        } else {
            TemporaryPartialStreetEdge temporaryPartialStreetEdge = new TemporaryPartialStreetEdge(
                    street, base, tov, geometries.second, name, lengthOut);

            temporaryPartialStreetEdge.setStreetClass(street.getStreetClass());
            temporaryPartialStreetEdge.setNoThruTraffic(street.isNoThruTraffic());
        }
    }

    private static P2<LineString> getGeometry(StreetEdge e, Coordinate nearestPoint) {
        LineString geometry = e.getGeometry();
        return GeometryUtils.splitGeometryAtPoint(geometry, nearestPoint);
    }

    @SuppressWarnings("rawtypes")
    private void postSetup() {
        for (Vertex gv : graph.getVertices()) {
            Vertex v = gv;
            /*
             * We add all edges with geometry, skipping transit, filtering them out after. We do not
             * index transit edges as we do not need them and some GTFS do not have shape data, so
             * long straight lines between 2 faraway stations will wreck performance on a hash grid
             * spatial index.
             * 
             * If one need to store transit edges in the index, we could improve the hash grid
             * rasterizing splitting long segments.
             */
            for (Edge e : gv.getOutgoing()) {
                LineString geometry = e.getGeometry();
                if (geometry == null) {
                    continue;
                }
                Envelope env = geometry.getEnvelopeInternal();
                if (edgeTree instanceof HashGridSpatialIndex)
                    ((HashGridSpatialIndex)edgeTree).insert(geometry, e);
                else
                    edgeTree.insert(env, e);
            }
            if (v instanceof TransitStopVertex) {
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
            Envelope env = new Envelope(v.getCoordinate());
            verticesTree.insert(env, v);
        }
    }

    /**
     * Get all transit stops within a given distance of a coordinate
     * @return The transit stops within a certain radius of the given location.
     */
    public List<TransitStopVertex> getNearbyTransitStops(Coordinate coordinate, double radius) {
        Envelope env = new Envelope(coordinate);
        env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(radius, coordinate.y),
                SphericalDistanceLibrary.metersToDegrees(radius));
        List<TransitStopVertex> nearby = getTransitStopForEnvelope(env);
        List<TransitStopVertex> results = new ArrayList<TransitStopVertex>();
        for (TransitStopVertex v : nearby) {
            if (SphericalDistanceLibrary.distance(v.getCoordinate(), coordinate) <= radius) {
                results.add(v);
            }
        }
        return results;
    }

    /**
     * Returns the vertices intersecting with the specified envelope.
     */
    @SuppressWarnings("unchecked")
    public List<Vertex> getVerticesForEnvelope(Envelope envelope) {
        List<Vertex> vertices = verticesTree.query(envelope);
        // Here we assume vertices list modifiable
        for (Iterator<Vertex> iv = vertices.iterator(); iv.hasNext();) {
            Vertex v = iv.next();
            if (!envelope.contains(new Coordinate(v.getLon(), v.getLat())))
                iv.remove();
        }
        return vertices;
    }

    /**
     * Return the edges whose geometry intersect with the specified envelope. Warning: edges w/o
     * geometry will not be indexed.
     */
    @SuppressWarnings("unchecked")
    public Collection<Edge> getEdgesForEnvelope(Envelope envelope) {
        List<Edge> edges = edgeTree.query(envelope);
        for (Iterator<Edge> ie = edges.iterator(); ie.hasNext();) {
            Edge e = ie.next();
            Envelope eenv = e.getGeometry().getEnvelopeInternal();
            //Envelope eenv = e.getEnvelope();
            if (!envelope.intersects(eenv))
                ie.remove();
        }
        return edges;
    }

    /**
     * @return The transit stops within an envelope.
     */
    @SuppressWarnings("unchecked")
    public List<TransitStopVertex> getTransitStopForEnvelope(Envelope envelope) {
        List<TransitStopVertex> stopVertices = transitStopTree.query(envelope);
        for (Iterator<TransitStopVertex> its = stopVertices.iterator(); its.hasNext();) {
            TransitStopVertex ts = its.next();
            if (!envelope.intersects(new Coordinate(ts.getLon(), ts.getLat())))
                its.remove();
        }
        return stopVertices;
    }

    /**
     * @param coordinate Location to search intersection at. Look in a MAX_CORNER_DISTANCE_METERS radius.
     * @return The nearest intersection, null if none found.
     */
    public StreetVertex getIntersectionAt(Coordinate coordinate) {
        double dLon = SphericalDistanceLibrary.metersToLonDegrees(MAX_CORNER_DISTANCE_METERS,
                coordinate.y);
        double dLat = SphericalDistanceLibrary.metersToDegrees(MAX_CORNER_DISTANCE_METERS);
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(dLon, dLat);
        List<Vertex> nearby = getVerticesForEnvelope(envelope);
        StreetVertex nearest = null;
        double bestDistanceMeter = Double.POSITIVE_INFINITY;
        for (Vertex v : nearby) {
            if (v instanceof StreetVertex) {
                v.getLabel().startsWith("osm:");
                double distanceMeter = SphericalDistanceLibrary.fastDistance(coordinate, v.getCoordinate());
                if (distanceMeter < MAX_CORNER_DISTANCE_METERS) {
                    if (distanceMeter < bestDistanceMeter) {
                        bestDistanceMeter = distanceMeter;
                        nearest = (StreetVertex) v;
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Gets a set of vertices corresponding to the location provided. It first tries to match a
     * Stop/StopCollection by id, and if not successful it uses the coordinates if provided.
     * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
     */
    public Set<Vertex> getVerticesForLocation(
            GenericLocation location,
            RoutingRequest options,
            boolean endVertex
    ) {
        // Check if Stop/StopCollection is found by FeedScopeId
        if(location.stopId != null) {
            Set<Vertex> transitStopVertices = graph.getStopVerticesById(location.stopId);
            if (transitStopVertices != null) {
                return transitStopVertices;
            }
        }

        // Check if coordinate is provided and connect it to graph
        Coordinate coordinate = location.getCoordinate();
        if (coordinate != null) {
            //return getClosestVertex(loc, options, endVertex);
            return Collections.singleton(
                    simpleStreetSplitter.getClosestVertex(location, options, endVertex));
        }

        return null;
    }

    @Override
    public String toString() {
        return getClass().getName() + " -- edgeTree: " + edgeTree.toString() + " -- verticesTree: " + verticesTree.toString();
    }

    /**
     * Finds the appropriate vertex for this location.
     * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
     */
    public Vertex getVertexForLocation(
            GenericLocation location,
            RoutingRequest options,
            boolean endVertex
    ) {
        // Check if coordinate is provided and connect it to graph
        Coordinate coordinate = location.getCoordinate();
        if (coordinate != null) {
            //return getClosestVertex(loc, options, endVertex);
            return simpleStreetSplitter.getClosestVertex(location, options, endVertex);
        }

        return null;
    }
}
