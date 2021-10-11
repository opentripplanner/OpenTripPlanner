package org.opentripplanner.graph_builder.linking;

import com.google.common.collect.Iterables;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import jersey.repackaged.com.google.common.collect.Lists;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.BikeParkUnlinked;
import org.opentripplanner.graph_builder.annotation.BikeRentalStationUnlinked;
import org.opentripplanner.graph_builder.annotation.StopLinkedTooFar;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.SemiPermanentPartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetBikeParkLink;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetCarRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetRentalLink;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVehicleRentalLink;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.CarRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.RentalStationVertex;
import org.opentripplanner.routing.vertextype.SemiPermanentSplitterVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class links transit stops to streets by splitting the streets (unless the stop is extremely close to the street
 * intersection).
 *
 * It is intended to eventually completely replace the existing stop linking code, which had been through so many
 * revisions and adaptations to different street and turn representations that it was very glitchy. This new code is
 * also intended to be deterministic in linking to streets, independent of the order in which the JVM decides to
 * iterate over Maps and even in the presence of points that are exactly halfway between multiple candidate linking
 * points.
 *
 * See discussion in pull request #1922, follow up issue #1934, and the original issue calling for replacement of
 * the stop linker, #1305.
 */
public class StreetSplitter {

    private static final Logger LOG = LoggerFactory.getLogger(StreetSplitter.class);

    public static final int MAX_SEARCH_RADIUS_METERS = 1000;

    private Boolean addExtraEdgesToAreas = false;

    private StreetEdgeFactory edgeFactory = new DefaultStreetEdgeFactory();

    public static final int WARNING_DISTANCE_METERS = 20;

    /**
     * DESTRUCTIVE_SPLIT means edges are split and new edges and vertices are created. This should occur when linking
     * transit stops etc. during graph building or inserting floating bicycles in a bikeshare system in realtime into
     * the graph. When these splits occur, a spatial index of all edges should be kept updated, otherwise there will
     * appear to be snapping issues as observed in https://github.com/opentripplanner/OpenTripPlanner/issues/2758.
     */
    public static final boolean DESTRUCTIVE_SPLIT = true;

    /**
     * NON_DESTRUCTIVE_SPLIT means new temporary edges and vertices are created and no edges are deleted. This is used
     * in individual requests when temporarily inserting the origin/destination into the graph. The spatial index of
     * street edges should not be updated to include these temporary edges.
     */
    public static final boolean NON_DESTRUCTIVE_SPLIT = false;

    /** if there are two ways and the distances to them differ by less than this value, we link to both of them */
    public static final double DUPLICATE_WAY_EPSILON_METERS = 0.001;

    private Graph graph;

    private HashGridSpatialIndex<Edge> idx;

    private SpatialIndex transitStopIndex;

    private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    // used to keep track of splitters associated with graphs to make sure that
    public static final HashSet<Graph> graphsWithSplitters = new HashSet<>();

    /**
     * Construct a new StreetSplitter.
     *
     * IMPORTANT NOTE: Only one StreetSplitter should be active on a graph at any given time. This is because any
     * newly-created split edges in this street splitter won't show up in the index of the other street splitters
     * (and potentially StreetVertexIndexServices).
     *
     * @param hashGridSpatialIndex If not null this index is used instead of creating new one
     * @param transitStopIndex Index of all transitStops which is generated in {@link org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl}
     */
    public StreetSplitter(
        Graph graph,
        HashGridSpatialIndex<Edge> hashGridSpatialIndex,
        SpatialIndex transitStopIndex
    ) {
        this.graph = graph;
        if (graphsWithSplitters.contains(graph)) {
            LOG.warn("Multiple StreetSplitters detected on the same graph! Make sure only the most recently created one is used going forward!");
        }
        LOG.info("New StreetSpiltter created successfully!");
        graphsWithSplitters.add(graph);
        this.transitStopIndex = transitStopIndex;

        //We build a spatial index if it isn't provided
        if (hashGridSpatialIndex == null) {
            // build a nice private spatial index, since we're adding and removing edges
            // place a lock while doing this to ensure thread-safe writes to the index
            synchronized (this) {
                idx = new HashGridSpatialIndex<>();
                for (StreetEdge se : Iterables.filter(graph.getEdges(), StreetEdge.class)) {
                    idx.insert(se.getGeometry(), se);
                }
            }
        } else {
            idx = hashGridSpatialIndex;
        }

    }

