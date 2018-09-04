package org.opentripplanner.gtfs.mapping;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.Agency;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Created by abyrd on 2018-08-24
 */
public class IdentifierCollisionTest {

    private static final String basePath = "src/test/resources/org/opentripplanner/gtfs/mapping/collision/";

    @Rule
    public TemporaryFolder gtfsFolderA = new TemporaryFolder();

    @Rule
    public TemporaryFolder gtfsFolderB = new TemporaryFolder();

    @Rule
    public TemporaryFolder gtfsFolderC = new TemporaryFolder();

    @Rule
    public TemporaryFolder gtfsFolderD = new TemporaryFolder();

    @Rule
    public TemporaryFolder gtfsFolderE = new TemporaryFolder();

    @Before
    public void createGtfs () throws IOException {
        copyGtfsDirectory(gtfsFolderA.getRoot());
        copyGtfsDirectory(gtfsFolderB.getRoot());
        copyGtfsDirectory(gtfsFolderC.getRoot());
        copyGtfsDirectory(gtfsFolderD.getRoot());
        copyGtfsDirectory(gtfsFolderE.getRoot());

        // In GTFS C, replace the feedInfo with one that declares a different feed_id
        FileUtils.copyFile(new File(basePath, "alt_feed_info.txt"),
                new File(gtfsFolderC.getRoot(), "feed_info.txt"));

        // In GTFS D and E, remove the feedInfo entirely so a unique feed_id must be generated
        new File(gtfsFolderD.getRoot(), "feed_info.txt").delete();
        new File(gtfsFolderE.getRoot(), "feed_info.txt").delete();
    }

    private void copyGtfsDirectory (File targetDirectory) throws IOException {
        System.out.println("Copying GTFS to target " + targetDirectory);
        FileUtils.copyDirectory(new File(basePath, "base_gtfs"), targetDirectory);
    }

    static final int N_FEEDS = 4;
    static final int N_STOPS_PER_FEED = 4;
    static final int N_ROUTES_PER_FEED = 2;
    static final int N_TRIPS_PER_FEED = 4;
    static final int N_SERVICES_PER_FEED = 1;

    /**
     * Expected characteristics:
     * We are loading two copies of the same feed with the same feed ID (TestFeed).
     * This collision should be detected and logged, and one of the feeds should be dropped.
     * We also expect the last two feeds, which have no feed ID, to have their own auto-generated IDs.
     * One feed has its own unique ID (AlternateFeed).
     */
    @Test
    public void testIdentifierCollisions () {

        List<GtfsBundle> gtfsBundles = Arrays.asList(
            new GtfsBundle(gtfsFolderA.getRoot()),
            new GtfsBundle(gtfsFolderB.getRoot()),
            new GtfsBundle(gtfsFolderC.getRoot()),
            new GtfsBundle(gtfsFolderD.getRoot()),
            new GtfsBundle(gtfsFolderE.getRoot())
        );
        GtfsModule gtfsBuilderModule = new GtfsModule(gtfsBundles);
        Graph graph = new Graph();
        gtfsBuilderModule.buildGraph(graph, null);
        graph.index(new DefaultStreetVertexIndexFactory());

        assertEquals("There are four feeds: two with the same ID that collide, two with no ID, one unique ID.",
                4, graph.getFeedIds().size());

        // Check vertices in the graph, then the results of indexing them.

        assertEquals(N_FEEDS * N_STOPS_PER_FEED,
                graph.getVertices().stream().filter(v -> v instanceof TransitStop).count());

        assertEquals(N_FEEDS * N_SERVICES_PER_FEED, graph.serviceCodes.size());

        assertEquals(N_FEEDS * N_STOPS_PER_FEED, graph.index.stopForId.size());

        assertEquals(N_FEEDS * N_ROUTES_PER_FEED, graph.index.routeForId.size());

        assertEquals(N_FEEDS * N_TRIPS_PER_FEED, graph.index.tripForId.size());

        for (String feedId : graph.getFeedIds()) {
            // Every feed should have one agency, all with the same name "TestAgency", but different feed scopes.
            Collection<Agency> agencies = graph.getAgencies(feedId);
            assertEquals(1, agencies.size());
            for (Agency agency : agencies) {
                assertEquals("TestAgency", agency.getId());
            }
        }

    }


}
