package org.opentripplanner.gtfs.mapping;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.routing.graph.Graph;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
        gtfsBuilderModule.buildGraph(new Graph(), null);

        // TODO assert the quantity of agencies, stops etc. loaded and check with sets that there are as many
        // unique idetifiers as objects.
    }


}