    /**
     * Construct a new StreetSplitter. Be aware that only one StreetSplitter should be
     * active on a graph at any given time.
     *
     * StreetSplitter generates index on graph and splits destructively (used in transit splitter)
     * @param graph
     */
    public StreetSplitter(Graph graph) {
        this(graph, null, null);
    }

    /**
     * Link all relevant vertices to the street network.
     * This is to be called only during graph building in order to destructively split edges in order to create
     * permanent edges.
     */
    public void linkAllStationsToGraph() {
        int linksMade = 0;
        LOG.info("Linking the street network to transit stops, static bike rentals and static bike parks.");
        for (Vertex v : graph.getVertices()) {
            if (v instanceof TransitStop || v instanceof BikeRentalStationVertex || v instanceof BikeParkVertex) {
                boolean alreadyLinked = v.getOutgoing().stream().anyMatch(e -> e instanceof StreetTransitLink);
                if (alreadyLinked) continue;
                if (!linkToClosestWalkableEdge(v, DESTRUCTIVE_SPLIT, false)) {
                    if (v instanceof TransitStop)
                        LOG.warn(graph.addBuilderAnnotation(new StopUnlinked((TransitStop) v)));
                    else if (v instanceof BikeRentalStationVertex)
                        LOG.warn(graph.addBuilderAnnotation(new BikeRentalStationUnlinked((BikeRentalStationVertex) v)));
                    else if (v instanceof BikeParkVertex)
                        LOG.warn(graph.addBuilderAnnotation(new BikeParkUnlinked((BikeParkVertex) v)));
                } else {
                    linksMade++;
                    if (linksMade % 5000 == 0) {
                        LOG.info("Made {} links from street network to transit stops, static bike rentals or static bike parks.", linksMade);
                    }
                }
            }
        }
        LOG.info("{} total links made.", linksMade);
    }

    /**
     * Link this vertex into the graph to the closest walkable edge.
     *
     * @param vertex The vertex to be linked.
     * @param destructiveSplitting If true splitting is permanent (Used when linking transit stops etc.) when
     *                             false the original edge is kept in the graph after creating split edges.
     * @param createSemiPermanentEdges If true, semi-permanent edges and a semi-permanent splitter vertex is created.
     *                                 These vertices and edges are intended to be kept in the graph and linked to
     *                                 during subsequent requests, but might be removed at a later time. For example a
     *                                 floating bike rental may need to be removed from the graph once the data is
     *                                 updated to show that the floating bike rental no longer exists or if its
     *                                 location has significantly changed.
     */
    public boolean linkToClosestWalkableEdge(
        Vertex vertex,
        final boolean destructiveSplitting,
        boolean createSemiPermanentEdges
    ) {
        return linkToGraph(vertex, TraverseMode.WALK, null, destructiveSplitting, createSemiPermanentEdges);
    }

    /**
     * Link a vertex into the graph to the closest edge that allows the traversal of the specified mode.
     */
    public boolean linkToGraph(
        Vertex vertex,
        TraverseMode traverseMode,
        RoutingRequest options,
        final boolean destructiveSplitting,
        boolean createSemiPermanentEdges
    ) {
        final TraverseModeSet traverseModeSet = new TraverseModeSet(traverseMode);
        // also set the walk mode to true for bicycling or micromobility. This is done because it is assumed that the
        // bike or micromobility vehicle is able to be walked along certain edges that explicitly forbid bicycling or
        // micromobility travel.
        if (traverseMode == TraverseMode.BICYCLE || traverseMode == TraverseMode.MICROMOBILITY) {
            traverseModeSet.setWalk(true);
        }
        return linkToGraph(vertex, traverseModeSet, options, destructiveSplitting, createSemiPermanentEdges);
    }

