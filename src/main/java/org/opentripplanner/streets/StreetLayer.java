package org.opentripplanner.streets;

import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.nustaq.offheap.bytez.malloc.MallocBytezAllocator;
import org.nustaq.offheap.structs.FSTStructAllocator;
import org.nustaq.offheap.structs.structtypes.StructArray;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.streets.structs.StreetIntersection;
import org.opentripplanner.streets.structs.StreetSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This stores the street layer of OTP routing data.
 * It uses FST structs.
 * https://github.com/RuedigerMoeller/fast-serialization/wiki/Structs
 * Usage gleaned from StructArray class.
 *
 * "Unfortunately, Oracle is removing Unsafe from Java 9 release"
 * "fast serialization does not rely on unsafe. If its present it makes use of it"
 *
 * There's also https://github.com/RichardWarburton/slab
 * which seems simpler to use.
 */
public class StreetLayer {

    private static final Logger LOG = LoggerFactory.getLogger(StreetLayer.class);

    private static int DEFAULT_SPEED_KPH = 50;

    // Edge lists should be constructed after the fact from edges. This minimizes serialized size too.
    List<TIntList> outgoingEdges;
    List<TIntList> incomingEdges;

    TLongIntMap vertexIndexForOsmNode = new TLongIntHashMap(100_000, 0.75f, -1, -1);
    TIntLongMap osmWayForEdgeIndex;
    int nextEdgeIndex = 0; // TODO replace with size of osmWayForEdgeIndex

    // TODO use negative IDs for temp vertices and edges.
    StructArray<StreetSegment> edges;
    StructArray<StreetIntersection> vertices;

    // We can't record the vertex coordinates here yet because we don't know how many vertices there are
    // and therefore cannot allocate storage for them.
    private void registerIntersection (long osmNodeId) {
        int vertexIndex = vertexIndexForOsmNode.get(osmNodeId);
        if (vertexIndex == -1) {
            // Register a new vertex, incrementing the index starting from zero.
            vertexIndex = vertexIndexForOsmNode.size();
            vertexIndexForOsmNode.put(osmNodeId, vertexIndex);
        }
    }

    private void copyIntersectionCoordiantes (OSM osm) {
        LOG.info("Copying OSM node coordinates for street intersections...");
        TLongIntIterator iter = vertexIndexForOsmNode.iterator();
        // TODO Store OSM node for vertex index as well.
        while (iter.hasNext()) {
            iter.advance();
            long osmNodeId = iter.key();
            int vertexIndex = iter.value();
            // Copy coordinates over from the OSM node.
            Node node = osm.nodes.get(osmNodeId);
            StreetIntersection intersection = vertices.get(vertexIndex);
            intersection.setLat(node.getLat());
            intersection.setLon(node.getLon());
        }
        LOG.info("Done copying coordinates.");
    }

    public void loadFromOsm (OSM osm) {
        // Create appropriately sized storage for the street graph
        countAndAllocate(osm);
        copyIntersectionCoordiantes(osm);
        LOG.info("Making street edges from OSM ways...");
        for (Map.Entry<Long, Way> entry : osm.ways.entrySet()) {
            Way way = entry.getValue();
            if (!way.hasTag("highway")) {
                continue;
            }
            int beginIdx = 0;
            for (int n = 1; n < way.nodes.length; n++) {
                if (osm.intersectionNodes.contains(way.nodes[n]) || n == (way.nodes.length - 1)) {
                    makeEdge(osm, way, beginIdx, n);
                    beginIdx = n;
                }
            }
        }
        LOG.info("Done making street edges.");
        LOG.info("Made {} edges and {} vertices.", nextEdgeIndex, vertexIndexForOsmNode.size());
    }

    /**
     * Perform a dry run through edge generation, creating all necessary vertices and edge lists.
     * You can't embed outgoing edges in the vertices themselves, that doesn't work when looking
     * for incoming edges (unless you duplicate every edge twice). And it's kind of messy for
     * street matching, live updates of speeds etc.
     */
    private void countAndAllocate(OSM osm) {
        LOG.info("Counting edges before allocating storage...");
        int nEdges = 0;
        for (Way way : osm.ways.values()) {
            if (!way.hasTag("highway")) {
                continue;
            }
            // Every way produces at least one forward and one backward edge.
            nEdges += 2;
            // Assign intersection indexes to the beginning/end OSM nodes as needed.
            registerIntersection(way.nodes[0]);
            registerIntersection(way.nodes[way.nodes.length - 1]);
            for (int n = 1; n < way.nodes.length - 1; n++) {
                if (osm.intersectionNodes.contains(way.nodes[n])) {
                    // Assign an index to this intersection as needed.
                    registerIntersection(way.nodes[n]);
                    // Each intersection along the way will add one forward and one backward edge.
                    nEdges += 2;
                }
            }
        }
        int nVertices = vertexIndexForOsmNode.size();
        LOG.info("Allocating storage for {} vertices and {} edges.", nVertices, nEdges);
        // FSTStructAllocator structAllocator = new FSTStructAllocator(1024 * 1024);
        // Using the off-heap allocator speeds up execution by about 25%.
        FSTStructAllocator structAllocator = new FSTStructAllocator(1024 * 1024, new MallocBytezAllocator());
        edges = structAllocator.newArray(nEdges, new StreetSegment());
        vertices = structAllocator.newArray(nVertices, new StreetIntersection());
    }

