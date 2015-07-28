package org.opentripplanner.streets;

import com.conveyal.gtfs.model.Stop;
import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.math3.util.FastMath;
import org.nustaq.offheap.bytez.malloc.MallocBytezAllocator;
import org.nustaq.offheap.structs.FSTStructAllocator;
import org.nustaq.offheap.structs.structtypes.StructArray;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.streets.structs.StreetIntersection;
import org.opentripplanner.streets.structs.StreetSegment;
import org.opentripplanner.transit.TransitLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This stores the street layer of OTP routing data.
 * It uses FST structs.
 * https://github.com/RuedigerMoeller/fast-serialization/wiki/Structs
 *
 * Any data that's not used by Analyst workers (street names and geometries for example)
 * should be optional so we can have fast-loading, small transportation network files to pass around.
 *
 * "Unfortunately, Oracle is removing Unsafe from Java 9 release"
 * "fast serialization does not rely on unsafe. If its present it makes use of it"
 *
 * There's also https://github.com/RichardWarburton/slab
 * which seems simpler to use.
 *
 * TODO TRY A COLUMN STORE - is it really any slower? What if it's spatially sorted?
 * Define a EdgeCursor object that moves to a given location. Cursor.seek() .setFlag() etc.
 *
 * TODO Morton-code-sort vertices, then sort edges by from-vertex.
 */
