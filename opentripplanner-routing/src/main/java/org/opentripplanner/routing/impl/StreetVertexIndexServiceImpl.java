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

import static org.opentripplanner.common.IterableLibrary.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods used during network linking and trip planning.
 * 
 * Creates a StreetLocation representing a location on a street that's not at an intersection, based on input latitude and longitude. Instantiating
 * this class is expensive, because it creates a spatial index of all of the intersections in the graph.
 */
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService {

    // Members are protected so that custom subclasses can access them.

    protected Graph graph;

    /**
     * Contains only instances of {@link StreetEdge}
     */
    protected SpatialIndex edgeTree;

    protected STRtree transitStopTree;

    protected STRtree intersectionTree;

    @Getter
    @Setter
    protected DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    // private static final double SEARCH_RADIUS_M = 100; // meters
    // private static final double SEARCH_RADIUS_DEG = DistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    /* all distance constants here are plate-carée Euclidean, 0.001 ~= 100m at equator */

    // Edges will only be found if they are closer than this distance
    public static final double MAX_DISTANCE_FROM_STREET = 0.01000;

    // Maximum difference in distance for two geometries to be considered coincident
    public static final double DISTANCE_ERROR = 0.000001;

    // If a point is within MAX_CORNER_DISTANCE, it is treated as at the corner.
    // This distance is a euclidean distance in lat/lng space.
    private static final double MAX_CORNER_DISTANCE = 0.0001;

    static final Logger LOG = LoggerFactory.getLogger(StreetVertexIndexServiceImpl.class);

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graph = graph;
        setup();
    }

    public StreetVertexIndexServiceImpl(Graph graph, DistanceLibrary distanceLibrary) {
        this.graph = graph;
        this.distanceLibrary = distanceLibrary;
        setup();
    }

    public void setup_modifiable() {
        edgeTree = new Quadtree();
        postSetup();
    }

    public void setup() {
        edgeTree = new STRtree();
        postSetup();
        ((STRtree) edgeTree).build();
    }

    private void postSetup() {

        transitStopTree = new STRtree();
        intersectionTree = new STRtree();

        for (Vertex gv : graph.getVertices()) {
            Vertex v = gv;
            // We only care about StreetEdges
            for (StreetEdge e : filter(gv.getOutgoing(), StreetEdge.class)) {
                if (e.getGeometry() == null) {
                    continue;
                }
                Envelope env = e.getGeometry().getEnvelopeInternal();
                edgeTree.insert(env, e);
            }
            if (v instanceof TransitStop) {
                // only index transit stops that (a) are entrances, or (b) have no associated
                // entrances
                TransitStop ts = (TransitStop) v;
                if (!ts.isEntrance() && ts.hasEntrances()) {
                    continue;
                }
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
            if (v instanceof IntersectionVertex) {
                Envelope env = new Envelope(v.getCoordinate());
                intersectionTree.insert(env, v);
            }
        }
        transitStopTree.build();
    }

    /**
     * Get all transit stops within a given distance of a coordinate
     * 
     * @param distance in meters
     */
    @SuppressWarnings("unchecked")
    public List<Vertex> getLocalTransitStops(Coordinate c, double distance) {
        Envelope env = new Envelope(c);
        env.expandBy(SphericalDistanceLibrary.metersToDegrees(distance));
        List<Vertex> nearby = transitStopTree.query(env);
        List<Vertex> results = new ArrayList<Vertex>();
        for (Vertex v : nearby) {
            if (distanceLibrary.distance(v.getCoordinate(), c) <= distance) {
                results.add(v);
            }
        }
        return results;
    }

    /**
     * Convenience helper for when extraEdges is empty/null.
     */
    private Vertex getClosestVertex(final GenericLocation location, RoutingRequest options) {
        return getClosestVertex(location, options, null);
    }

    /**
     * Returns the closest vertex for this GenericLocation. If necessary, this vertex will be created by splitting nearby edges (non-permanently).
     * 
     * This method is the heart of the logic that searches for the start and endpoints of RideRequests. As such, it is protected so that subclasses
     * can override the search behavior.
     */
    protected Vertex getClosestVertex(final GenericLocation location, RoutingRequest options,
            List<Edge> extraEdges) {
        LOG.debug("Looking for/making a vertex near {}", location);

        // first, check for intersections very close by
        Coordinate coord = location.getCoordinate();
        StreetVertex intersection = getIntersectionAt(coord, MAX_CORNER_DISTANCE);
        String calculatedName = location.getName();

        if (intersection != null) {
            // coordinate is at a street corner or endpoint
            if (!location.hasName()) {
                LOG.debug("found intersection {}. not splitting.", intersection);
                // generate names for corners when no name was given
                Set<String> uniqueNameSet = new HashSet<String>();
                for (Edge e : intersection.getOutgoing()) {
                    if (e instanceof StreetEdge) {
                        uniqueNameSet.add(e.getName());
                    }
                }
                List<String> uniqueNames = new ArrayList<String>(uniqueNameSet);
                Locale locale;
                if (options == null) {
                    locale = new Locale("en");
                } else {
                    locale = options.getLocale();
                }
                ResourceBundle resources = ResourceBundle.getBundle("internals", locale);
                String fmt = resources.getString("corner");
                if (uniqueNames.size() > 1) {
                    calculatedName = String.format(fmt, uniqueNames.get(0), uniqueNames.get(1));
                } else if (uniqueNames.size() == 1) {
                    calculatedName = uniqueNames.get(0);
                } else {
                    calculatedName = resources.getString("unnamedStreet");
                }
            }
            StreetLocation closest = new StreetLocation(graph, "corner " + Math.random(), coord,
                    calculatedName);
            FreeEdge e = new FreeEdge(closest, intersection);
            closest.getExtra().add(e);
            e = new FreeEdge(intersection, closest);
            closest.getExtra().add(e);
            return closest;
        }

        // if no intersection vertices were found, then find the closest transit stop
        // (we can return stops here because this method is not used when street-transit linking)
        double closestStopDistance = Double.POSITIVE_INFINITY;
        Vertex closestStop = null;
        // elsewhere options=null means no restrictions, find anything.
        // here we skip examining stops, as they are really only relevant when transit is being used
        if (options != null && options.getModes().isTransit()) {
            for (Vertex v : getLocalTransitStops(coord, 1000)) {
                double d = distanceLibrary.distance(v.getCoordinate(), coord);
                if (d < closestStopDistance) {
                    closestStopDistance = d;
                    closestStop = v;
                }
            }
        }
        LOG.debug(" best stop: {} distance: {}", closestStop, closestStopDistance);

        // then find closest walkable street
        StreetLocation closestStreet = null;
        CandidateEdgeBundle bundle = getClosestEdges(location, options, extraEdges, null, false);
        CandidateEdge candidate = bundle.best;
        double closestStreetDistance = Double.POSITIVE_INFINITY;
        if (candidate != null) {
            StreetEdge bestStreet = candidate.edge;
            Coordinate nearestPoint = candidate.nearestPointOnEdge;
            closestStreetDistance = distanceLibrary.distance(coord, nearestPoint);
            LOG.debug("best street: {} dist: {}", bestStreet.toString(), closestStreetDistance);
            if (calculatedName == null || "".equals(calculatedName)) {
                calculatedName = bestStreet.getName();
            }
            String closestName = String.format("%s_%s", bestStreet.getName(), location.toString());
            closestStreet = StreetLocation.createStreetLocation(graph, closestName, calculatedName,
                    bundle.toEdgeList(), nearestPoint, coord);
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
                    closestStreet.addExtraEdgeTo(closestStop);
                }
            }
            LOG.debug("returning split street");
            return closestStreet;
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<Vertex> getVerticesForEnvelope(Envelope envelope) {
        return intersectionTree.query(envelope);
    }

    @Override
    public Collection<StreetEdge> getEdgesForEnvelope(Envelope envelope) {
        return edgeTree.query(envelope);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CandidateEdgeBundle getClosestEdges(GenericLocation location,
            TraversalRequirements reqs, List<Edge> extraEdges, Collection<Edge> preferredEdges,
            boolean possibleTransitLinksOnly) {
        Coordinate coordinate = location.getCoordinate();
        Envelope envelope = new Envelope(coordinate);

        // Collect the extra StreetEdges to consider.
        Iterable<StreetEdge> extraStreets = IterableLibrary.filter(graph.getTemporaryEdges(),
                StreetEdge.class);
        if (extraEdges != null) {
            extraStreets = Iterables.concat(IterableLibrary.filter(extraEdges, StreetEdge.class),
                    extraStreets);
        }

        double envelopeGrowthAmount = 0.001; // ~= 100 meters
        double radius = 0;
        CandidateEdgeBundle candidateEdges = new CandidateEdgeBundle();
        while (candidateEdges.size() == 0) {
            // expand envelope -- assumes many close searches and occasional far ones
            envelope.expandBy(envelopeGrowthAmount);
            radius += envelopeGrowthAmount;
            if (radius > MAX_DISTANCE_FROM_STREET) {
                return candidateEdges; // empty list
            }

            Iterable<StreetEdge> nearbyEdges = edgeTree.query(envelope);
            if (nearbyEdges != null) {
                nearbyEdges = Iterables.concat(nearbyEdges, extraStreets);
            }

            // oh. This is part of the problem: we're not linking to one-way
            // streets, even though that is a perfectly reasonable thing to do.
            // we need to handle that using bundles.
            for (StreetEdge e : nearbyEdges) {
                // Ignore invalid edges.
                if (e == null || e.getFromVertex() == null) {
                    continue;
                }

                // Ignore those edges we can't traverse. canBeTraversed checks internally if 
                // walking a bike is possible on this StreetEdge.
                if (!reqs.canBeTraversed(e)) {
                    continue;
                }

                // Compute preference value
                double preferrence = 1;
                if (preferredEdges != null && preferredEdges.contains(e)) {
                    preferrence = 3.0;
                }

                TraverseModeSet modes = reqs.getModes();
                CandidateEdge ce = new CandidateEdge(e, location, preferrence, modes);

                // Even if an edge is outside the query envelope, bounding boxes can
                // still intersect. In this case, distance to the edge is greater
                // than the query envelope size.
                if (ce.getDistance() < radius) {
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
     * @param coordinate Point to get edges near
     * @param request RoutingRequest that must be able to traverse the edge (all edges if null)
     * @param extraEdges Any edges not in the graph that might be included (allows trips within one block)
     * @param preferredEdges Any edges to prefer in the search
     * @param possibleTransitLinksOnly only return edges traversable by cars or are platforms
     * @return
     */
    protected CandidateEdgeBundle getClosestEdges(GenericLocation location, RoutingRequest request,
            List<Edge> extraEdges, Collection<Edge> preferredEdges, boolean possibleTransitLinksOnly) {
        // NOTE(flamholz): if request is null, will initialize TraversalRequirements
        // that accept all modes of travel.
        TraversalRequirements reqs = new TraversalRequirements(request);

        return getClosestEdges(location, reqs, extraEdges, preferredEdges, possibleTransitLinksOnly);
    }

    /**
     * Convenience wrapper that uses MAX_CORNER_DISTANCE.
     * 
     * @param coordinate
     * @return
     */
    public StreetVertex getIntersectionAt(Coordinate coordinate) {
        return getIntersectionAt(coordinate, MAX_CORNER_DISTANCE);
    }

    @SuppressWarnings("unchecked")
    public StreetVertex getIntersectionAt(Coordinate coordinate, double distanceError) {
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(distanceError * 2);
        List<StreetVertex> nearby = intersectionTree.query(envelope);
        StreetVertex nearest = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (StreetVertex v : nearby) {
            double distance = coordinate.distance(v.getCoordinate());
            if (distance < distanceError) {
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = v;
                }
            }
        }
        return nearest;
    }

    @Override
    /** radius is meters */
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radius) {
        Envelope envelope = new Envelope(coordinate);

        envelope.expandBy(SphericalDistanceLibrary.metersToDegrees(radius));
        List<?> stops = transitStopTree.query(envelope);
        ArrayList<TransitStop> out = new ArrayList<TransitStop>();
        for (Object o : stops) {
            TransitStop stop = (TransitStop) o;
            if (distanceLibrary.distance(stop.getCoordinate(), coordinate) < radius) {
                out.add(stop);
            }
        }
        return out;
    }

    @Override
    /** radius is meters */
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinateOne,
            Coordinate coordinateTwo) {
        Envelope envelope = new Envelope(coordinateOne, coordinateTwo);

        List<?> stops = transitStopTree.query(envelope);
        ArrayList<TransitStop> out = new ArrayList<TransitStop>();
        for (Object o : stops) {
            TransitStop stop = (TransitStop) o;
            out.add(stop);
        }
        return out;
    }

    @Override
    public Vertex getVertexForLocation(GenericLocation location, RoutingRequest options) {
        return getVertexForLocation(location, options, null);
    }

    /**
     * @param other: non-null when another vertex has already been found. When the from vertex has already been made/found, that vertex is passed in
     *        when finding/creating the to vertex. TODO: This appears to be for reusing the extra edges list -- is this still needed?
     */
    @Override
    public Vertex getVertexForLocation(GenericLocation loc, RoutingRequest options, Vertex other) {
        Coordinate c = loc.getCoordinate();
        if (c != null) {
            if (other instanceof StreetLocation) {
                return getClosestVertex(loc, options, ((StreetLocation) other).getExtra());
            } else {
                return getClosestVertex(loc, options);
            }
        }

        // No Coordinate available.
        String place = loc.getPlace();
        if (place == null) {
            return null;
        }

        // did not match lat/lon, interpret place as a vertex label.
        // this should probably only be used in tests.
        return graph.getVertex(place);
    }

}
