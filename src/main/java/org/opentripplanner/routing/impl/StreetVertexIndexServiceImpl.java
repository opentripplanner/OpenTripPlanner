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

package org.opentripplanner.routing.impl;


import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.SampleVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * 
 * Creates a TemporaryStreetLocation representing a location on a street that's not at an
 * intersection, based on input latitude and longitude. Instantiating this class is expensive,
 * because it creates a spatial index of all of the intersections in the graph.
 */
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService {

    private Graph graph;

    /**
     * Contains only instances of {@link StreetEdge}
     */
    private SpatialIndex edgeTree;
    private SpatialIndex transitStopTree;
    private SpatialIndex verticesTree;

    // private static final double SEARCH_RADIUS_M = 100; // meters
    // private static final double SEARCH_RADIUS_DEG = DistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    // Maximum difference in distance for two geometries to be considered coincident, plate-carée Euclidean
    // 0.001 ~= 100m at equator
    public static final double DISTANCE_ERROR = 0.000001;

    // If a point is within MAX_CORNER_DISTANCE, it is treated as at the corner.
    private static final double MAX_CORNER_DISTANCE_METERS = 10;
    
    // Edges will only be found if they are closer than this distance
    // TODO: this default may be too large?
    public static final double MAX_DISTANCE_FROM_STREET_METERS = 1000;
    
    private static final double MAX_DISTANCE_FROM_STREET_DEGREES =
            MAX_DISTANCE_FROM_STREET_METERS * 180 / Math.PI / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M;

    static final Logger LOG = LoggerFactory.getLogger(StreetVertexIndexServiceImpl.class);

    public StreetVertexIndexServiceImpl(Graph graph) {
        this(graph, true);
    }

    public StreetVertexIndexServiceImpl(Graph graph, boolean hashGrid) {
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

        TemporaryStreetLocation location = new TemporaryStreetLocation(label, nearestPoint, name,
                endVertex);
        for (StreetEdge street : edges) {
            Vertex fromv = street.getFromVertex();
            Vertex tov = street.getToVertex();
            wheelchairAccessible |= ((StreetEdge) street).isWheelchairAccessible();
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

        double lengthIn = street.getDistance() * lengthRatioIn;
        double lengthOut = street.getDistance() * (1 - lengthRatioIn);

        if (endVertex) {
            TemporaryPartialStreetEdge temporaryPartialStreetEdge = new TemporaryPartialStreetEdge(
                    street, fromv, base, geometries.first, name, lengthIn);

            temporaryPartialStreetEdge.setElevationProfile(ElevationUtils
                    .getPartialElevationProfile(street.getElevationProfile(), 0, lengthIn), false);
            temporaryPartialStreetEdge.setNoThruTraffic(street.isNoThruTraffic());
            temporaryPartialStreetEdge.setStreetClass(street.getStreetClass());
        } else {
            TemporaryPartialStreetEdge temporaryPartialStreetEdge = new TemporaryPartialStreetEdge(
                    street, base, tov, geometries.second, name, lengthOut);

            temporaryPartialStreetEdge.setElevationProfile(ElevationUtils
                    .getPartialElevationProfile(street.getElevationProfile(), lengthIn,
                    lengthIn + lengthOut), false);
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
                if (e instanceof PatternEdge)
                    continue;
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
            if (v instanceof TransitStop) {
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
            Envelope env = new Envelope(v.getCoordinate());
            verticesTree.insert(env, v);
        }
    }

    /**
     * Get all transit stops within a given distance of a coordinate
     */
    @Override
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radius) {
        Envelope env = new Envelope(coordinate);
        env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(radius, coordinate.y),
                SphericalDistanceLibrary.metersToDegrees(radius));
        List<TransitStop> nearby = getTransitStopForEnvelope(env);
        List<TransitStop> results = new ArrayList<TransitStop>();
        for (TransitStop v : nearby) {
            if (SphericalDistanceLibrary.distance(v.getCoordinate(), coordinate) <= radius) {
                results.add(v);
            }
        }
        return results;
    }

    /**
     * Convenience helper for when extraEdges is empty/null.
     */
    private Vertex getClosestVertex(final GenericLocation location, RoutingRequest options,
                                    boolean endVertex) {
        return getClosestVertex(location, options, null, endVertex);
    }

    /**
     * Returns the closest vertex for this GenericLocation. If necessary, this vertex will be created by splitting nearby edges (non-permanently).
     * 
     * This method is the heart of the logic that searches for the start and endpoints of a
     * RoutingRequest.
     */
    private Vertex getClosestVertex(final GenericLocation location, RoutingRequest options,
            List<Edge> extraEdges, boolean endVertex) {
        LOG.debug("Looking for/making a vertex near {}", location);

        // first, check for intersections very close by
        Coordinate coord = location.getCoordinate();
        StreetVertex intersection = getIntersectionAt(coord);
        I18NString calculatedName = null;
        Locale locale;
        if (options == null) {
            locale = ResourceBundleSingleton.INSTANCE.getDefaultLocale();
        } else {
            locale = options.locale;
        }
        if (location.name != null) {
            calculatedName = new NonLocalizedString(location.name);
        }
        if (intersection != null) {
            // We have an intersection vertex. Check that this vertex has edges we can traverse.
            boolean canEscape = false;
            if (options == null) {
                canEscape = true; // Some tests do not supply options.
            } else {
                TraversalRequirements reqs = new TraversalRequirements(options);
                for (StreetEdge e : Iterables.filter ( options.arriveBy ?
                        intersection.getIncoming() : intersection.getOutgoing(),
                        StreetEdge.class)) {
                    if (reqs.canBeTraversed(e)) {
                        canEscape = true;
                        break;
                    }
                }
            }       
            if (canEscape) { 
                // Coordinate is at an intersection or street endpoint, and has traversible edges.
                if (!location.hasName()) {
                    LOG.debug("found intersection {}. not splitting.", intersection);

                    calculatedName = intersection.getIntersectionName(locale);

                }
                TemporaryStreetLocation closest = new TemporaryStreetLocation(
                        "corner " + Math.random(), coord, calculatedName, endVertex);
                if (endVertex) {
                    new TemporaryFreeEdge(intersection, closest);
                } else {
                    new TemporaryFreeEdge(closest, intersection);
                }

                return closest;
            }
        }

        // if no intersection vertices were found, then find the closest transit stop
        // (we can return stops here because this method is not used when street-transit linking)
        double closestStopDistance = Double.POSITIVE_INFINITY;
        Vertex closestStop = null;
        // elsewhere options=null means no restrictions, find anything.
        // here we skip examining stops, as they are really only relevant when transit is being used
        if (options != null && options.modes.isTransit()) {
            for (TransitStop v : getNearbyTransitStops(coord, 1000)) {
                if (!v.isStreetLinkable()) continue;

                double d = SphericalDistanceLibrary.distance(v.getCoordinate(), coord);
                if (d < closestStopDistance) {
                    closestStopDistance = d;
                    closestStop = v;
                }
            }
        }
        LOG.debug(" best stop: {} distance: {}", closestStop, closestStopDistance);

        // then find closest walkable street
        TemporaryStreetLocation closestStreet = null;
        CandidateEdgeBundle bundle = getClosestEdges(location, options, extraEdges, null, false);
        CandidateEdge candidate = bundle.best;
        double closestStreetDistance = Double.POSITIVE_INFINITY;
        if (candidate != null) {
            StreetEdge bestStreet = candidate.edge;
            Coordinate nearestPoint = candidate.nearestPointOnEdge;
            closestStreetDistance = SphericalDistanceLibrary.distance(coord, nearestPoint);
            LOG.debug("best street: {} dist: {}", bestStreet.toString(), closestStreetDistance);
            if (calculatedName == null || "".equals(calculatedName.toString(locale))) {
                calculatedName = new NonLocalizedString(bestStreet.getName(locale));
            }
            //TODO: Where is this closestName actually used?
            String closestName = String.format("%s_%s", calculatedName.toString(locale), location.toString());
            closestStreet = createTemporaryStreetLocation(graph, closestName, calculatedName,
                    bundle.toEdgeList(), nearestPoint, endVertex);
        }

        // decide whether to return street, or street + stop
        if (closestStreet == null) {
            // no street found, return closest stop or null
            LOG.debug("returning only transit stop (no street found)");
            return closestStop; // which will be null if none was found
        } else {
            // street found
            if (closestStop != null) {
                // both street and stop found
                double relativeStopDistance = closestStopDistance / closestStreetDistance;
                if (relativeStopDistance < 1.5) {
                    LOG.debug("linking transit stop to street (distances are comparable)");
                    if (endVertex) {
                        new TemporaryFreeEdge(closestStop, closestStreet);
                    } else {
                        new TemporaryFreeEdge(closestStreet, closestStop);
                    }
                }
            }
            LOG.debug("returning split street");
            return closestStreet;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
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

    @SuppressWarnings("unchecked")
    @Override
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

    @SuppressWarnings("unchecked")
    @Override
    public List<TransitStop> getTransitStopForEnvelope(Envelope envelope) {
        List<TransitStop> transitStops = transitStopTree.query(envelope);
        for (Iterator<TransitStop> its = transitStops.iterator(); its.hasNext();) {
            TransitStop ts = its.next();
            if (!envelope.intersects(new Coordinate(ts.getLon(), ts.getLat())))
                its.remove();
        }
        return transitStops;
    }

    @Override
    public CandidateEdgeBundle getClosestEdges(GenericLocation location,
            TraversalRequirements reqs, List<Edge> extraEdges, Collection<Edge> preferredEdges,
            boolean possibleTransitLinksOnly) {
        Coordinate coordinate = location.getCoordinate();
        Envelope envelope = new Envelope(coordinate);

        double envelopeGrowthAmount = 0.001; // ~= 100 meters at equator
        
        // in latitude degrees, converted to longitude degrees as needed
        double radius = 0;
        
        double xscale = Math.cos(coordinate.y * Math.PI / 180);
        
        CandidateEdgeBundle candidateEdges = new CandidateEdgeBundle();
        while (candidateEdges.size() == 0) {
            // expand envelope -- assumes many close searches and occasional far ones
            // scale the latitude degrees so that longitude is equivalent
            envelope.expandBy(envelopeGrowthAmount / xscale, envelopeGrowthAmount);
            radius += envelopeGrowthAmount;
            if (radius > MAX_DISTANCE_FROM_STREET_DEGREES) {
                return candidateEdges; // empty list
            }

            Iterable<Edge> nearbyEdges = getEdgesForEnvelope(envelope);

            // oh. This is part of the problem: we're not linking to one-way
            // streets, even though that is a perfectly reasonable thing to do.
            // we need to handle that using bundles.
            for (Edge e : nearbyEdges) {
                // Ignore invalid edges.
                if (e == null || e.getFromVertex() == null || !(e instanceof StreetEdge)) {
                    continue;
                }
                StreetEdge se = (StreetEdge)e;

                // Ignore those edges we can't traverse. canBeTraversed checks internally if 
                // walking a bike is possible on this StreetEdge.
                if (!reqs.canBeTraversed(se)) {
                    continue;
                }

                // Compute preference value
                double preference = 1;
                if (preferredEdges != null && preferredEdges.contains(e)) {
                    preference = 3.0;
                }

                TraverseModeSet modes = reqs.modes;
                CandidateEdge ce = new CandidateEdge(se, location, preference, modes);

                // Even if an edge is outside the query envelope, bounding boxes can
                // still intersect. In this case, distance to the edge is greater
                // than the query envelope size.
                // The distance is represented in latitude degrees regardless of direction because the
                // coordinate system is scaled
                if (ce.distance < radius) {
                    candidateEdges.add(ce);
                }
            }
        }

        Collection<CandidateEdgeBundle> bundles = candidateEdges.binByDistanceAndAngle();
        // initially set best bundle to the closest bundle
        CandidateEdgeBundle best = null;
        for (CandidateEdgeBundle bundle : bundles) {
            if (best == null || bundle.best.score < best.best.score) {
                if (possibleTransitLinksOnly) {
                    // assuming all platforms are tagged when they are not car streets... #1077 
                    if (!(bundle.allowsCars() || bundle.isPlatform()))
                        continue;
                }
                best = bundle;
            }
        }

        return best;
    }

    @Override
    public CandidateEdgeBundle getClosestEdges(GenericLocation location, TraversalRequirements reqs) {
        return getClosestEdges(location, reqs, null, null, false);
    }

    /**
     * Find edges closest to the given location.
     * 
     * TODO(flamholz): consider deleting.
     * 
     * @param location Point to get edges near
     * @param request RoutingRequest that must be able to traverse the edge (all edges if null)
     * @param extraEdges Any edges not in the graph that might be included (allows trips within one block)
     * @param preferredEdges Any edges to prefer in the search
     * @param possibleTransitLinksOnly only return edges traversable by cars or are platforms
     * @return
     */
    private CandidateEdgeBundle getClosestEdges(GenericLocation location, RoutingRequest request,
            List<Edge> extraEdges, Collection<Edge> preferredEdges, boolean possibleTransitLinksOnly) {
        // NOTE(flamholz): if request is null, will initialize TraversalRequirements
        // that accept all modes of travel.
        TraversalRequirements reqs = new TraversalRequirements(request);

        return getClosestEdges(location, reqs, extraEdges, preferredEdges, possibleTransitLinksOnly);
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
    
    @Override
    public Vertex getVertexForLocation(GenericLocation loc, RoutingRequest options,
                                       boolean endVertex) {
        Coordinate c = loc.getCoordinate();
        if (c != null) {
            return getClosestVertex(loc, options, endVertex);
        }

        // No Coordinate available.
        String place = loc.place;
        if (place == null) {
            return null;
        }

        // did not match lat/lon, interpret place as a vertex label.
        // this should probably only be used in tests,
        // though it does allow routing from stop to stop.
        return graph.getVertex(place);
    }

    @Override
    public String toString() {
        return getClass().getName() + " -- edgeTree: " + edgeTree.toString() + " -- verticesTree: " + verticesTree.toString();
    }

    @Override
    public Coordinate getClosestPointOnStreet(Coordinate c) {
        CandidateEdge e = getClosestEdges(new GenericLocation(c), new TraversalRequirements()).best;
        return e != null ? e.nearestPointOnEdge : null;
    }

    @Override
    public Vertex getSampleVertexAt(Coordinate coordinate, boolean dest) {
        SampleFactory sfac = graph.getSampleFactory();

        Sample s = sfac.getSample(coordinate.x, coordinate.y);

        if (s == null)
            return null;

        // create temp vertex
        SampleVertex v = new SampleVertex(graph, coordinate);

        // create edges
        if (dest) {
            if (s.v0 != null)
                new SampleEdge(s.v0, v, s.d0);

            if (s.v1 != null)
                new SampleEdge(s.v1, v, s.d1);
        }
        else {
            if (s.v0 != null)
                new SampleEdge(v, s.v0, s.d0);

            if (s.v1 != null)
                new SampleEdge(v, s.v1, s.d1);
        }

        return v;
    }
}
