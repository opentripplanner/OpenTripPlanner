package org.opentripplanner.graph_builder.linking;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that links up the stops of a transit network to a street network.
 * Should be called after both the transit network and street network are loaded.
 */
public class TransitToStreetNetworkModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(TransitToStreetNetworkModule.class);

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to streets...");
        // split streets
        //NetworkLinker linker = new NetworkLinker(graph, extra);
        //linker.createLinkage();

        PermanentStreetSplitter splitter = PermanentStreetSplitter.createNewDefaultInstance(graph, null, false);
        splitter.link();

        // don't split streets
        //SampleStopLinker linker = new SampleStopLinker(graph);
        //linker.link(true);
    }

    @Override
    public void checkInputs() {
        //no inputs
    }
}