    /**
     *  Link this vertex into the graph (ie, connect the vertex to other vertices in the graph by creating one or more
     *  new edges/vertexes which are then connected to the other vertices/edges of the graph.
     *
     * @param vertex The vertex to be linked.
     * @param traverseModeSet The traverse modes.
     * @param options The routing options.
     * @param destructiveSplitting If true, splitting is permanent (Used when linking transit stops etc). When
     *                             false the original edge is kept in the graph after creating split edges.
     * @param createSemiPermanentEdges If true, semi-permanent edges and a semi-permanent splitter vertex is created.
     *                                 These vertices and edges are intended to be kept in the graph and linked to
     *                                 during subsequent requests, but might be removed at a later time. For example a
     *                                 floating bike rental may need to be removed from the graph once the data is
     *                                 updated to show that the floating bike rental no longer exists or if its
     *                                 location has significantly changed.
     */
    public boolean linkToGraph(
        Vertex vertex,
        TraverseModeSet traverseModeSet,
        RoutingRequest options,
        final boolean destructiveSplitting,
        boolean createSemiPermanentEdges
    ) {
        // find nearby street edges
        // TODO: we used to use an expanding-envelope search, which is more efficient in
        // dense areas. but first let's see how inefficient this is. I suspect it's not too
        // bad and the gains in simplicity are considerable.
        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(MAX_SEARCH_RADIUS_METERS);

        Envelope env = new Envelope(vertex.getCoordinate());

        // Perform a simple local equirectangular projection, so distances are expressed in degrees latitude.
        final double xscale = Math.cos(vertex.getLat() * Math.PI / 180);

        // Expand more in the longitude direction than the latitude direction to account for converging meridians.
        env.expandBy(radiusDeg / xscale, radiusDeg);

        final double DUPLICATE_WAY_EPSILON_DEGREES = SphericalDistanceLibrary.metersToDegrees(DUPLICATE_WAY_EPSILON_METERS);

        // We sort the list of candidate edges by distance to the stop
        // This should remove any issues with things coming out of the spatial index in different orders
        // Then we link to everything that is within DUPLICATE_WAY_EPSILON_METERS of the best distance
        // so that we capture back edges and duplicate ways.
        List<StreetEdge> candidateEdges = idx.query(env).stream()
            .filter(
                streetEdge -> streetEdge instanceof StreetEdge &&
                    // Do not find SemiPermanentPartialStreetEdges if creating new SemiPermanentPartialStreetEdges.
                    // Use only permanent StreetEdges instead
                    (!createSemiPermanentEdges || !(streetEdge instanceof SemiPermanentPartialStreetEdge))
            )
            .map(edge -> (StreetEdge) edge)
            // note: not filtering by radius here as distance calculation is expensive
            // we do that below.
            .filter(edge -> edge.canTraverse(traverseModeSet) &&
                // only link to edges still in the graph.
                edge.getToVertex().getIncoming().contains(edge))
            .collect(Collectors.toList());

        // Make a map of distances to all edges.
        final TIntDoubleMap distances = new TIntDoubleHashMap();
        for (StreetEdge e : candidateEdges) {
            distances.put(e.getId(), distance(vertex, e, xscale));
        }

        // Sort the list.
        Collections.sort(candidateEdges, (o1, o2) -> {
            double diff = distances.get(o1.getId()) - distances.get(o2.getId());
            // A Comparator must return an integer but our distances are doubles.
            if (diff < 0) return -1;
            if (diff > 0) return 1;
            return 0;
        });

        // find the closest candidate edges
        if (candidateEdges.isEmpty() || distances.get(candidateEdges.get(0).getId()) > radiusDeg) {
            // We only link to stops if we are searching for origin/destination and for that we need transitStopIndex.
            if (destructiveSplitting || transitStopIndex == null) {
                return false;
            }
            LOG.debug("No street edge was found for {}", vertex);
            // We search for closest stops (since this is only used in origin/destination linking if no edges were found)
            // in the same way the closest edges are found.
            List<TransitStop> candidateStops = new ArrayList<>();
            transitStopIndex.query(env).forEach(candidateStop ->
                candidateStops.add((TransitStop) candidateStop)
            );

            final TIntDoubleMap stopDistances = new TIntDoubleHashMap();

            for (TransitStop t : candidateStops) {
                stopDistances.put(t.getIndex(), distance(vertex, t, xscale));
            }

            Collections.sort(candidateStops, (o1, o2) -> {
                    double diff = stopDistances.get(o1.getIndex()) - stopDistances.get(o2.getIndex());
                    if (diff < 0) return -1;
                    if (diff > 0) return 1;
                    return 0;
            });
            if (candidateStops.isEmpty() || stopDistances.get(candidateStops.get(0).getIndex()) > radiusDeg) {
                LOG.debug("Stops aren't close either!");
                return false;
            } else {
                List<TransitStop> bestStops = Lists.newArrayList();
                // Add stops until there is a break of epsilon meters.
                // we do this to enforce determinism. if there are a lot of stops that are all extremely close to each other,
                // we want to be sure that we deterministically link to the same ones every time. Any hard cutoff means things can
                // fall just inside or beyond the cutoff depending on floating-point operations.
                int i = 0;
                do {
                    bestStops.add(candidateStops.get(i++));
                } while (i < candidateStops.size() &&
                    stopDistances.get(candidateStops.get(i).getIndex()) - stopDistances
                        .get(candidateStops.get(i - 1).getIndex()) < DUPLICATE_WAY_EPSILON_DEGREES);

                for (TransitStop stop: bestStops) {
                    LOG.debug("Linking vertex to stop: {}", stop.getName());
                    makeTemporaryEdges((TemporaryStreetLocation)vertex, stop, destructiveSplitting);
                }
                return true;
            }
        } else {

            // find the best edges
            List<StreetEdge> bestEdges = Lists.newArrayList();

            // add edges until there is a break of epsilon meters.
            // we do this to enforce determinism. if there are a lot of edges that are all extremely close to each other,
            // we want to be sure that we deterministically link to the same ones every time. Any hard cutoff means things can
            // fall just inside or beyond the cutoff depending on floating-point operations.
            int i = 0;
            do {
                bestEdges.add(candidateEdges.get(i++));
            } while (i < candidateEdges.size() &&
                distances.get(candidateEdges.get(i).getId()) - distances
                    .get(candidateEdges.get(i - 1).getId()) < DUPLICATE_WAY_EPSILON_DEGREES);

            for (StreetEdge edge : bestEdges) {
                linkToEdge(vertex, edge, xscale, options, destructiveSplitting, createSemiPermanentEdges);
            }

            // Warn if a linkage was made, but the linkage was suspiciously long.
            if (vertex instanceof TransitStop) {
                double distanceDegreesLatitude = distances.get(candidateEdges.get(0).getId());
                int distanceMeters = (int)SphericalDistanceLibrary.degreesLatitudeToMeters(distanceDegreesLatitude);
                if (distanceMeters > WARNING_DISTANCE_METERS) {
                    // Registering an annotation but not logging because tests produce thousands of these warnings.
                    graph.addBuilderAnnotation(new StopLinkedTooFar((TransitStop)vertex, distanceMeters));
                }
            }

            return true;
        }
    }

