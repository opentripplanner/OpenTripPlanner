package org.opentripplanner.netex.configure;

import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.netex.loader.NetexZipFileHierarchy;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.NetexParameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for dependency injection and creating main NeTEx module
 * objects. This decouple the main classes in the netex module, and serve
 * as a single entry-point to create a {@link NetexModule} witch simplify
 * the code({@link org.opentripplanner.graph_builder.GraphBuilder}) using it.
 * <p>
 * This class inject the build configuration. This way none of the other
 * classes in the `org.opentripplanner.netex` have dependencies to the
 * {@link GraphBuilderParameters}.
 * <p>
 * The naming convention used is close to the defacto standard used by Spring.
 */
public class NetexConfig {

    private final GraphBuilderParameters builderParams;

    private NetexConfig(GraphBuilderParameters builderParams) {
        this.builderParams = builderParams;
    }

    public static NetexModule netexModule(GraphBuilderParameters builderParams, List<File> netexFiles) {
        return new NetexConfig(builderParams).netexModule(netexFiles);
    }

    public static NetexBundle netexBundleForTest(GraphBuilderParameters builderParams, File netexZipFile) {
        return new NetexConfig(builderParams).netexBundle(netexZipFile);
    }


    private NetexModule netexModule(List<File> netexFiles) {
        List<NetexBundle> netexBundles = new ArrayList<>();

        for(File netexFile : netexFiles){
            NetexBundle netexBundle = netexBundle(netexFile);
            netexBundles.add(netexBundle);
        }

        return new NetexModule(
                builderParams.netex.netexFeedId,
                builderParams.parentStopLinking,
                builderParams.stationTransfers,
                (int)(builderParams.subwayAccessTime * 60),
                builderParams.maxInterlineDistance,
                netexBundles
        );
    }

    /** public to enable testing */
    private NetexBundle netexBundle(File netexFile) {
        return new NetexBundle(builderParams.netex.netexFeedId, fileHierarchy(netexFile));
    }

    private NetexZipFileHierarchy fileHierarchy(File netexFile){
        NetexParameters c = builderParams.netex;
        return new NetexZipFileHierarchy(
                c.ignoreFilePattern,
                c.sharedFilePattern,
                c.sharedGroupFilePattern,
                c.groupFilePattern,
                netexFile
        );
    }
}
