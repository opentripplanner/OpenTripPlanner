package org.opentripplanner.streets;

import com.conveyal.gtfs.model.Stop;
import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
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

    private static final int SNAP_RADIUS_MM = 5 * 1000;

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
    public VertexStore vertexStore = new VertexStore(100_000);
    public EdgeStore edgeStore = new EdgeStore(vertexStore, 200_000);

    transient Histogram edgesPerWayHistogram = new Histogram("Number of edges per way per direction");
    transient Histogram pointsPerEdgeHistogram = new Histogram("Number of geometry points per edge");

    public void loadFromOsm (OSM osm) {
        LOG.info("Making street edges from OSM ways...");
        this.osm = osm;
        for (Map.Entry<Long, Way> entry : osm.ways.entrySet()) {
            Way way = entry.getValue();
            if ( ! (way.hasTag("highway") || way.hasTag("area", "yes") || way.hasTag("public_transport", "platform"))) {
                continue;
            }
            int nEdgesCreated = 0;
            int beginIdx = 0;
            // Break each OSM way into topological segments between intersections, and make one edge per segment.
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

    /**
     * Get or create mapping from a global long OSM ID to an internal street vertex ID, creating the vertex as needed.
     */
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

    /**
     * Make an edge for a sub-section of an OSM way, typically between two intersections or dead ends.
     */
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
        // Skip by twos, we only need to index forward (even) edges. Their odd companions have the same geometry.
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
        StreetRouter router = new StreetRouter(this);
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
     * TODO move this into Split.perform(), store streetLayer ref in Split
     *
     * @return the index of a street vertex very close to this stop, or -1 if no such vertex could be found or created.
     */
    public int getOrCreateVertexNear (double lat, double lon, double radiusMeters) {

        Split split = findSplit(lat, lon, radiusMeters);
        if (split == null) {
            // If no linking site was found within range.
            return -1;
        }

        // We have a linking site. Find or make a suitable vertex at that site.
        // Retaining the original Edge cursor object inside findSplit is not necessary, one object creation is harmless.
        EdgeStore.Edge edge = edgeStore.getCursor(split.edge);

        // Check for cases where we don't need to create a new vertex (the edge is reached end-wise)
        if (split.lengthBefore_mm < SNAP_RADIUS_MM || split.lengthAfter_mm < SNAP_RADIUS_MM) {
            if (split.lengthBefore_mm < split.lengthAfter_mm) {
                // Very close to the beginning of the edge.
                return edge.getFromVertex();
            } else {
                // Very close to the end of the edge.
                return edge.getToVertex();
            }
        }

        // The split is somewhere away from an existing intersection vertex. Make a new vertex.
        int newVertexIndex = vertexStore.addVertexFixed((int)split.fLat, (int)split.fLon);

        // Modify the existing bidirectional edge pair to lead up to the split.
        // Its spatial index entry is still valid, its envelope has only shrunk.
        int oldToVertex = edge.getToVertex();
        edge.setLengthMm(split.lengthBefore_mm);
        edge.setToVertex(newVertexIndex);
        edge.setGeometry(Collections.EMPTY_LIST); // Turn it into a straight line for now.

        // Make a second, new bidirectional edge pair after the split and add it to the spatial index.
        // New edges will be added to edge lists later (the edge list is a transient index).
        EdgeStore.Edge newEdge = edgeStore.addStreetPair(newVertexIndex, oldToVertex, split.lengthAfter_mm);
        spatialIndex.insert(newEdge.getEnvelope(), newEdge.edgeIndex);
        // TODO newEdge.copyFlagsFrom(edge) to match the existing edge...
        return newVertexIndex;

        // TODO store street-to-stop distance in a table in TransitLayer. This also allows adjusting for subway entrances etc.

    }

    /**
     * Non-destructively find a good split location on an existing street.
     * @return a Split object representing a point along a sub-segment of a specific edge.
     */
    public Split findSplit (double lat, double lon, double radiusMeters) {
        return Split.find (lat, lon, radiusMeters, this);
    }

    /**
     * For every stop in a TransitLayer, find or create a nearby vertex in the street layer and record the connection
     * between the two.
     */
    public void associateStops (TransitLayer transitLayer, int radiusMeters) {
        for (Stop stop : transitLayer.stopForIndex) {
            int streetVertexIndex = getOrCreateVertexNear(stop.stop_lat, stop.stop_lon, radiusMeters);
            transitLayer.streetVertexForStop.add(streetVertexIndex); // -1 means no link
            // The inverse stopForStreetVertex map is a transient, derived index and will be built later.
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