    /**
     * Link to all vertices in area/platform
     */
    private void linkTransitToAreaVertices(Vertex splitterVertex, AreaEdgeList area) {
        List<Vertex> vertices = new ArrayList<>();

        for (AreaEdge areaEdge : area.getEdges()) {
            if (!vertices.contains(areaEdge.getToVertex())) vertices.add(areaEdge.getToVertex());
            if (!vertices.contains(areaEdge.getFromVertex())) vertices.add(areaEdge.getFromVertex());
        }

        for (Vertex vertex : vertices) {
            if (vertex instanceof StreetVertex && !vertex.equals(splitterVertex)) {
                LineString line = geometryFactory.createLineString(new Coordinate[] { splitterVertex.getCoordinate(), vertex.getCoordinate()});
                double length = SphericalDistanceLibrary.distance(splitterVertex.getCoordinate(),
                        vertex.getCoordinate());
                I18NString name = new LocalizedString("", new OSMWithTags());

                edgeFactory.createAreaEdge((IntersectionVertex) splitterVertex, (IntersectionVertex) vertex, line, name, length,StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area);
                edgeFactory.createAreaEdge((IntersectionVertex) vertex, (IntersectionVertex) splitterVertex, line, name, length,StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area);
            }
        }
    }

    /**
     * Split the edge and link in the transit stop.
     * @param vertex An object of Vertex to be linked to an edge.
     * @param edge An object of StreetEdge to be linked to.
     * @param xscale The longitude scale factor in Equirectangular projection.
     * @param options An object of RoutingRequest
     * @param destructiveSplitting If true splitting is permanent (Used when linking transit stops etc.) when
     *                             false the original edge is kept in the graph after creating split edges.
     * @param createSemiPermanentEdges If true, semi-permanent edges and a semi-permanent splitter vertex is created.
     *                                 These vertices and edges are intended to be kept in the graph and linked to
     *                                 during subsequent requests, but might be removed at a later time. For example a
     *                                 floating bike rental may need to be removed from the graph once the data is
     *                                 updated to show that the floating bike rental no longer exists or if its
     *                                 location has significantly changed.
     */
    private void linkToEdge(
        Vertex vertex,
        StreetEdge edge,
        double xscale,
        RoutingRequest options,
        final boolean destructiveSplitting,
        boolean createSemiPermanentEdges
    ) {
        // TODO: we've already built this line string, we should save it
        LineString orig = edge.getGeometry();
        LineString transformed = equirectangularProject(orig, xscale);
        LocationIndexedLine il = new LocationIndexedLine(transformed);
        LinearLocation ll = il.project(new Coordinate(vertex.getLon() * xscale, vertex.getLat()));

        // if we're very close to one end of the line or the other, or endwise, don't bother to split,
        // cut to the chase and link directly
        // We use a really tiny epsilon here because we only want points that actually snap to exactly the same location on the
        // street to use the same vertices. Otherwise the order the stops are loaded in will affect where they are snapped.
        if (ll.getSegmentIndex() == 0 && ll.getSegmentFraction() < 1e-8) {
            makeLinkEdges(vertex, (StreetVertex) edge.getFromVertex(), destructiveSplitting);
        }
        // -1 converts from count to index. Because of the fencepost problem, npoints - 1 is the "segment"
        // past the last point
        else if (ll.getSegmentIndex() == orig.getNumPoints() - 1) {
            makeLinkEdges(vertex, (StreetVertex) edge.getToVertex(), destructiveSplitting);
        }

        // nPoints - 2: -1 to correct for index vs count, -1 to account for fencepost problem
        else if (ll.getSegmentIndex() == orig.getNumPoints() - 2 && ll.getSegmentFraction() > 1 - 1e-8) {
            makeLinkEdges(vertex, (StreetVertex) edge.getToVertex(), destructiveSplitting);
        }

        else {

            TemporaryVertex temporaryVertex = null;
            boolean endVertex = false;
            if (vertex instanceof TemporaryVertex) {
                temporaryVertex = (TemporaryVertex) vertex;
                endVertex = temporaryVertex.isEndVertex();

            }
            //This throws runtime TrivialPathException if same edge is split in origin and destination link
            //It is only used in origin/destination linking since otherwise options is null
            if (options != null) {
                options.canSplitEdge(edge);
            }
            // split the edge, get the split vertex
            SplitterVertex v0 = split(
                edge,
                ll,
                temporaryVertex != null,
                endVertex,
                destructiveSplitting,
                createSemiPermanentEdges
            );

            // if SemiPermanentEdges were created, update some of their characteristics to ensure that they are not
            // traversed if there is no reason to do so
            if (createSemiPermanentEdges) {
                Set<Edge> newlySplitEdges = new HashSet<>();
                newlySplitEdges.addAll(v0.getOutgoing());
                newlySplitEdges.addAll(v0.getIncoming());

                if (vertex instanceof BikeRentalStationVertex) {
                    for (Edge splitEdge : newlySplitEdges) {
                        ((SemiPermanentPartialStreetEdge) splitEdge).setBikeRentalOptionRequired();
                    }
                } else if (vertex instanceof CarRentalStationVertex) {
                    for (Edge splitEdge : newlySplitEdges) {
                        ((SemiPermanentPartialStreetEdge) splitEdge).setCarRentalOptionRequired();
                    }
                } else if (vertex instanceof VehicleRentalStationVertex) {
                    for (Edge splitEdge : newlySplitEdges) {
                        ((SemiPermanentPartialStreetEdge) splitEdge).setVehicleRentalOptionRequired();
                    }
                }
            }

            makeLinkEdges(vertex, v0, destructiveSplitting);

            // If splitter vertex is part of area; link splittervertex to all other vertexes in area, this creates
            // edges that were missed by WalkableAreaBuilder
            if (edge instanceof AreaEdge && vertex instanceof TransitStop && this.addExtraEdgesToAreas) {
                linkTransitToAreaVertices(v0, ((AreaEdge) edge).getArea());
            }
        }
    }


