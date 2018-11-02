package org.opentripplanner.common.diff;

import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This test verifies that the ObjectDiffer can find differences in nested objects.
 * It is expected to automatically perform a recursive, semantic comparison of the two supplied objects.
 */
public class ObjectDifferTest {

    @Test
    public void testDiff() throws IllegalAccessException {
        {
            // Compare two separate essentially empty graphs.
            ObjectDiffer objectDiffer = new ObjectDiffer();
            objectDiffer.ignoreFields("graphBuilderAnnotations", "streetNotesService", "vertexById", "buildTime", "modCount");
            Graph graph1 = new Graph();
            Graph graph2 = new Graph();
            objectDiffer.compareTwoObjects(graph1, graph2);
        }

        // Ideally we'd compare two separate but identical complex graphs.
        // A test that builds the same graph twice will fail for the following reasons:
        // There is global state in Vertex.index and the feeds IDs that mean if you build the same graph twice
        // the feed IDs and vertex index numbers will all be unique. This is however evidence that the diff
        // tool is detecting changes in the graph contents.
        // On the other hand deserializing the same graph twice or doing a round trip through serialization and
        // deserialization should produce identical graphs.

        {
            // Create two semantically equal objects that are composed of completely separate instances.
            // No differences should be found between the two.
            Map<Integer, Fields> o1 = makeFieldsMap();
            Map<Integer, Fields> o2 = makeFieldsMap();
            ObjectDiffer objectDiffer = new ObjectDiffer();
            objectDiffer.compareTwoObjects(o1, o2);
            assertFalse(objectDiffer.hasDifferences());

            // Now make one of the two nested objects different. Differences should be found.
            o2.get(10).nestedMap.get("50").clear();
            // This should fail
            objectDiffer = new ObjectDiffer(); // TODO add a reset function
            objectDiffer.compareTwoObjects(o1, o2);
            assertTrue(objectDiffer.hasDifferences());
            objectDiffer.printDifferences();
        }

    }

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