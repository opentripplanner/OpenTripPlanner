package org.opentripplanner.common.diff;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.StaticStreetNotesSource;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

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
            .ignoreFields(Sets.newHashSet("graphBuilderAnnotations", "streetNotesService", "vertexById", "buildTime", "modCount"))
            .identifiers(Sets.newHashSet("id", "index"))
            .useEqualsBuilder(Sets.newHashSet(TurnRestriction.class, StaticStreetNotesSource.class, Vertex.class))
            .build();

    private DiffPrinter diffPrinter = new DiffPrinter();

    // TODO split into several tests
    @Test
    public void testDiff() throws IllegalAccessException {
        {
            // Compare two separate essentially empty graphs.
            Graph graph1 = new Graph();
            Graph graph2 = new Graph();
            List<Difference> differences = genericObjectDiffer.compareObjects(graph1, graph2, genericDiffConfig);
            assertTrue(differences.isEmpty());
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
            Wrapper o1 = new Wrapper();
            Wrapper o2 = new Wrapper();
            List<Difference> differences = genericObjectDiffer.compareObjects(o1, o2, genericDiffConfig);
            assertTrue(differences.isEmpty());

            // Now make one of the two nested objects different. Differences should be found.
            o2.wrappedItem.get("50").clear();
            differences = genericObjectDiffer.compareObjects(o1, o2, genericDiffConfig);
            // This assertion fails - the differ gives a false negative when the map values are different.
            assertFalse(differences.isEmpty());
        }

    }

    /**
     * The diff code currently assumes the outermost object should be compared field-by-field (is not a Collection or
     * Map and does not define semantic equals() method). Therefore we compare two appropriate objects wrapping complex
     * nested collections.
     */
    private static class Wrapper {
        Map<String, Map<int[], String>> wrappedItem = makeNestedObject();
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