    /**
     * Split the street edge at the given fraction
     *
     * @param edge to be split
     * @param ll fraction at which to split the edge
     * @param temporarySplit if true this is temporary split at origin/destinations search and only temporary edges
     *                       vertices are created
     * @param endVertex if this is temporary edge this is true if this is end vertex otherwise it doesn't matter
     * @param destructiveSplitting If true splitting is permanent (Used when linking transit stops etc.) when
     *                             false the original edge is kept in the graph after creating split edges.
     * @param createSemiPermanentEdges If true, semi-permanent edges and a semi-permanent splitter vertex is created.
     *                                 These vertices and edges are intended to be kept in the graph and linked to
     *                                 during subsequent requests, but might be removed at a later time. For example a
     *                                 floating bike rental may need to be removed from the graph once the data is
     *                                 updated to show that the floating bike rental no longer exists or if its
     *                                 location has significantly changed.
     * @return Splitter vertex with added new edges
     */
    private SplitterVertex split (
        StreetEdge edge,
        LinearLocation ll,
        boolean temporarySplit,
        boolean endVertex,
        final boolean destructiveSplitting,
        boolean createSemiPermanentEdges
    ) {

        LineString geometry = edge.getGeometry();

        // create the geometries
        Coordinate splitPoint = ll.getCoordinate(geometry);

        // every edge can be split exactly once, so this is a valid label
        SplitterVertex v;
        StringBuilder splitLabel = new StringBuilder();
        splitLabel.append("split from ");
        splitLabel.append(edge.getId());
        splitLabel.append(" #");
        int splintNum = 1;
        splitLabel.append(splintNum);
        while (graph.containsVertexLabel(splitLabel.toString())) {
            splintNum++;
            splitLabel.replace(splitLabel.lastIndexOf("#") + 1, Integer.MAX_VALUE, String.valueOf(splintNum));
        }
        String vertexLabel = splitLabel.toString();
        if (createSemiPermanentEdges) {
            v = new SemiPermanentSplitterVertex(graph, vertexLabel, splitPoint.x, splitPoint.y, edge);
        } else if (temporarySplit) {
            v = new TemporarySplitterVertex(vertexLabel, splitPoint.x, splitPoint.y, edge, endVertex);
        } else {
            v = new SplitterVertex(graph, vertexLabel, splitPoint.x, splitPoint.y, edge);
        }

        // Split the 'edge' at 'v' in 2 new edges and connect these 2 edges to the
        // existing vertices
        P2<StreetEdge> edges = edge.split(v, destructiveSplitting, createSemiPermanentEdges);

        if (destructiveSplitting || createSemiPermanentEdges) {
            // update indices of new edges
            synchronized (this) {
                // Note: Write operations are not synchronized in HashGridSpatialIndex, hence the lock.
                idx.insert(edges.first.getGeometry(), edges.first);
                idx.insert(edges.second.getGeometry(), edges.second);

                // if destructively splitting, remove the edge from the index
                if (destructiveSplitting) {
                    idx.remove(edge.getGeometry().getEnvelopeInternal(), edge);
                }
            }
        }

        if (destructiveSplitting) {
            // remove original edge from the graph if split was destructive
            edge.getToVertex().removeIncoming(edge);
            edge.getFromVertex().removeOutgoing(edge);
        }

        return v;
    }

