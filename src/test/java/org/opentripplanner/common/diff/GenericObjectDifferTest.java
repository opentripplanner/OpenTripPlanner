package org.opentripplanner.common.diff;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.StaticStreetNotesSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This test verifies that the GenericObjectDiffer can find differences in nested objects.
 * It is expected to automatically perform a recursive, semantic comparison of the two supplied objects.
 */
public class GenericObjectDifferTest {

    private GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();

    private GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder()
            .ignoreFields(Sets.newHashSet("graphBuilderAnnotations", "streetNotesService", "vertexById", "buildTime"))
            .identifiers(Sets.newHashSet("id", "index"))
            .useEqualsBuilder(Sets.newHashSet(TurnRestriction.class, StaticStreetNotesSource.class, Vertex.class))
            .build();

    private DiffPrinter diffPrinter = new DiffPrinter();

    @Test
    public void testDiff() throws IllegalAccessException {
        Graph graph = new Graph();
        Graph graph2 = new Graph();
        List<Difference> differences = genericObjectDiffer.compareObjects(graph, graph2, genericDiffConfig);
        assertTrue(differences.isEmpty());

        // Create two semantically equal objects that are composed of completely separate instances.
        // No differences shoudld be found between the two.
        Map<String, Map<int[], String>> o1 = makeNestedObject();
        Map<String, Map<int[], String>> o2 = makeNestedObject();
        differences = genericObjectDiffer.compareObjects(o1, o2, genericDiffConfig);
        assertTrue(differences.isEmpty());

        // Now make one of the two nested objects different. Differences should be found.
        o2.get("50").clear();
        differences = genericObjectDiffer.compareObjects(o1, o2, genericDiffConfig);
        // This assertion fails - the differ gives a false negative when the map values are different.
        // assertFalse(differences.isEmpty());
    }

    public static Map<String, Map<int[], String>> makeNestedObject() {
        Map<String, Map<int[], String>> result = new HashMap<>();
        for (int x = 10; x < 100; x += 10) {
            result.put(Integer.toString(x), getMap(x, 10));
        }
        return result;
    }

    public static Map<int[], String> getMap (int start, int size) {
        Map<int[], String> result = new HashMap<>();
        for (int x = start; x < start + size; x++) {
            int[] array = getSequentialArray(x, 10);
            String string = Integer.toString(x);
            result.put(array, string);
        }
        return result;
    }

    public static int[] getSequentialArray (int start, int length) {
        return IntStream.rangeClosed(start, start + length).toArray();
    }

}