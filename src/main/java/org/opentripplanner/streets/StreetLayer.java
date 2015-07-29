package org.opentripplanner.streets;

import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import com.conveyal.osmlib.Way;
import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.math3.util.FastMath;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.TransitLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
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
    EdgeStore edgeStore = new EdgeStore(200_000);
    VertexStore vertexStore = new VertexStore(100_000);

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

    private void makeEdge (Way way, int beginIdx, int endIdx) {

        long beginOsmNodeId = way.nodes[beginIdx];
        long endOsmNodeId = way.nodes[endIdx];

        // Will create mapping if it doesn't exist yet.
        int beginVertexIndex = getVertexIndexForOsmNode(beginOsmNodeId);
        int endVertexIndex = getVertexIndexForOsmNode(endOsmNodeId);

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

        // Create and store the forward and backward edge
        edgeStore.addStreetPair(beginVertexIndex, endVertexIndex, lengthMeters);

    }

    public void indexStreets () {
        LOG.info("Indexing streets...");
        spatialIndex = new IntHashGrid();
        for (VertexStore.Vertex vertex = vertexStore.getCursor(); vertex.advance(); ) {
            double x = vertex.getLon();
            double y = vertex.getLat();
            Envelope envelope = new Envelope(x, x, y, y);
            spatialIndex.insert(envelope, vertex.index);
        }
        LOG.info("Done indexing streets.");
    }

    public static void main (String[] args) {

        // Load transit data and link into graph
        String gtfsSourceFile = args[1];
        TransitLayer transitLayer = TransitLayer.fromGtfs(gtfsSourceFile);

        // Round-trip serialize the transit layer and test its speed
        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("transit.otp"));
            transitLayer.write(outputStream);
            outputStream.close();
            InputStream inputStream = new BufferedInputStream(new FileInputStream("transit.otp"));
            transitLayer = TransitLayer.read(inputStream);
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Load OSM data
        String osmSourceFile = args[0];
        OSM osm = new OSM(null);
        osm.intersectionDetection = true;
        osm.readFromFile(osmSourceFile);
        StreetLayer streetLayer = new StreetLayer();
        streetLayer.loadFromOsm(osm);
        osm.close();
        // streetLayer.dump();
        streetLayer.buildEdgeLists();
        streetLayer.indexStreets();

        streetLayer.linkTransitStops(transitLayer);

        // Round-trip serialize the street layer and test its speed
        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("test.out"));
            streetLayer.write(outputStream);
            outputStream.close();
            InputStream inputStream = new BufferedInputStream(new FileInputStream("test.out"));
            streetLayer = StreetLayer.read(inputStream);
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Do some routing.
        streetLayer.testRouting(false, transitLayer);
        streetLayer.testRouting(true, transitLayer);

    }

    public void linkTransitStops (TransitLayer transitLayer) {
        LOG.info("Linking transit stops to streets...");
        int s = 0;
        // FIXME
//        for (Stop stop : transitLayer.stops) {
//            linkNearestIntersection(s, stop.stop_lat, stop.stop_lon, 300);
//            s++;
//        }
        LOG.info("Done linking transit stops to streets.");
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

    private void buildEdgeLists() {
        LOG.info("Building edge lists from edges...");
        outgoingEdges = new ArrayList<>(vertexStore.nVertices);
        incomingEdges = new ArrayList<>(vertexStore.nVertices);
        for (int v = 0; v < vertexStore.nVertices; v++) {
            outgoingEdges.add(new TIntArrayList(4));
            incomingEdges.add(new TIntArrayList(4));
        }
        EdgeStore.Edge edge = edgeStore.getCursor();
        while (edge.advance()) {
            outgoingEdges.get(edge.getFromVertex()).add(edge.index);
            incomingEdges.get(edge.getToVertex()).add(edge.index);
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
        VertexStore.Vertex vertex = vertexStore.getCursor();
        TIntIterator vertexIterator = candidateVertices.iterator();
        while (vertexIterator.hasNext()) {
            int v = vertexIterator.next();
            vertex.seek(v);
            double dx = vertex.getLon() - lon;
            double dy = vertex.getLat() - lat;
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
            int edgeId = edgeStore.addStreetPair(closestVertex, stopIndex, closestDistance);
            edgeStore.getCursor(edgeId).setFlag(EdgeStore.Flag.TRANSIT_LINK);
            // Special edge type:
            // The same edge is inserted in both incoming and outgoing lists of the source layer (streets).
            outgoingEdges.get(closestVertex).add(edgeId);
            incomingEdges.get(closestVertex).add(edgeId);
            // LOG.info("Linked stop {} to street vertex {} at {} meters.", stopIndex, closestVertex, (int)closestDistance);
        }
    }

}