    /** Make the appropriate type of link edges from a vertex */
    private void makeLinkEdges(Vertex from, StreetVertex to, final boolean destructiveSplitting) {
        if (from instanceof TemporaryStreetLocation) {
            makeTemporaryEdges((TemporaryStreetLocation) from, to, destructiveSplitting);
        } else if (from instanceof TransitStop) {
            makeTransitLinkEdges((TransitStop) from, to, destructiveSplitting);
        } else if (from instanceof BikeRentalStationVertex) {
            makeBikeRentalLinkEdges((BikeRentalStationVertex) from, to);
        } else if (from instanceof BikeParkVertex) {
            makeBikeParkEdges((BikeParkVertex) from, to, destructiveSplitting);
        } else if (from instanceof CarRentalStationVertex) {
            makeCarRentalLinkEdges((CarRentalStationVertex) from, to);
        } else if (from instanceof VehicleRentalStationVertex) {
            makeVehicleRentalLinkEdges((VehicleRentalStationVertex) from, to);
        }
    }

    /**
     * Make temporary edges to origin/destination vertex in origin/destination search
     */
    private void makeTemporaryEdges(TemporaryStreetLocation from, Vertex to, final boolean destructiveSplitting) {
        if (destructiveSplitting) {
            throw new RuntimeException("Destructive splitting is used on temporary edges. Something is wrong!");
        }
        if (to instanceof TemporarySplitterVertex) {
            from.setWheelchairAccessible(((TemporarySplitterVertex) to).isWheelchairAccessible());
        }
        // there is no need to create a TemporaryFreeEdge when the to vertex
        if (from.isEndVertex()) {
            // Don't create a TemporaryFreeEdge if the to vertex doesn't have any incoming edges, it's a dead end that
            // was probably caused by avoiding the creation of identical TemporaryPartialStreetEdges in cases where a
            // certain SemiPermanentPartialStreetEdge was split.
            if (to.getDegreeIn() == 0) return;
            LOG.debug("Linking end vertex to {} -> {}", to, from);
            new TemporaryFreeEdge(to, from);
        } else {
            // Don't create a TemporaryFreeEdge if the to vertex doesn't have any outgoing edges, it's a dead end that
            // was probably caused by avoiding the creation of identical TemporaryPartialStreetEdges in cases where a
            // certain SemiPermanentPartialStreetEdge was split.
            if (to.getDegreeOut() == 0) return;
            LOG.debug("Linking start vertex to {} -> {}", from, to);
            new TemporaryFreeEdge(from, to);
        }
    }

