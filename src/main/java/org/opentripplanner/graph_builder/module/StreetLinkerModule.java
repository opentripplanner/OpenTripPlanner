package org.opentripplanner.graph_builder.module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that links various objects
 * in the graph to the street network. It should be run after both the transit network and street network are loaded.
 * It links three things: transit stops, bike rental stations, and park-and-ride lots. Therefore it should be run
 * even when there's no GTFS data present to make bike rental services and parking lots usable.
 */
public class StreetLinkerModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(StreetLinkerModule.class);

    public void setAddExtraEdgesToAreas(Boolean addExtraEdgesToAreas) {
        this.addExtraEdgesToAreas = addExtraEdgesToAreas;
    }

    public Boolean getAddExtraEdgesToAreas() {
        return addExtraEdgesToAreas;
    }

    private Boolean addExtraEdgesToAreas = true;

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // don't include transit, because we also link P+Rs and bike rental stations,
        // which you could have without transit. However, if you have transit, this module should be run after it
        // is loaded.
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        if(graph.hasStreets) {
            LOG.info("Linking transit stops, bike rental stations, bike parking areas, and park-and-rides to graph . . .");
            SimpleStreetSplitter linker = new SimpleStreetSplitter(graph);
            linker.setAddExtraEdgesToAreas(this.addExtraEdgesToAreas);
            linker.link();
        }
        //Calculates convex hull of a graph which is shown in routerInfo API point
        graph.calculateConvexHull();
    }

    @Override
    public void checkInputs() {
        //no inputs
    }
}