public class StreetLayer implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(StreetLayer.class);

    private static int DEFAULT_SPEED_KPH = 50;

    // Set this before loading OSM data to reserve space for linking
    public TransitLayer transitLayer;

    // Edge lists should be constructed after the fact from edges. This minimizes serialized size too.
    transient List<TIntList> outgoingEdges;
    transient List<TIntList> incomingEdges;
    transient IntHashGrid spatialIndex = new IntHashGrid();

    TLongIntMap vertexIndexForOsmNode = new TLongIntHashMap(100_000, 0.75f, -1, -1);
    // TIntLongMap osmWayForEdgeIndex;

    int nEdges = 0;
    int nVertices = 0;

    // TODO use negative IDs for temp vertices and edges.
    StructArray<StreetSegment> edges;
    StructArray<StreetIntersection> vertices;

    transient Histogram edgesPerWayHistogram = new Histogram("Number of edges per way per direction");
    transient Histogram pointsPerEdgeHistogram = new Histogram("Number of geometry points per edge");

    // We can't record the vertex coordinates here yet because we don't know how many vertices there are
    // and therefore cannot allocate storage for them.
    private void registerIntersection (long osmNodeId) {
        int vertexIndex = vertexIndexForOsmNode.get(osmNodeId);
        if (vertexIndex == -1) {
            // Register a new vertex, incrementing the index starting from zero.
            vertexIndex = nVertices++;
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
            int nEdgesCreated = 0;
            int beginIdx = 0;
            for (int n = 1; n < way.nodes.length; n++) {
                if (osm.intersectionNodes.contains(way.nodes[n]) || n == (way.nodes.length - 1)) {
                    makeEdge(osm, way, beginIdx, n);
                    nEdgesCreated += 1;
                    beginIdx = n;
                }
            }
            edgesPerWayHistogram.add(nEdgesCreated);
        }
        LOG.info("Done making street edges.");
        LOG.info("Made {} edges and {} vertices.", nEdges, nVertices);
        edgesPerWayHistogram.display();
        pointsPerEdgeHistogram.display();
        vertexIndexForOsmNode = null; // not needed at this point
    }

    /**
     * Perform a dry run through edge generation, creating all necessary vertices and edge lists.
     * You can't embed outgoing edges in the vertices themselves, that doesn't work when looking
     * for incoming edges (unless you duplicate every edge twice). And it's kind of messy for
     * street matching, live updates of speeds etc.
     *
     * FST StructArrays are fixed-size, which means at least for now we need to pre-reserve enough space for any
     * non-OSM edges that will be added. Then again, auto-enlarging them would involve temporarily having 2x the
     * memory allocated during the copy. We could extend in chunks.
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
        // Reserve some space for edge growth during transit linking.
        int nTransitLinkVertices = transitLayer.stops.size(); // one splitter vertex per transit stop.
        int nTransitLinkEdges = transitLayer.stops.size() * 3; // one linking edge and two splitter edges per transit stop.

        // Using the off-heap MallocBytezAllocator allocator speeds up execution by about 25%.
        // FSTStructAllocator structAllocator = new FSTStructAllocator(1024 * 1024);
        FSTStructAllocator structAllocator = new FSTStructAllocator(1024 * 1024, new MallocBytezAllocator());
        edges = structAllocator.newArray(nEdges + nTransitLinkEdges, new StreetSegment());
        vertices = structAllocator.newArray(nVertices + nTransitLinkVertices, new StreetIntersection());
    }

    private void makeEdge (OSM osm, Way way, int beginIdx, int endIdx) {

        long beginOsmNodeId = way.nodes[beginIdx];
        long endOsmNodeId = way.nodes[endIdx];

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
        pointsPerEdgeHistogram.add(endIdx - beginIdx + 1);
        if (lengthMeters * 1000 > Integer.MAX_VALUE) {
            LOG.warn("Street segment was too long, skipping.");
            return;
        }

        // Create and store the forward edge
        int forwardEdgeIndex = nEdges++;
        StreetSegment fwdSeg = edges.get(forwardEdgeIndex);
        fwdSeg.setFromVertex(beginVertexIndex);
        fwdSeg.setToVertex(endVertexIndex);
        fwdSeg.setLength(lengthMeters);
        fwdSeg.setSpeed(DEFAULT_SPEED_KPH);

        // Create and store the backward edge
        int backwardEdgeIndex = nEdges++;
        StreetSegment backSeg = edges.get(backwardEdgeIndex);
        backSeg.setFromVertex(endVertexIndex);
        backSeg.setToVertex(beginVertexIndex);
        backSeg.setLength(lengthMeters);
        backSeg.setSpeed(DEFAULT_SPEED_KPH);
        backSeg.setFlag(StreetSegment.Flag.BACKWARD);

    }

    public void indexStreets () {
        LOG.info("Indexing streets...");
        spatialIndex = new IntHashGrid();
        for (int v = 0; v < nVertices; v++) {
            StreetIntersection intersection = vertices.get(v);
            double x = intersection.getLon();
            double y = intersection.getLat();
            Envelope envelope = new Envelope(x, x, y, y);
            spatialIndex.insert(envelope, v);
        }
        LOG.info("Done indexing streets.");
    }

    public static void main (String[] args) {

        // Load transit data so we know how much space to reserve for linking.
        String gtfsSourceFile = args[1];
        TransitLayer transitLayer = TransitLayer.fromGtfs(gtfsSourceFile);

        // Load OSM data
        String osmSourceFile = args[0];
        OSM osm = new OSM(null);
        osm.intersectionDetection = true;
        osm.readFromFile(osmSourceFile);
        StreetLayer streetLayer = new StreetLayer();
        streetLayer.transitLayer = transitLayer;
        streetLayer.loadFromOsm(osm);
        osm.close();
        // streetLayer.dump();
        streetLayer.buildEdgeLists();
        streetLayer.indexStreets();
        streetLayer.linkTransitStops();

        // Round-trip serialize the street layer and test its speed
//        try {
//            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("test.out"));
//            streetLayer.write(outputStream);
//            outputStream.close();
//            InputStream inputStream = new BufferedInputStream(new FileInputStream("test.out"));
//            streetLayer = StreetLayer.read(inputStream);
//            inputStream.close();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        streetLayer.testRouting(false);
//        streetLayer.testRouting(true);


    }

    public void linkTransitStops () {
        LOG.info("Linking transit stops to streets...");
        int s = 0;
        for (Stop stop : transitLayer.stops) {
            linkNearestIntersection(s, stop.stop_lat, stop.stop_lon, 300);
            s++;
        }
        LOG.info("Done linking transit stops to streets.");
    }

    public void testRouting (boolean withDestinations) {
        LOG.info("Routing from random vertices in the graph...");
        LOG.info("{} goal direction.", withDestinations ? "Using" : "Not using");
        StreetRouter router = new StreetRouter(this);
        long startTime = System.currentTimeMillis();
        final int N = 1_500;
        final int nVertices = outgoingEdges.size();
        Random random = new Random();
        for (int n = 0; n < N; n++) {
            int from = random.nextInt(nVertices);
            int to = withDestinations ? random.nextInt(nVertices) : StreetRouter.ALL_VERTICES;
            StreetIntersection intersection = vertices.get(from);
            LOG.info("Routing from ({}, {}).", intersection.getLat(), intersection.getLon());
            router.route(from, to);
            if (n != 0 && n % 100 == 0) {
                LOG.info("    {}/{} searches", n, N);
            }
        }
        double eTime = System.currentTimeMillis() - startTime;
        LOG.info("average response time {} msec", eTime / N);
    }

    public void dump () {
        for (int e = 0; e < nEdges; e++) {
            System.out.println(edges.get(e));
        }
    }

    private void buildEdgeLists() {
        LOG.info("Building edge lists from edges...");
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

    public static StreetLayer read (InputStream stream) throws Exception {
        LOG.info("Reading street layer...");
        FSTObjectInput in = new FSTObjectInput(stream);
        StreetLayer result = (StreetLayer) in.readObject(StreetLayer.class);
        result.buildEdgeLists(); // These are lost when saving.
        in.close();
        LOG.info("Done reading.");
        return result;
    }

    public void write (OutputStream stream) throws IOException {
        LOG.info("Writing street layer...");
        FSTObjectOutput out = new FSTObjectOutput(stream);
        out.writeObject(this, StreetLayer.class );
        out.close();
        LOG.info("Done writing.");
    }

    public void linkNearestIntersection (int stopIndex, double lat, double lon, double radiusMeters) {

        final double metersPerDegreeLat = 111111.111;
        double cosLat = FastMath.cos(FastMath.toRadians(lat));
        double radiusDegreesLat = radiusMeters / metersPerDegreeLat;
        double radiusDegreesLon = radiusDegreesLat * cosLat;
        Envelope envelope = new Envelope(lon, lon, lat, lat);
        envelope.expandBy(radiusDegreesLon, radiusDegreesLat);
        double squaredRadiusDegreesLat = radiusDegreesLat * radiusDegreesLat;

        // Index query result can include false positives
        TIntSet candidateVertices = spatialIndex.query(envelope);
        int closestVertex = -1;
        double closestDistance = Double.POSITIVE_INFINITY;
        TIntIterator vertexIterator = candidateVertices.iterator();
        while (vertexIterator.hasNext()) {
            int v = vertexIterator.next();
            StreetIntersection intersection = vertices.get(v);
            double dx = intersection.getLon() - lon;
            double dy = intersection.getLat() - lat;
            dx *= cosLat;
            double squaredDistanceDegreesLat = dx * dx + dy * dy;
            if (squaredDistanceDegreesLat <= squaredRadiusDegreesLat) {
                if (squaredDistanceDegreesLat < closestDistance) {
                    closestVertex = v;
                    closestDistance = squaredDistanceDegreesLat;
                }
            }
        }
        if (closestVertex >= 0) {
            closestDistance = FastMath.sqrt(closestDistance) * metersPerDegreeLat;
            int edgeId = nEdges++;
            StreetSegment segment = edges.get(edgeId);
            segment.setFlag(StreetSegment.Flag.TRANSIT_LINK);
            segment.setFromVertex(closestVertex);
            segment.setToVertex(stopIndex);
            segment.setLength(closestDistance);
            // Special edge type:
            // The same edge is inserted in both incoming and outgoing lists of the source layer (streets).
            outgoingEdges.get(closestVertex).add(edgeId);
            incomingEdges.get(closestVertex).add(edgeId);
            // LOG.info("Linked stop {} to street vertex {} at {} meters.", stopIndex, closestVertex, (int)closestDistance);
        }
    }

}