    /** Make bike park edges */
    private void makeBikeParkEdges(BikeParkVertex from, StreetVertex to, final boolean destructiveSplitting) {
        if (!destructiveSplitting) {
            throw new RuntimeException("Bike park edges are created with non destructive splitting!");
        }
        for (StreetBikeParkLink sbpl : Iterables.filter(from.getOutgoing(), StreetBikeParkLink.class)) {
            if (sbpl.getToVertex() == to)
                return;
        }

        new StreetBikeParkLink(from, to);
        new StreetBikeParkLink(to, from);
    }

    /**
     * Make street transit link edges, unless they already exist.
     */
    private void makeTransitLinkEdges (TransitStop tstop, StreetVertex v, final boolean destructiveSplitting) {
        if (!destructiveSplitting) {
            throw new RuntimeException("Transitedges are created with non destructive splitting!");
        }
        // ensure that the requisite edges do not already exist
        // this can happen if we link to duplicate ways that have the same start/end vertices or if
        // certain transit stops were already linked to the network in the TransitToTaggedStopsModule.
        for (StreetTransitLink e : Iterables.filter(tstop.getOutgoing(), StreetTransitLink.class)) {
            if (e.getToVertex() == v)
                return;
        }

        new StreetTransitLink(tstop, v);
        new StreetTransitLink(v, tstop);
    }

    /** Make link edges for bike rental */
    private void makeBikeRentalLinkEdges (BikeRentalStationVertex from, StreetVertex to) {
        for (StreetBikeRentalLink sbrl : Iterables.filter(from.getOutgoing(), StreetBikeRentalLink.class)) {
            if (sbrl.getToVertex() == to)
                return;
        }

        new StreetBikeRentalLink(from, to);
        new StreetBikeRentalLink(to, from);
    }

    /** Make link edges for car rental */
    private void makeCarRentalLinkEdges (CarRentalStationVertex from, StreetVertex to) {
        for (StreetCarRentalLink scrl : Iterables.filter(from.getOutgoing(), StreetCarRentalLink.class)) {
            if (scrl.getToVertex() == to)
                return;
        }

        new StreetCarRentalLink(from, to);
        new StreetCarRentalLink(to, from);
    }

    /** Make link edges for vehicle rental */
    private void makeVehicleRentalLinkEdges (VehicleRentalStationVertex from, StreetVertex to) {
        for (StreetVehicleRentalLink scrl : Iterables.filter(from.getOutgoing(), StreetVehicleRentalLink.class)) {
            if (scrl.getToVertex() == to)
                return;
        }

        new StreetVehicleRentalLink(from, to);
        new StreetVehicleRentalLink(to, from);
    }

    /** projected distance from stop to edge, in latitude degrees */
    private static double distance (Vertex tstop, StreetEdge edge, double xscale) {
        // Despite the fact that we want to use a fast somewhat inaccurate projection, still use JTS library tools
        // for the actual distance calculations.
        LineString transformed = equirectangularProject(edge.getGeometry(), xscale);
        return transformed.distance(geometryFactory.createPoint(new Coordinate(tstop.getLon() * xscale, tstop.getLat())));
    }

    /** projected distance from stop to another stop, in latitude degrees */
    private static double distance (Vertex tstop, Vertex tstop2, double xscale) {
        // use JTS internal tools wherever possible
        return new Coordinate(tstop.getLon() * xscale, tstop.getLat()).distance(new Coordinate(tstop2.getLon() * xscale, tstop2.getLat()));
    }

    /** project this linestring to an equirectangular projection */
    private static LineString equirectangularProject(LineString geometry, double xscale) {
        Coordinate[] coords = new Coordinate[geometry.getNumPoints()];

        for (int i = 0; i < coords.length; i++) {
            Coordinate c = geometry.getCoordinateN(i);
            c = (Coordinate) c.clone();
            c.x *= xscale;
            coords[i] = c;
        }

        return geometryFactory.createLineString(coords);
    }

