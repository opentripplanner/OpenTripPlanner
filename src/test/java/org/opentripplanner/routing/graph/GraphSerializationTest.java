package org.opentripplanner.routing.graph;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.util.WeakValueHashMap;
import org.jets3t.service.io.TempFile;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.diff.ObjectDiffer;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.vertextype.TransitStation;

import java.io.File;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Tests that saving a graph and reloading it (round trip through serialization and deserialization) does not corrupt
 * the graph, and yields exactly the same data.
 *
 * We tried several existing libraries to perform the comparison but nothing did exactly what we needed in a way that
 * we could control precisely.
 *
 * The object differ we're using started out as a copy of the one supplied by csolem via the Entur OTP branch at
 * https://github.com/entur/OpenTripPlanner/tree/protostuff_poc but has been mostly rewritten at this point.
 *
 * Created by abyrd on 2018-10-26
 */
public class GraphSerializationTest {

    @Test
    public void testRoundTrip () throws Exception {

        // This graph does not make an ideal test because it doesn't have any street data.
        // TODO switch to another graph that has both GTFS and OSM data
        Graph originalGraph = ConstantsForTests.getInstance().getPortlandGraph();
        // Remove the transit stations, which have no edges and won't survive serialization.
        List<Vertex> transitVertices = originalGraph.getVertices().stream()
                .filter(v -> v instanceof TransitStation).collect(Collectors.toList());
        transitVertices.forEach(originalGraph::remove);
        originalGraph.index(new DefaultStreetVertexIndexFactory());

        // Test comparison of two references to the same graph.
        // We can exclude relatively few classes here, the two trees are of course perfectly identical.
        // We do skip edge lists - otherwise this does a depth-first search of the graph causing a stack overflow.
        // And also some deeply buried weak-value hash maps, which refuse to tell you what their keys are.
        ObjectDiffer objectDiffer = new ObjectDiffer();
        objectDiffer.ignoreFields("incoming", "outgoing");
        objectDiffer.ignoreClasses(WeakValueHashMap.class);
        objectDiffer.enableComparingIdenticalObjects();
        objectDiffer.compareTwoObjects(originalGraph, originalGraph);
        objectDiffer.printDifferences();
        assertFalse(objectDiffer.hasDifferences());

        // Now round-trip the graph through serialization.
        File tempFile = TempFile.createTempFile("graph", "pdx");
        originalGraph.save(tempFile);
        Graph copiedGraph = Graph.load(tempFile, Graph.LoadLevel.FULL);

        // This time we need to make some exclusions because some classes are inherently transient or contain
        // unordered lists we can't yet compare.
        objectDiffer = new ObjectDiffer();
        // Skip incoming and outgoing edge lists. These are unordered lists which will not compare properly.
        // The edges themselves will be compared via another field, and the edge lists are reconstructed after deserialization.
        objectDiffer.ignoreFields("incoming", "outgoing");
        objectDiffer.useEquals(BitSet.class, LineString.class, Polygon.class);
        // HashGridSpatialIndex contains unordered lists in its bins. This is rebuilt after deserialization anyway.
        // The deduplicator in the loaded graph will be empty, because it is transient and only fills up when items
        // are deduplicated.
        objectDiffer.ignoreClasses(HashGridSpatialIndex.class, ThreadPoolExecutor.class, Deduplicator.class);

        objectDiffer.compareTwoObjects(originalGraph, copiedGraph);
        assertFalse(objectDiffer.hasDifferences());
        objectDiffer.printDifferences();
    }

}
