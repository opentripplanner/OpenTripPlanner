package org.opentripplanner.streets;

import com.conveyal.gtfs.model.Stop;
import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.TransitLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This stores the street layer of OTP routing data.
 *
 * Is is currently using a column store.
 * An advantage of disk-backing this (FSTStructs, MapDB optimized for zero-based
 * integer keys) would be that we can remove any logic about loading/unloading graphs.
 * We could route over a whole continent without using much memory.
 *
 * Any data that's not used by Analyst workers (street names and geometries for example)
 * should be optional so we can have fast-loading, small transportation network files to pass around.
 * It can even be loaded from the OSM MapDB on demand.
 *
 * There's also https://github.com/RichardWarburton/slab
 * which seems simpler to use.
 *
 * TODO Morton-code-sort vertices, then sort edges by from-vertex.
 */
public class StreetLayer implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(StreetLayer.class);

    // Edge lists should be constructed after the fact from edges. This minimizes serialized size too.
    transient List<TIntList> outgoingEdges;
    transient List<TIntList> incomingEdges;
    transient IntHashGrid spatialIndex = new IntHashGrid();

    TLongIntMap vertexIndexForOsmNode = new TLongIntHashMap(100_000, 0.75f, -1, -1);
    // TIntLongMap osmWayForEdgeIndex;

    // TODO use negative IDs for temp vertices and edges.

    // This is only used when loading from OSM, and is then nulled to save memory.
    transient OSM osm;

    // Initialize these when we have an estimate of the number of expected edges.
    VertexStore vertexStore = new VertexStore(100_000);
    EdgeStore edgeStore = new EdgeStore(vertexStore, 200_000);

    transient Histogram edgesPerWayHistogram = new Histogram("Number of edges per way per direction");
    transient Histogram pointsPerEdgeHistogram = new Histogram("Number of geometry points per edge");

    public void loadFromOsm (OSM osm) {
        LOG.info("Making street edges from OSM ways...");
        this.osm = osm;
        for (Map.Entry<Long, Way> entry : osm.ways.entrySet()) {
            Way way = entry.getValue();
            if (!way.hasTag("highway")) {
                continue;
            }
            int nEdgesCreated = 0;
            int beginIdx = 0;
            for (int n = 1; n < way.nodes.length; n++) {
                if (osm.intersectionNodes.contains(way.nodes[n]) || n == (way.nodes.length - 1)) {
                    makeEdge(way, beginIdx, n);
                    nEdgesCreated += 1;
                    beginIdx = n;
                }
            }
            edgesPerWayHistogram.add(nEdgesCreated);
        }
        LOG.info("Done making street edges.");
        LOG.info("Made {} vertices and {} edges.", vertexStore.nVertices, edgeStore.nEdges);
        edgesPerWayHistogram.display();
        pointsPerEdgeHistogram.display();
        // Clear unneeded indexes
        vertexIndexForOsmNode = null;
        osm = null;
    }

    // Get or create mapping
    private int getVertexIndexForOsmNode(long osmNodeId) {
        int vertexIndex = vertexIndexForOsmNode.get(osmNodeId);
        if (vertexIndex == -1) {
            // Register a new vertex, incrementing the index starting from zero.
            // Store node coordinates for this new street vertex
            Node node = osm.nodes.get(osmNodeId);
            vertexIndex = vertexStore.addVertex(node.getLat(), node.getLon());
            vertexIndexForOsmNode.put(osmNodeId, vertexIndex);
        }
        return vertexIndex;
    }

    /**
     * Calculate length from a list of nodes. This is done in advance of creating an edge pair because we need to catch
     * potential length overflows before we ever reserve space for the edges.
     */
    private int getEdgeLengthMillimeters (List<Node> nodes) {
        double lengthMeters = 0;
        Node prevNode = nodes.get(0);
        for (Node node : nodes.subList(1, nodes.size())) {
            lengthMeters += SphericalDistanceLibrary
                    .fastDistance(prevNode.getLat(), prevNode.getLon(), node.getLat(), node.getLon());
            prevNode = node;
        }
        if (lengthMeters * 1000 > Integer.MAX_VALUE) {
            return -1;
        }
        return (int)(lengthMeters * 1000);
    }

    private void makeEdge (Way way, int beginIdx, int endIdx) {

        long beginOsmNodeId = way.nodes[beginIdx];
        long endOsmNodeId = way.nodes[endIdx];

        // Will create mapping if it doesn't exist yet.
        int beginVertexIndex = getVertexIndexForOsmNode(beginOsmNodeId);
        int endVertexIndex = getVertexIndexForOsmNode(endOsmNodeId);

        // Fetch the OSM node objects for this subsection of the OSM way.
        int nNodes = endIdx - beginIdx + 1;
        List<Node> nodes = new ArrayList<>(nNodes);
        for (int n = beginIdx; n <= endIdx; n++) {
            long nodeId = way.nodes[n];
            Node node = osm.nodes.get(nodeId);
            nodes.add(node);
        }

        // Compute edge length and check that it can be properly represented.
        int edgeLengthMillimeters = getEdgeLengthMillimeters(nodes);
        if (edgeLengthMillimeters < 0) {
            LOG.warn("Street segment was too long to be represented, skipping.");
            return;
        }

        // Create and store the forward and backward edge
        EdgeStore.Edge newForwardEdge = edgeStore.addStreetPair(beginVertexIndex, endVertexIndex, edgeLengthMillimeters);
        newForwardEdge.setGeometry(nodes);
        pointsPerEdgeHistogram.add(nNodes);

    }

    public void indexStreets () {
        LOG.info("Indexing streets...");
        spatialIndex = new IntHashGrid();
        // Skip by twos, we only need to index forward (even) edges.
        EdgeStore.Edge edge = edgeStore.getCursor();
        for (int e = 0; e < edgeStore.nEdges; e += 2) {
            edge.seek(e);
            spatialIndex.insert(edge.getEnvelope(), e);
        }
        LOG.info("Done indexing streets.");
    }

    /** After JIT this appears to scale almost linearly with number of cores. */
    public void testRouting (boolean withDestinations, TransitLayer transitLayer) {
        LOG.info("Routing from random vertices in the graph...");
        LOG.info("{} goal direction.", withDestinations ? "Using" : "Not using");
        StreetRouter router = new StreetRouter(this, transitLayer);
        long startTime = System.currentTimeMillis();
        final int N = 1_000;
        final int nVertices = outgoingEdges.size();
        Random random = new Random();
        for (int n = 0; n < N; n++) {
            int from = random.nextInt(nVertices);
            int to = withDestinations ? random.nextInt(nVertices) : StreetRouter.ALL_VERTICES;
            VertexStore.Vertex vertex = vertexStore.getCursor(from);
            // LOG.info("Routing from ({}, {}).", vertex.getLat(), vertex.getLon());
            router.route(from, to);
            if (n != 0 && n % 100 == 0) {
                LOG.info("    {}/{} searches", n, N);
            }
        }
        double eTime = System.currentTimeMillis() - startTime;
        LOG.info("average response time {} msec", eTime / N);
    }

    public void buildEdgeLists() {
        LOG.info("Building edge lists from edges...");
        outgoingEdges = new ArrayList<>(vertexStore.nVertices);
        incomingEdges = new ArrayList<>(vertexStore.nVertices);
        for (int v = 0; v < vertexStore.nVertices; v++) {
            outgoingEdges.add(new TIntArrayList(4));
            incomingEdges.add(new TIntArrayList(4));
        }
        EdgeStore.Edge edge = edgeStore.getCursor();
        while (edge.advance()) {
            outgoingEdges.get(edge.getFromVertex()).add(edge.edgeIndex);
            incomingEdges.get(edge.getToVertex()).add(edge.edgeIndex);
        }
        LOG.info("Done building edge lists.");
        // Display histogram of edge list sizes
        Histogram edgesPerListHistogram = new Histogram("Number of edges per edge list");
        for (TIntList edgeList : outgoingEdges) {
            edgesPerListHistogram.add(edgeList.size());
        }
        for (TIntList edgeList : incomingEdges) {
            edgesPerListHistogram.add(edgeList.size());
        }
        edgesPerListHistogram.display();
    }

    /**
     * Create a street-layer vertex representing a transit stop.
     * Connect that new vertex to the street network if possible.
     * The vertex will be created and assigned an index whether or not it is successfully linked.
     *
     * TODO maybe use X and Y everywhere for fixed point, and lat/lon for double precision degrees.
     *
     * @return the street vertex index that was created for this stop.
     */
    public int linkTransitStop (double lat, double lon, double radiusMeters) {

        // NOTE THIS ENTIRE GEOMETRIC CALCULATION IS HAPPENING IN FIXED PRECISION INT DEGREES
        int fLat = VertexStore.degreesToFixedInt(lat);
        int fLon = VertexStore.degreesToFixedInt(lon);

        // We won't worry about the perpendicular walks yet.
        // Just insert or find a vertex on the nearest road and return that vertex.

        final double metersPerDegreeLat = 111111.111;
        double cosLat = FastMath.cos(FastMath.toRadians(lat)); // The projection factor, Earth is a "sphere"
        double radiusFixedLat = VertexStore.degreesToFixedInt(radiusMeters / metersPerDegreeLat);
        double radiusFixedLon = radiusFixedLat / cosLat; // Expand the X search space, don't shrink it.
        Envelope envelope = new Envelope(fLon, fLon, fLat, fLat);
        envelope.expandBy(radiusFixedLon, radiusFixedLat);
        double squaredRadiusFixedLat = radiusFixedLat * radiusFixedLat; // May overflow, don't use an int
        EdgeStore.Edge edge = edgeStore.getCursor();
        TIntIterator edgeIterator = spatialIndex.query(envelope).iterator(); // Iterate over forward (even) edges found.
        // The split location currently being examined and the best one seen so far.
        Split curr = new Split();
        Split best = new Split();
        while (edgeIterator.hasNext()) {
            curr.edge = edgeIterator.next();
            edge.seek(curr.edge);
            edge.iterateGeometry((seg, fLat0, fLon0, fLat1, fLon1) -> {
                // Find the fraction along the current segment
                curr.seg = seg;
                curr.frac = GeometryUtils.segmentFraction(fLon0, fLat0, fLon1, fLat1, fLon, fLat, cosLat);
                // Project to get the closest point on the segment.
                // Note: the fraction is scaleless, xScale is accounted for in the segmentFraction function.
                curr.fLon = fLon0 + curr.frac * (fLon1 - fLon0);
                curr.fLat = fLat0 + curr.frac * (fLat1 - fLat0);
                // Find squared distance to edge (avoid taking root)
                double dx = (curr.fLon - fLon) * cosLat;
                double dy = (curr.fLat - fLat);
                curr.distSquared = dx * dx + dy * dy;
                // Ignore segments that are too far away (filter false positives).
                // Replace the best segment if we've found something closer.
                if (curr.distSquared < squaredRadiusFixedLat && curr.distSquared < best.distSquared) {
                    best.setFrom(curr);
                }
            }); // end loop over segments
        } // end loop over edges

        // If no linking site was found within range, exit.
        if (best.edge < 0) {
            return -1;
        }

        // We have a linking site. Find or make a suitable vertex at that site.
        edge.seek(best.edge);
        if (best.frac == 0 && best.seg == 0) {
            return edge.getFromVertex(); // At the beginning of the edge
        } else if (best.frac == 1 && best.seg == edge.nSegments() - 1) {
            return edge.getToVertex(); // At the end of the edge
        }
        // The split is somewhere away from an existing vertex. Make a new one.
        // We must iterate back over the edge to accumulate distances along its geometry.

        best.lengthBefore = 0; // stored in Split to avoid "effectively final" BS.
        edge.iterateGeometry((seg, fLat0, fLon0, fLat1, fLon1) -> {
            // Sum lengths only up to the split point.
            // lengthAfter should be total length minus lengthBefore, which ensures splits do not change total lengths.
            if (seg <= best.seg) {
                double dx = (fLon1 - fLon0) * cosLat;
                double dy = (fLat1 - fLat0);
                double length = FastMath.sqrt(dx * dx + dy * dy);
                // TODO accumulate before/after geoms. Split point can be passed over since it's not an intermediate.
                if (seg == best.seg) {
                    length *= best.frac;
                }
                best.lengthBefore += length;
            }
        });

        // Create a new vertex at the split point.
        int newVertexIndex = vertexStore.addVertexFixed((int)best.fLat, (int)best.fLon);

        // Convert the fixed-precision degree measurements into (milli)meters
        int length_mm_1 = (int)(VertexStore.fixedIntToDegrees((int)(best.lengthBefore)) * metersPerDegreeLat * 1000);
        int length_mm_2 = edge.getLengthMm() - length_mm_1;

        // Modify the existing bidirectional edge pair to lead up to the split.
        // Its spatial index entry is still valid, its envelope has only shrunk.
        int oldToVertex = edge.getToVertex();
        edge.setLengthMm(length_mm_1);
        edge.setToVertex(newVertexIndex);
        edge.setGeometry(Collections.EMPTY_LIST); // Turn it into a straight line for now.

        // Make a second, new bidirectional edge pair after the split and add it to the spatial index.
        // New edges will be added to edge lists later (the edge list is a transient index).
        EdgeStore.Edge newEdge = edgeStore.addStreetPair(newVertexIndex, oldToVertex, length_mm_2);
        spatialIndex.insert(newEdge.getEnvelope(), newEdge.edgeIndex);
        // TODO newEdge.copyFlagsFrom(edge) to match the existing edge...
        return newVertexIndex;
    }

    public void linkStops (TransitLayer transitLayer) {
        for (Stop stop : transitLayer.stopForIndex) {
            int streetVertexIndex = linkTransitStop(stop.stop_lat, stop.stop_lon, 300);
            transitLayer.streetVertexForStop.add(streetVertexIndex); // -1 means no link
            // The inverse stopForStreetVertex map is a transient, derived index and will be built from streetVertexForStop.
        }
    }

    /**
     * Used to split streets for temporary endpoints and for transit stops.
     * transit: favor platforms and pedestrian paths, used in linking stops to streets
     * intoStreetLayer: the edges created by splitting a street are one-way. by default they are one-way out of the street
     * network, e.g. out to a transit stop or to the destination. When intoStreets is true, the split is performed such that
     * it leads into the street network instead of out of it. The fact that split edges are uni-directional is important
     * for a couple of reasons: it avoids using transit stops as shortcuts, and it makes temporary endpoint vertices
     * harmless to searches happening in other threads.
     */
    public void splitStreet(int fixedLon, int fixedLat, boolean transit, boolean out) {

    }

    public int getVertexCount() {
        return vertexStore.nVertices;
    }

}