    /**
     * Used to link origin and destination points to graph non destructively.
     *
     * Split edges don't replace existing ones and only temporary edges and vertices are created.
     *
     * Will throw TrivialPathException if origin and destination Location are on the same edge
     *
     * @param location
     * @param options
     * @param endVertex true if this is destination vertex
     * @return
     */
    public Vertex linkOriginDestination(GenericLocation location, RoutingRequest options, boolean endVertex) {

        if (endVertex) {
            LOG.debug("Finding end vertex for {}", location);
        } else {
            LOG.debug("Finding start vertex for {}", location);
        }
        Coordinate coord = location.getCoordinate();
        //TODO: add nice name
        String name;

        if (location.name == null || location.name.isEmpty()) {
            if (endVertex) {
                name = "Destination";
            } else {
                name = "Origin";
            }
        } else {
            name = location.name;
        }
        TemporaryStreetLocation closest = new TemporaryStreetLocation(UUID.randomUUID().toString(),
            coord, new NonLocalizedString(name), endVertex);

        TraverseMode nonTransitMode = TraverseMode.WALK;
        boolean linkWithSingleMode = true;

        //It can be null in tests
        if (options != null) {
            TraverseModeSet modes = options.modes;
            if (modes.getCar())
                // for park and ride we will start in car mode and walk to the end vertex
                if (endVertex && (options.parkAndRide || options.kissAndRide)) {
                    nonTransitMode = TraverseMode.WALK;
                }
                // tnc routing and car rental it would be possible to walk or drive
                else if (options.useTransportationNetworkCompany || options.allowCarRental) {
                    linkWithSingleMode = false;
                    if(!linkToGraph(
                        closest,
                        new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR),
                        options,
                        NON_DESTRUCTIVE_SPLIT,
                        false
                    )) {
                        LOG.warn("Couldn't link {}", location);
                    }
                } else {
                    nonTransitMode = TraverseMode.CAR;
                }
            else if (modes.getWalk()) nonTransitMode = TraverseMode.WALK;
            else if (modes.getBicycle()) nonTransitMode = TraverseMode.BICYCLE;
            else if (modes.getMicromobility()) nonTransitMode = TraverseMode.MICROMOBILITY;
        }

        if(linkWithSingleMode && !linkToGraph(closest, nonTransitMode, options, NON_DESTRUCTIVE_SPLIT, false)) {
            LOG.warn("Couldn't link {}", location);
        }
        return closest;

    }

    public Boolean getAddExtraEdgesToAreas() {
        return addExtraEdgesToAreas;
    }

    public void setAddExtraEdgesToAreas(Boolean addExtraEdgesToAreas) {
        this.addExtraEdgesToAreas = addExtraEdgesToAreas;
    }

    /**
     * Helper method for removing all vertices and edges associated with a SemiPermanentSplitterVertex. The purpose of
     * a SemiPermanentSplitterVertex is to model access to another vertex (typically a bike/car/vehicle rental station
     * vertex) from an existing set of street vertices. This method removes all edges immediately connected to the given
     * vertex, but also searches outward for Street{Bike/Car/Vehicle}RentalLink edges and associated connected vertices
     * and edges and removes those as well from the graph.
     */
    public void removeRentalStationVertexAndAssociatedSemiPermanentVerticesAndEdges (RentalStationVertex rentalStationVertex) {
        // before removing the bikeRentalStationVertex, first find and remove all associated
        // SemiPermanentSplitterVertices
        for (Edge rentalStationVertexEdge : rentalStationVertex.getOutgoing()) {
            if (rentalStationVertexEdge instanceof StreetRentalLink) {
                StreetRentalLink toStreetLink = (StreetRentalLink) rentalStationVertexEdge;
                StreetVertex streetVertex = (StreetVertex) toStreetLink.getToVertex();
                // Sometimes a StreetRentalLink can link directly to a permanent StreetVertex, so make sure only
                // SemiPermanentSplitterVertices are removed.
                if (streetVertex != null && streetVertex instanceof SemiPermanentSplitterVertex) {
                    // remove all associated SemiPermanentPartialStreetEdges from the splitter index
                    for (Edge edge : streetVertex.getOutgoing()) {
                        if (edge instanceof SemiPermanentPartialStreetEdge) {
                            removeEdgeFromIdx((StreetEdge) edge);
                        }
                    }
                    for (Edge edge : streetVertex.getIncoming()) {
                        if (edge instanceof SemiPermanentPartialStreetEdge) {
                            removeEdgeFromIdx((StreetEdge) edge);
                        }
                    }
                    if (graph.containsVertex(streetVertex)) {
                        // remove the SemiPermanentSplitterVertex and associated SemiPermanentPartialStreetEdges
                        graph.removeVertexAndEdges(streetVertex);
                    }
                }
            }
        }

        // remove the bikeRentalStationVertex from the graph if it's in there (why wouldn't it be?)
        if (graph.containsVertex(rentalStationVertex)) {
            graph.removeVertexAndEdges(rentalStationVertex);
        }
    }

    public void removeEdgeFromIdx (StreetEdge edge) {
        synchronized (this) {
            idx.remove(edge.getGeometry().getEnvelopeInternal(), edge);
        }
    }
}
