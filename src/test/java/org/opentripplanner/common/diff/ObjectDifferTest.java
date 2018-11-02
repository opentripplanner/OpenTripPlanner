package org.opentripplanner.common.diff;

import org.geotools.util.WeakValueHashMap;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.TransitStation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This test verifies that the ObjectDiffer can find differences in nested objects.
 * It is expected to automatically perform a recursive, semantic comparison of the two supplied objects.
 */
public class ObjectDifferTest {

    // Ideally we'd also test comparing two separate but identical complex graphs, built separately from the same inputs.
    // A test that builds the same graph twice will fail for the following reasons:
    // There is global state in Vertex.index and the feeds IDs that mean if you build the same graph twice
    // the feed IDs and vertex index numbers will all be unique. This is however evidence that the diff
    // tool is detecting changes in the graph contents.
    // On the other hand deserializing the same graph twice or doing a round trip through serialization and
    // deserialization should produce identical graphs.

    /**
     * Test comparison of two references to the same graph. This should obviously yield no differences at all,
     * and allows us to perform a very deep comparison of almost the entire object graph because there are no problems
     * with lists being reordered, transient indexes being rebuilt, etc. The ObjectDiffer supports such comparisons
     * of identical objects with a special switch specifically for testing.
     */
    @Test
    public void compareGraphToItself () {
        // This graph does not make an ideal test because it doesn't have any street data.
        // TODO switch to another graph that has both GTFS and OSM data
        Graph originalGraph = ConstantsForTests.getInstance().getPortlandGraph();
        originalGraph.index(new DefaultStreetVertexIndexFactory());
        // We can exclude relatively few classes here, because the object trees are of course perfectly identical.
        // We do skip edge lists - otherwise we trigger a depth-first search of the graph causing a stack overflow.
        // We also skip some deeply buried weak-value hash maps, which refuse to tell you what their keys are.
        ObjectDiffer objectDiffer = new ObjectDiffer();
        objectDiffer.ignoreFields("incoming", "outgoing");
        objectDiffer.ignoreClasses(WeakValueHashMap.class);
        objectDiffer.enableComparingIdenticalObjects(); // This is critical to perform a deep test.
        objectDiffer.compareTwoObjects(originalGraph, originalGraph);
        objectDiffer.printDifferences();
        assertFalse(objectDiffer.hasDifferences());
    }


    @Test
    public void testEmptyGraphs() {
        // Compare two separate essentially empty graphs.
        ObjectDiffer objectDiffer = new ObjectDiffer();
        objectDiffer.ignoreFields("graphBuilderAnnotations", "streetNotesService", "vertexById", "buildTime", "modCount");
        Graph graph1 = new Graph();
        Graph graph2 = new Graph();
        objectDiffer.compareTwoObjects(graph1, graph2);
    }

    /**
     * Create two semantically equal objects that are composed of completely separate instances.
     * No differences should be found between the two until we make a change to one of them.
     */
    @Test
    public void testContrivedNesting() {

        // First make the two separate but semantically identical object trees.
        Map<Integer, Fields> o1 = makeFieldsMap();
        Map<Integer, Fields> o2 = makeFieldsMap();
        ObjectDiffer objectDiffer = new ObjectDiffer();
        objectDiffer.compareTwoObjects(o1, o2);
        assertFalse(objectDiffer.hasDifferences());

        // Now make one of the two nested objects different. Differences should be detected.
        o2.get(10).nestedMap.get("50").clear();
        objectDiffer = new ObjectDiffer(); // TODO add a reset function?
        objectDiffer.compareTwoObjects(o1, o2);
        objectDiffer.printDifferences();
        assertTrue(objectDiffer.hasDifferences());

    }

    /// The rest of these are helper functions to build a complicated tree of objects.

    private Map<Integer, Fields> makeFieldsMap () {
        Map<Integer, Fields> result = new HashMap<>();
        for (int i = 0; i < 100; i += 10) {
            result.put(Integer.valueOf(i), new Fields(i));
        }
        return result;
    }

    private static class Fields {
        int integer;
        String string;
        Double doubleObject;
        Map<String, Map<String, int[]>> nestedMap;

        public Fields(int i) {
            this.integer = i;
            this.string = Integer.toString(i);
            this.doubleObject = Double.valueOf(i);
            this.nestedMap = makeNestedMap();
        }
    }

    public static Map<String, Map<String, int[]>> makeNestedMap() {
        Map<String, Map<String, int[]>> result = new HashMap<>();
        for (int x = 10; x < 100; x += 10) {
            result.put(Integer.toString(x), getMap(x, 10));
        }
        return result;
    }

    public static Map<String, int[]> getMap (int start, int size) {
        Map<String, int[]> result = new HashMap<>();
        for (int x = start; x < start + size; x++) {
            int[] array = getSequentialArray(x, 10);
            String string = Integer.toString(x);
            result.put(string, array);
        }
        return result;
    }

    public static int[] getSequentialArray (int start, int length) {
        return IntStream.rangeClosed(start, start + length).toArray();
    }

}