    // TODO Morton-code-sort vertices, then sort edges by from-vertex.

    private void makeEdge (OSM osm, Way way, int beginIdx, int endIdx) {

        long beginOsmNodeId = way.nodes[beginIdx];
        long endOsmNodeId = way.nodes[endIdx];

        // Getting a vertex index will create edge lists as needed.
        int beginVertexIndex = vertexIndexForOsmNode.get(beginOsmNodeId);
        int endVertexIndex = vertexIndexForOsmNode.get(endOsmNodeId);

        // Determine geometry and length of this edge
        Node prevNode = osm.nodes.get(beginOsmNodeId);
        double lengthMeters = 0;
        for (int n = beginIdx; n <= endIdx; n++) {
            long nodeId = way.nodes[n];
            Node node = osm.nodes.get(nodeId);
            lengthMeters += SphericalDistanceLibrary.fastDistance(
                    prevNode.getLat(), prevNode.getLon(), node.getLat(), node.getLon());
            prevNode = node;
        }
        if (lengthMeters * 1000 > Integer.MAX_VALUE) {
            LOG.warn("Street segment was too long, skipping.");
            return;
        }

        // Create and store the forward edge
        int forwardEdgeIndex = nextEdgeIndex++;
        StreetSegment fwdSeg = edges.get(forwardEdgeIndex);
        fwdSeg.setFromVertex(beginVertexIndex);
        fwdSeg.setToVertex(endVertexIndex);
        fwdSeg.setLength(lengthMeters);
        fwdSeg.setSpeed(DEFAULT_SPEED_KPH);

        // Create and store the backward edge
        int backwardEdgeIndex = nextEdgeIndex++;
        StreetSegment backSeg = edges.get(backwardEdgeIndex);
        backSeg.setFromVertex(endVertexIndex);
        backSeg.setToVertex(beginVertexIndex);
        backSeg.setLength(lengthMeters);
        backSeg.setSpeed(DEFAULT_SPEED_KPH);
        backSeg.setFlag(StreetSegment.Flag.BACKWARD);

    }

    public static void main (String[] args) {
        String osmSourceFile = args[0];
        // String streetFile =
        OSM osm = new OSM(null);
        osm.intersectionDetection = true;
        osm.readFromFile(osmSourceFile);
        StreetLayer streetLayer = new StreetLayer();
        streetLayer.loadFromOsm(osm);
        osm.close();
        // streetLayer.dump();
        streetLayer.buildEdgeLists();
        streetLayer.testRouting(false);
        streetLayer.testRouting(true);
    }

    private void edgeListHistogram() {
        int[] histogram = new int[10];
        for (TIntList edgeList : outgoingEdges) {
            int nEdges = edgeList.size();
            if (nEdges >= histogram.length) {
                continue;
            }
            histogram[nEdges] += 1;
        }
        LOG.info ("Histogram of edge count per vertex:");
        for (int i = 0; i < histogram.length; i++) {
            LOG.info ("{} edges: {}", i, histogram[i]);
        }
    }

    public void testRouting (boolean withDestinations) {
        LOG.info("Routing from every vertex in the graph...");
        LOG.info("{} goal direction.", withDestinations ? "Using" : "Not using");
        StreetRouter router = new StreetRouter(this);
        long startTime = System.currentTimeMillis();
        final int N = 1_000;
        final int nVertices = outgoingEdges.size();
        Random random = new Random();
        for (int n = 0; n < N; n++) {
            int from = random.nextInt(nVertices);
            int to = withDestinations ? random.nextInt(nVertices) : StreetRouter.ALL_VERTICES;
            router.route(from, to);
            if (n != 0 && n % 100 == 0) {
                LOG.info("    {}/{} searches", n, N);
            }
        }
        double eTime = System.currentTimeMillis() - startTime;
        LOG.info("average response time {} msec", eTime / N);
    }

    public void dump () {
        for (int e = 0; e < nextEdgeIndex; e++) {
            System.out.println(edges.get(e));
        }
    }

    private void buildEdgeLists() {
        LOG.info("Building edge lists from edges...");
        int nVertices = vertexIndexForOsmNode.size();
        outgoingEdges = new ArrayList<>(nVertices);
        incomingEdges = new ArrayList<>(nVertices);
        for (int v = 0; v < nVertices; v++) {
            outgoingEdges.add(new TIntArrayList(4));
            incomingEdges.add(new TIntArrayList(4));
        }
        Iterator<StreetSegment> edgeIterator = edges.iterator();
        int e = 0;
        while (edgeIterator.hasNext()) {
            StreetSegment segment = edgeIterator.next();
            outgoingEdges.get(segment.getFromVertex()).add(e);
            incomingEdges.get(segment.getToVertex()).add(e);
            e += 1;
        }
        LOG.info("Done building edge lists.");
    }

    public void save() {
    }

}
