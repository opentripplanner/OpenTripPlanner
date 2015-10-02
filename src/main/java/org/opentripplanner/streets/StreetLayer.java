package org.opentripplanner.streets;

import com.conveyal.gtfs.model.Stop;
import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.TransitLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

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

    /**
     * Minimum allowable size (in number of vertices) for a disconnected subgraph; subgraphs smaller than these will be removed.
     * There are several reasons why one might have a disconnected subgraph. The most common is poor quality
     * OSM data. However, they also could be due to areas that really are disconnected in the street graph,
     * and are connected only by transit. These could be literal islands (Vashon Island near Seattle comes
     * to mind), or islands that are isolated by infrastructure (for example, airport terminals reachable
     * only by transit or driving, for instance BWI or SFO).
     */
    public static final int MIN_SUBGRAPH_SIZE = 40;

    private static final int SNAP_RADIUS_MM = 5 * 1000;

    // Edge lists should be constructed after the fact from edges. This minimizes serialized size too.
    transient List<TIntList> outgoingEdges;
    transient List<TIntList> incomingEdges;
    public transient IntHashGrid spatialIndex = new IntHashGrid();

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

    public TransitLayer linkedTransitLayer = null;

    /** Load street layer from an OSM-lib OSM DB */
    public void loadFromOsm(OSM osm) {
        loadFromOsm(osm, true, false);
    }

    /** Load OSM, optionally removing floating subgraphs (recommended) */
    void loadFromOsm (OSM osm, boolean removeIslands, boolean saveVertexIndex) {
        if (!osm.intersectionDetection)
            throw new IllegalArgumentException("Intersection detection not enabled on OSM source");

        LOG.info("Making street edges from OSM ways...");
        this.osm = osm;
        for (Map.Entry<Long, Way> entry : osm.ways.entrySet()) {
            Way way = entry.getValue();
            if ( ! (way.hasTag("highway") || way.hasTag("public_transport", "platform"))) {
                continue;
            }

            // don't allow users to use proposed infrastructure
            if (way.hasTag("highway", "proposed"))
                continue;

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

        if (removeIslands)
            removeDisconnectedSubgraphs(MIN_SUBGRAPH_SIZE);

        edgesPerWayHistogram.display();
        pointsPerEdgeHistogram.display();
        // Clear unneeded indexes, allow them to be gc'ed
        if (!saveVertexIndex)
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
            // FIXME for now we are skipping edges over 1km because they can have huge envelopes. TODO Rasterize them.
            //if (edge.getLengthMm() < 1 * 1000 * 1000) {
            spatialIndex.insert(edge.getEnvelope(), e);
            //}
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
            VertexStore.Vertex vertex = vertexStore.getCursor(from);
            // LOG.info("Routing from ({}, {}).", vertex.getLat(), vertex.getLon());
            router.setOrigin(from);
            router.toVertex = withDestinations ? random.nextInt(nVertices) : StreetRouter.ALL_VERTICES;
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
        if (split.distance0_mm < SNAP_RADIUS_MM || split.distance1_mm < SNAP_RADIUS_MM) {
            if (split.distance0_mm < split.distance1_mm) {
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
        edge.setLengthMm(split.distance0_mm);
        edge.setToVertex(newVertexIndex);
        edge.setGeometry(Collections.EMPTY_LIST); // Turn it into a straight line for now.

        // Make a second, new bidirectional edge pair after the split and add it to the spatial index.
        // New edges will be added to edge lists later (the edge list is a transient index).
        EdgeStore.Edge newEdge = edgeStore.addStreetPair(newVertexIndex, oldToVertex, split.distance1_mm);
        spatialIndex.insert(newEdge.getEnvelope(), newEdge.edgeIndex);
        // TODO newEdge.copyFlagsFrom(edge) to match the existing edge...
        return newVertexIndex;

        // TODO store street-to-stop distance in a table in TransitLayer. This also allows adjusting for subway entrances etc.

    }

    /**
     * Non-destructively find a location on an existing street near the given point.
     * PARAMETERS ARE FLOATING POINT GEOGRAPHIC (not fixed point ints)
     * @return a Split object representing a point along a sub-segment of a specific edge, or null if there are no streets nearby.
     */
    public Split findSplit (double lat, double lon, double radiusMeters) {
        return Split.find (lat, lon, radiusMeters, this);
    }


    /**
     * For every stop in a TransitLayer, find or create a nearby vertex in the street layer and record the connection
     * between the two.
     * It only makes sense to link one TransitLayer to one StreetLayer, otherwise the bi-mapping between transit stops
     * and street vertices would be ambiguous.
     */
    public void associateStops (TransitLayer transitLayer, int radiusMeters) {
        for (Stop stop : transitLayer.stopForIndex) {
            int streetVertexIndex = getOrCreateVertexNear(stop.stop_lat, stop.stop_lon, radiusMeters);
            transitLayer.streetVertexForStop.add(streetVertexIndex); // -1 means no link
            // The inverse stopForStreetVertex map is a transient, derived index and will be built later.
        }
        // Bidirectional reference between the StreetLayer and the TransitLayer
        transitLayer.linkedStreetLayer = this;
        this.linkedTransitLayer = transitLayer;
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

    /**
     * Find and remove all subgraphs with fewer than minSubgraphSize vertices. Uses a flood fill
     * algorithm, see http://stackoverflow.com/questions/1348783.
     */
    public void removeDisconnectedSubgraphs(int minSubgraphSize) {
        LOG.info("Removing subgraphs with fewer than {} vertices");
        boolean edgeListsBuilt = incomingEdges != null;

        int nSubgraphs = 0;

        if (!edgeListsBuilt)
            buildEdgeLists();

        // labels for the flood fill algorithm
        TIntIntMap vertexLabels = new TIntIntHashMap();

        // vertices and edges that should be removed
        TIntSet verticesToRemove = new TIntHashSet();
        TIntSet edgesToRemove = new TIntHashSet();

        for (int vertex = 0; vertex < vertexStore.nVertices; vertex++) {
            // N.B. this is not actually running a search for every vertex as after the first few
            // almost all of the vertices are labeled
            if (vertexLabels.containsKey(vertex))
                continue;

            StreetRouter r = new StreetRouter(this);
            r.setOrigin(vertex);
            // walk to the end of the graph
            r.distanceLimitMeters = 100000;
            r.route();

            TIntList reachedVertices = new TIntArrayList();

            int nReached = 0;
            for (int reachedVertex = 0; reachedVertex < vertexStore.nVertices; reachedVertex++) {
                if (r.getTravelTimeToVertex(reachedVertex) != Integer.MAX_VALUE) {
                    nReached++;
                    // use source vertex as label, saves a variable
                    vertexLabels.put(reachedVertex, vertex);
                    reachedVertices.add(reachedVertex);
                }
            }

            if (nReached < minSubgraphSize) {
                LOG.debug("Removing disconnected subgraph of size {} near {}, {}",
                        nReached, vertexStore.fixedLats.get(vertex) / VertexStore.FIXED_FACTOR,
                        vertexStore.fixedLons.get(vertex) / VertexStore.FIXED_FACTOR);
                nSubgraphs++;
                verticesToRemove.addAll(reachedVertices);
                reachedVertices.forEach(v -> {
                    // can't use method reference here because we always have to return true
                    incomingEdges.get(v).forEach(e -> {
                        edgesToRemove.add(e);
                        return true; // continue iteration
                    });
                    outgoingEdges.get(v).forEach(e -> {
                        edgesToRemove.add(e);
                        return true; // continue iteration
                    });
                    return true; // iteration should continue
                });
            }

            if (nSubgraphs > 0)
                LOG.info("Removed {} disconnected subgraphs", nSubgraphs);
            else
                LOG.info("Found no subgraphs to remove, congratulations for having clean OSM data.");
        }

        // rebuild the edge store with some edges removed
        edgeStore.remove(edgesToRemove.toArray());
        // TODO remove vertices as well? this is messy because the edges point into them

        // don't forget this
        if (edgeListsBuilt)
            buildEdgeLists();
        else {
            incomingEdges = null;
            outgoingEdges = null;
        }

        LOG.info("Done removing subgraphs. {} edges remain", edgeStore.nEdges);
    }
}
