package org.opentripplanner.graph_builder.linking;

import com.google.common.collect.Iterables;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BikeParkUnlinked;
import org.opentripplanner.graph_builder.issues.BikeRentalStationUnlinked;
import org.opentripplanner.graph_builder.issues.EntranceUnlinked;
import org.opentripplanner.graph_builder.issues.StopLinkedTooFar;
import org.opentripplanner.graph_builder.issues.StopUnlinked;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.StreetBikeParkLink;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.edgetype.TransitEntranceLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
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
 * It would be wise to keep this new incarnation of the linking code relatively simple, considering what happened before.
 *
 * See discussion in pull request #1922, follow up issue #1934, and the original issue calling for replacement of
 * the stop linker, #1305.
 */
public class SimpleStreetSplitter {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleStreetSplitter.class);

    private static final int INITIAL_SEARCH_RADIUS_METERS = 100;

    private static final int MAX_SEARCH_RADIUS_METERS = 1000;

    private static final int WARNING_DISTANCE_METERS = 20;

    /** if there are two ways and the distances to them differ by less than this value, we link to both of them */
    private static final double DUPLICATE_WAY_EPSILON_METERS = 0.001;

    private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

    private final DataImportIssueStore issueStore;

    private final StreetEdgeFactory edgeFactory;

    private final Graph graph;

    private final HashGridSpatialIndex<Edge> idx;

    private final SpatialIndex transitStopIndex;

    // If true edges are split and new edges are created (used when linking transit stops etc. during graph building)
    // If false new temporary edges are created and no edges are deleted (Used when searching for origin/destination)
    private final boolean destructiveSplitting;

    private Boolean addExtraEdgesToAreas = false;

    /**
     * Construct a new SimpleStreetSplitter.
     * NOTE: Only one SimpleStreetSplitter should be active on a graph at any given time.
     *
     * @param hashGridSpatialIndex If not null this index is used instead of creating new one
     * @param transitStopIndex Index of all transitStops which is generated in {@link StreetVertexIndex}
     * @param destructiveSplitting If true splitting is permanent (Used when linking transit stops etc.) when false Splitting is only for duration of a request. Since they are made from temporary vertices and edges.
     */
    public SimpleStreetSplitter(Graph graph, HashGridSpatialIndex<Edge> hashGridSpatialIndex,
        SpatialIndex transitStopIndex, boolean destructiveSplitting, DataImportIssueStore issueStore
    ) {
        this.issueStore = issueStore;
        this.graph = graph;
        this.transitStopIndex = transitStopIndex;
        this.destructiveSplitting = destructiveSplitting;
        this.edgeFactory = new DefaultStreetEdgeFactory();

        //We build a spatial index if it isn't provided
        if (hashGridSpatialIndex == null) {
            // build a nice private spatial index, since we're adding and removing edges
            idx = new HashGridSpatialIndex<>();
            for (StreetEdge se : graph.getEdgesOfType(StreetEdge.class)) {
                idx.insert(se.getGeometry(), se);
            }
        } else {
            idx = hashGridSpatialIndex;
        }
    }

    /**
     * Construct a new SimpleStreetSplitter. Be aware that only one SimpleStreetSplitter should be
     * active on a graph at any given time.
     *
     * SimpleStreetSplitter generates index on graph and splits destructively (used in transit splitter)

     */
    public SimpleStreetSplitter(Graph graph, DataImportIssueStore issueStore) {
        this(graph, null, null, true, issueStore);
    }

    public static SimpleStreetSplitter createForTest(Graph graph) {
        return new SimpleStreetSplitter(graph, null, null, true, new DataImportIssueStore(false));
    }

    /** Link all relevant vertices to the street network */
    public void link () {
        link(TransitStopVertex.class, StopUnlinked::new);
        link(TransitEntranceVertex.class, EntranceUnlinked::new);
        link(BikeRentalStationVertex.class, BikeRentalStationUnlinked::new);
        link(BikeParkVertex.class, BikeParkUnlinked::new);
    }

    @SuppressWarnings("Convert2MethodRef")
    public <T extends Vertex> void link(
            Class<T> type,
            Function<T, DataImportIssue> unlinkedIssueMapper
    ) {
        @SuppressWarnings("unchecked")
        List<T> vertices = graph.getVertices()
                .stream()
                .filter(type::isInstance)
                .map(it -> (T)it)
                .collect(Collectors.toList());

        String actionName = "Link " + type.getSimpleName();

        if(vertices.isEmpty()) {
            LOG.info("{} skiped. No such data exist.", actionName);
            return;
        }

        ProgressTracker progress = ProgressTracker.track(actionName, 500, vertices.size());
        LOG.info(progress.startMessage());

        for (T v : vertices) {
            // Do not link vertices, which are already linked by TransitToTaggedStopsModule
            boolean alreadyLinked = v.getOutgoing().stream().anyMatch(e -> e instanceof StreetTransitLink);
            if (alreadyLinked) { continue; }

            // Do not link stops connected by pathways
            if (v instanceof TransitStopVertex && ((TransitStopVertex) v).hasPathways()) {
                continue;
            }

            if (!link(v)) {
                issueStore.add(unlinkedIssueMapper.apply(v));
            }
            // Keep lambda! A method-ref would cause incorrect class and line number to be logged
            progress.step(m -> LOG.info(m));
        }
        LOG.info(progress.completeMessage());
    }

    /** Link this vertex into the graph to the closest walkable edge */
    public boolean link (Vertex vertex) {
        return link(vertex, TraverseMode.WALK, null);
    }

    /**
     * Link the given vertex into the graph (expand on that...)
     * In OTP2 where the transit search can be quite fast, searching for a good linking point can be a significant
     * fraction of response time. Hannes Junnila has reported >70% speedups in searches by making the search radius
     * smaller. Therefore we use an expanding-envelope search, which is more efficient in dense areas.
     * @return whether linking succeeded (an edge or edges were found within range)
     */
    public boolean link(Vertex vertex, TraverseMode traverseMode, RoutingRequest options) {
        if (linkToStreetEdges(vertex, traverseMode, options, INITIAL_SEARCH_RADIUS_METERS)) {
            return true;
        }
        return linkToStreetEdges(vertex, traverseMode, options, MAX_SEARCH_RADIUS_METERS);
    }

    private static class DistanceTo<T> {
        T item;
        // Possible optimization: store squared lat to skip thousands of sqrt operations
        // However we're using JTS distance functions that probably won't allow us to skip the final sqrt call.
        double distanceDegreesLat;
        public DistanceTo (T item, double distanceDegreesLat) {
            this.item = item;
            this.distanceDegreesLat = distanceDegreesLat;
        }
    }

    public boolean linkToStreetEdges (Vertex vertex, TraverseMode traverseMode, RoutingRequest options, int radiusMeters) {

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(radiusMeters);

        Envelope env = new Envelope(vertex.getCoordinate());

        // Perform a simple local equirectangular projection, so distances are expressed in degrees latitude.
        final double xscale = Math.cos(vertex.getLat() * Math.PI / 180);

        // Expand more in the longitude direction than the latitude direction to account for converging meridians.
        env.expandBy(radiusDeg / xscale, radiusDeg);

        final double DUPLICATE_WAY_EPSILON_DEGREES = SphericalDistanceLibrary.metersToDegrees(DUPLICATE_WAY_EPSILON_METERS);

        final TraverseModeSet traverseModeSet = new TraverseModeSet(traverseMode);
        if (traverseMode == TraverseMode.BICYCLE) {
            traverseModeSet.setWalk(true);
        }
        // Scope block to avoid confusing edge-related local variables with stop-related variables below.
        {
            // Perform several transformations at once on the edges returned by the index.
            // Only consider street edges traversable by the given mode and still present in the graph.
            // Calculate a distance to each of those edges, and keep only the ones within the search radius.
            List<DistanceTo<StreetEdge>> candidateEdges = idx.query(env).stream()
                    .filter(StreetEdge.class::isInstance)
                    .map(StreetEdge.class::cast)
                    .filter(e -> e.canTraverse(traverseModeSet) && edgeReachableFromGraph(e))
                    .map(e -> new DistanceTo<>(e, distance(vertex, e, xscale)))
                    .filter(ead -> ead.distanceDegreesLat < radiusDeg)
                    .collect(Collectors.toList());

            // The following logic has gone through several different versions using different approaches.
            // The core idea is to find all edges that are roughly the same distance from the given vertex, which will
            // catch things like superimposed edges going in opposite directions.
            // First, all edges within DUPLICATE_WAY_EPSILON_METERS of of the best distance were selected.
            // More recently, the edges were sorted in order of increasing distance, and all edges in the list were selected
            // up to the point where a distance increase of DUPLICATE_WAY_EPSILON_DEGREES from one edge to the next.
            // This was in response to concerns about arbitrary cutoff distances: at any distance, it's always possible
            // one half of a dual carriageway (or any other pair of edges in opposite directions) will be caught and the
            // other half lost. It seems like this was based on some incorrect premises about floating point calculations
            // being non-deterministic.
            if (!candidateEdges.isEmpty()) {
                // There is at least one appropriate edge within range.
                double closestDistance = candidateEdges.stream()
                        .mapToDouble(ce -> ce.distanceDegreesLat)
                        .min().getAsDouble();

                candidateEdges.stream()
                        .filter(ce -> ce.distanceDegreesLat <= closestDistance + DUPLICATE_WAY_EPSILON_DEGREES)
                        .forEach(ce -> link(vertex, ce.item, xscale, options));

                // Warn if a linkage was made for a transit stop, but the linkage was suspiciously long.
                if (vertex instanceof TransitStopVertex) {
                    int distanceMeters = (int)SphericalDistanceLibrary.degreesLatitudeToMeters(closestDistance);
                    if (distanceMeters > WARNING_DISTANCE_METERS) {
                        issueStore.add(new StopLinkedTooFar((TransitStopVertex)vertex, distanceMeters));
                    }
                }
                return true;
            }
        }
        if (radiusMeters >= MAX_SEARCH_RADIUS_METERS) {
            // There were no candidate edges within the max linking distance, fall back on finding transit stops.
            // We only link to stops if we are searching for origin/destination and for that we need transitStopIndex.
            if (destructiveSplitting || transitStopIndex == null) {
                return false;
            }
            LOG.debug("No street edge was found for {}, checking transit stop vertices.", vertex);
            List<TransitStopVertex> transitStopVertices = transitStopIndex.query(env);
            List<DistanceTo<TransitStopVertex>> candidateStops = transitStopVertices.stream()
                    .map(tsv -> new DistanceTo<>(tsv, distance(vertex, tsv, xscale)))
                    .filter(dts -> dts.distanceDegreesLat <= radiusDeg)
                    .collect(Collectors.toList());

            if (candidateStops.isEmpty()) {
                LOG.debug("No stops nearby.");
                return false;
            }
            // There is at least one stop within range.
            double closestDistance = candidateStops.stream()
                    .mapToDouble(c -> c.distanceDegreesLat)
                    .min().getAsDouble();

            candidateStops.stream()
                    .filter(dts -> dts.distanceDegreesLat <= closestDistance + DUPLICATE_WAY_EPSILON_DEGREES)
                    .map(dts -> dts.item)
                    .forEach(sv -> {
                        LOG.debug("Linking vertex to stop: {}", sv.getName());
                        makeTemporaryEdges((TemporaryStreetLocation)vertex, sv);
                    });

            return true;
        }
        return false;
    }

    /**
     * While in destructive splitting mode (during graph construction rather than handling routing requests), we remove
     * edges that have been split and may then re-split the resulting segments recursively, so parts of them are also
     * removed. Newly created edge fragments are added to the spatial index; the edges that were split are removed
     * (disconnected) from the graph but were previously not removed from the spatial index, so for all subsequent
     * splitting operations we had to check whether any edge coming out of the spatial index had been "soft deleted".
     *
     * I believe this was compensating for the fact that STRTrees are optimized at construction and read-only. That
     * restriction no longer applies since we've been using our own hash grid spatial index instead of the STRTree.
     * So rather than filtering out soft deleted edges, this is now an assertion that the system behaves as intended,
     * and will log an error if the spatial index is returning edges that have been disconnected from the graph.
     */
    private static boolean edgeReachableFromGraph (Edge edge) {
        boolean edgeReachableFromGraph = edge.getToVertex().getIncoming().contains(edge);
        if (!edgeReachableFromGraph) {
            LOG.error("Edge returned from spatial index is no longer reachable from graph. That is not expected.");
        }
        return edgeReachableFromGraph;
    }

    // Link to all vertices in area/platform
    private void linkTransitToAreaVertices(Vertex splitterVertex, AreaEdgeList area) {
        List<Vertex> vertices = new ArrayList<>();

        for (AreaEdge areaEdge : area.getEdges()) {
            if (!vertices.contains(areaEdge.getToVertex())) vertices.add(areaEdge.getToVertex());
            if (!vertices.contains(areaEdge.getFromVertex())) vertices.add(areaEdge.getFromVertex());
        }

        for (Vertex vertex : vertices) {
            if (vertex instanceof  StreetVertex && !vertex.equals(splitterVertex)) {
                LineString line = GEOMETRY_FACTORY.createLineString(new Coordinate[] { splitterVertex.getCoordinate(), vertex.getCoordinate()});
                double length = SphericalDistanceLibrary.distance(splitterVertex.getCoordinate(),
                        vertex.getCoordinate());
                I18NString name = new LocalizedString("", new OSMWithTags());

                edgeFactory.createAreaEdge((IntersectionVertex) splitterVertex, (IntersectionVertex) vertex, line, name, length,StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area);
                edgeFactory.createAreaEdge((IntersectionVertex) vertex, (IntersectionVertex) splitterVertex, line, name, length,StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area);
            }
        }
    }

    /** split the edge and link in the transit stop */
    private void link(Vertex tstop, StreetEdge edge, double xscale, RoutingRequest options) {
        // TODO: we've already built this line string, we should save it
        LineString orig = edge.getGeometry();
        LineString transformed = equirectangularProject(orig, xscale);
        LocationIndexedLine il = new LocationIndexedLine(transformed);
        LinearLocation ll = il.project(new Coordinate(tstop.getLon() * xscale, tstop.getLat()));

        // if we're very close to one end of the line or the other, or endwise, don't bother to split,
        // cut to the chase and link directly
        // We use a really tiny epsilon here because we only want points that actually snap to exactly the same location on the
        // street to use the same vertices. Otherwise the order the stops are loaded in will affect where they are snapped.
        if (ll.getSegmentIndex() == 0 && ll.getSegmentFraction() < 1e-8) {
            makeLinkEdges(tstop, (StreetVertex) edge.getFromVertex());
        }
        // -1 converts from count to index. Because of the fencepost problem, npoints - 1 is the "segment"
        // past the last point
        else if (ll.getSegmentIndex() == orig.getNumPoints() - 1) {
            makeLinkEdges(tstop, (StreetVertex) edge.getToVertex());
        }

        // nPoints - 2: -1 to correct for index vs count, -1 to account for fencepost problem
        else if (ll.getSegmentIndex() == orig.getNumPoints() - 2 && ll.getSegmentFraction() > 1 - 1e-8) {
            makeLinkEdges(tstop, (StreetVertex) edge.getToVertex());
        }

        else {

            TemporaryVertex temporaryVertex = null;
            boolean endVertex = false;
            if (tstop instanceof TemporaryVertex) {
                temporaryVertex = (TemporaryVertex) tstop;
                endVertex = temporaryVertex.isEndVertex();

            }
            // split the edge, get the split vertex
            SplitterVertex v0 = split(edge, ll, temporaryVertex != null, endVertex);
            makeLinkEdges(tstop, v0);

            if (OTPFeature.FlexRouting.isOn() && graph.index != null
                && edge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN_AND_CAR)
            ) {
                Point p = GeometryUtils.getGeometryFactory().createPoint(v0.getCoordinate());
                Envelope env = p.getEnvelopeInternal();
                for (FlexStopLocation location : graph.index.getFlexIndex().locationIndex.query(env)) {
                    if (!location.getGeometry().disjoint(p)) {
                        if (v0.flexStopLocations == null) {
                            v0.flexStopLocations = new HashSet<>();
                        }
                        v0.flexStopLocations.add(location);
                    }
                }
            }

            // If splitter vertex is part of area; link splittervertex to all other vertexes in area, this creates
            // edges that were missed by WalkableAreaBuilder
            if (edge instanceof AreaEdge && tstop instanceof TransitStopVertex && this.addExtraEdgesToAreas) {
                linkTransitToAreaVertices(v0, ((AreaEdge) edge).getArea());
            }
        }
    }

    /**
     * Split the street edge at the given fraction
     *
     * @param edge to be split
     * @param ll fraction at which to split the edge
     * @param temporarySplit if true this is temporary split at origin/destinations search and only temporary edges vertices are created
     * @param endVertex if this is temporary edge this is true if this is end vertex otherwise it doesn't matter
     * @return Splitter vertex with added new edges
     */
    private SplitterVertex split (StreetEdge edge, LinearLocation ll, boolean temporarySplit, boolean endVertex) {
        LineString geometry = edge.getGeometry();

        // create the geometries
        Coordinate splitPoint = ll.getCoordinate(geometry);

        SplitterVertex v;
        String uniqueSplitLabel = "split_" + graph.nextSplitNumber++;
        if (temporarySplit) {
            TemporarySplitterVertex tsv = new TemporarySplitterVertex(
                    uniqueSplitLabel, splitPoint.x, splitPoint.y, edge, endVertex);
            tsv.setWheelchairAccessible(edge.isWheelchairAccessible());
            v = tsv;
        } else {
            v = new SplitterVertex(graph, uniqueSplitLabel, splitPoint.x, splitPoint.y, edge);
        }

        // Split the 'edge' at 'v' in 2 new edges and connect these 2 edges to the
        // existing vertices
        P2<StreetEdge> edges = edge.split(v, !temporarySplit);

        if (destructiveSplitting) {
            // update indices of new edges
            idx.insert(edges.first.getGeometry(), edges.first);
            idx.insert(edges.second.getGeometry(), edges.second);

            // remove original edge from the graph
            edge.getToVertex().removeIncoming(edge);
            edge.getFromVertex().removeOutgoing(edge);
            // remove original edges from the spatial index
            // This iterates over the entire rectangular envelope of the edge rather than the segments making it up.
            // It will be inefficient for very long edges, but creating a new remove method mirroring the more efficient
            // insert logic is not trivial and would require additional testing of the spatial index.
            idx.remove(edge.getGeometry().getEnvelopeInternal(), edge);
        }

        return v;
    }

    /** Make the appropriate type of link edges from a vertex */
    private void makeLinkEdges(Vertex from, StreetVertex to) {
        if (from instanceof TemporaryStreetLocation) {
            makeTemporaryEdges((TemporaryStreetLocation) from, to);
        } else if (from instanceof TransitStopVertex) {
            makeTransitLinkEdges((TransitStopVertex) from, to);
        } else if (from instanceof TransitEntranceVertex) {
            makeTransitLinkEdges((TransitEntranceVertex) from, to);
        } else if (from instanceof BikeRentalStationVertex) {
            makeBikeRentalLinkEdges((BikeRentalStationVertex) from, to);
        } else if (from instanceof BikeParkVertex) {
            makeBikeParkEdges((BikeParkVertex) from, to);
        }
    }

    /** Make temporary edges to origin/destination vertex in origin/destination search **/
    private void makeTemporaryEdges(TemporaryStreetLocation from, Vertex to) {
        if (destructiveSplitting) {
            throw new RuntimeException("Destructive splitting is used on temporary edges. Something is wrong!");
        }
        if (to instanceof TemporarySplitterVertex) {
            from.setWheelchairAccessible(((TemporarySplitterVertex) to).isWheelchairAccessible());
        }
        if (from.isEndVertex()) {
            LOG.debug("Linking end vertex to {} -> {}", to, from);
            new TemporaryFreeEdge(to, from);
        } else {
            LOG.debug("Linking start vertex to {} -> {}", from, to);
            new TemporaryFreeEdge(from, to);
        }
    }

    /** Make bike park edges */
    private void makeBikeParkEdges(BikeParkVertex from, StreetVertex to) {
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
    private void makeTransitLinkEdges (TransitStopVertex tstop, StreetVertex v) {
        if (!destructiveSplitting) {
            throw new RuntimeException("Transitedges are created with non destructive splitting!");
        }
        // ensure that the requisite edges do not already exist
        // this can happen if we link to duplicate ways that have the same start/end vertices.
        for (StreetTransitLink e : Iterables.filter(tstop.getOutgoing(), StreetTransitLink.class)) {
            if (e.getToVertex() == v)
                return;
        }

        new StreetTransitLink(tstop, v, tstop.hasWheelchairEntrance());
        new StreetTransitLink(v, tstop, tstop.hasWheelchairEntrance());
    }

    /**
     * Make street transit link edges, unless they already exist.
     */
    private void makeTransitLinkEdges(TransitEntranceVertex entrance, StreetVertex v) {
        if (!destructiveSplitting) {
            throw new RuntimeException("Transitedges are created with non destructive splitting!");
        }
        // ensure that the requisite edges do not already exist
        // this can happen if we link to duplicate ways that have the same start/end vertices.
        for (TransitEntranceLink e : Iterables.filter(entrance.getOutgoing(), TransitEntranceLink.class)) {
            if (e.getToVertex() == v) { return; }
        }

        new TransitEntranceLink(entrance, v, entrance.isWheelchairEntrance());
        new TransitEntranceLink(v, entrance, entrance.isWheelchairEntrance());
    }

    /** Make link edges for bike rental */
    private void makeBikeRentalLinkEdges (BikeRentalStationVertex from, StreetVertex to) {
        if (!destructiveSplitting) {
            throw new RuntimeException("Bike rental edges are created with non destructive splitting!");
        }
        for (StreetBikeRentalLink sbrl : Iterables.filter(from.getOutgoing(), StreetBikeRentalLink.class)) {
            if (sbrl.getToVertex() == to)
                return;
        }

        new StreetBikeRentalLink(from, to);
        new StreetBikeRentalLink(to, from);
    }

    /** projected distance from stop to edge, in latitude degrees */
    private static double distance (Vertex tstop, StreetEdge edge, double xscale) {
        // Despite the fact that we want to use a fast somewhat inaccurate projection, still use JTS library tools
        // for the actual distance calculations.
        LineString transformed = equirectangularProject(edge.getGeometry(), xscale);
        return transformed.distance(GEOMETRY_FACTORY.createPoint(new Coordinate(tstop.getLon() * xscale, tstop.getLat())));
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

        return GEOMETRY_FACTORY.createLineString(coords);
    }

    /**
     * Used to link origin and destination points to graph non destructively.
     * Split edges don't replace existing ones and only temporary edges and vertices are created.
     * Will throw TrivialPathException if origin and destination Location are on the same edge
     *
     * @param endVertex true if this is destination vertex
     */
    public Vertex getClosestVertex(GenericLocation location, RoutingRequest options,
        boolean endVertex) {
        if (destructiveSplitting) {
            throw new RuntimeException("Origin and destination search is used with destructive splitting. Something is wrong!");
        }
        if (endVertex) {
            LOG.debug("Finding end vertex for {}", location);
        } else {
            LOG.debug("Finding start vertex for {}", location);
        }
        Coordinate coord = location.getCoordinate();
        //TODO: add nice name
        String name;

        if (location.label == null || location.label.isEmpty()) {
            if (endVertex) {
                name = "Destination";
            } else {
                name = "Origin";
            }
        } else {
            name = location.label;
        }
        TemporaryStreetLocation closest = new TemporaryStreetLocation(UUID.randomUUID().toString(),
            coord, new NonLocalizedString(name), endVertex);

        TraverseMode nonTransitMode = TraverseMode.WALK;
        //It can be null in tests
        if (options != null) {
            TraverseModeSet modes = options.streetSubRequestModes;
            if (modes.getCar())
                if (options.carPickup) {
                    nonTransitMode = TraverseMode.WALK;
                }
                // for park and ride we will start in car mode and walk to the end vertex
                else if (endVertex && options.parkAndRide) {
                    nonTransitMode = TraverseMode.WALK;
                } else {
                    nonTransitMode = TraverseMode.CAR;
                }
            else if (modes.getWalk())
                nonTransitMode = TraverseMode.WALK;
            else if (modes.getBicycle())
                nonTransitMode = TraverseMode.BICYCLE;
        }

        if(!link(closest, nonTransitMode, options)) {
            LOG.warn("Couldn't link {}", location);
        }
        return closest;

    }

    public void setAddExtraEdgesToAreas(Boolean addExtraEdgesToAreas) {
        this.addExtraEdgesToAreas = addExtraEdgesToAreas;
    }
}
