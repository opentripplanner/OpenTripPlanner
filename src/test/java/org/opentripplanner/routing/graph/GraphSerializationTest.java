package org.opentripplanner.routing.graph;

import com.google.common.collect.Sets;
import org.jets3t.service.io.TempFile;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.diff.DiffPrinter;
import org.opentripplanner.common.diff.Difference;
import org.opentripplanner.common.diff.GenericDiffConfig;
import org.opentripplanner.common.diff.GenericObjectDiffer;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.notes.StaticStreetNotesSource;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * Tests that saving a graph and reloading it (round trip through serialization and deserialization) does not corrupt
 * the graph, and yields exactly the same data.
 *
 * Candidates for performing the comparison included:
 * DeepEquals from https://github.com/jdereg/java-util
 * isEqualToComparingFieldByFieldRecursively() from http://joel-costigliola.github.io/assertj/
 *
 * We need to use a system that can detect cycles, and one that can display or record any differences encountered.
 * DeepEquals cannot record what differences it encounters. isEqualToComparingFieldByFieldRecursively finds
 * differences but there are so many it runs out of memory.
 *
 * So currently I'm using the object differ supplied by csolem via the Entur OTP branch at
 * https://github.com/entur/OpenTripPlanner/tree/protostuff_poc
 *
 * Created by abyrd on 2018-10-26
 */
public class GraphSerializationTest {

    @Test
    public void testRoundTrip () throws Exception {

        Graph originalGraph = ConstantsForTests.getInstance().getPortlandGraph();
        originalGraph.index(new DefaultStreetVertexIndexFactory());

        File tempFile = TempFile.createTempFile("graph", "pdx");
        originalGraph.save(tempFile);
        Graph copiedGraph = Graph.load(tempFile, Graph.LoadLevel.FULL);

        //Assertions.assertThat(copiedGraph).isEqualToComparingFieldByFieldRecursively(originalGraph);
        //assertTrue(DeepEquals.deepEquals(copiedGraph, originalGraph));

        GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();

        GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder()
                .ignoreFields(Sets.newHashSet("streetNotesService", "modCount"))
                .identifiers(Sets.newHashSet("id", "index"))
                .useEqualsBuilder(Sets.newHashSet(TurnRestriction.class, StaticStreetNotesSource.class, Vertex.class))
                .build();

        List<Difference> differences = genericObjectDiffer.compareObjects(originalGraph, copiedGraph, genericDiffConfig);
        // System.out.println(new DiffPrinter().diffListToString(differences));
        for (Difference difference : differences) {
            // Transit stations in the Portland data set do not have any edges going through them, so they are lost.
            assertTrue(difference.oldValue instanceof TransitStation);
        }

    }

}
