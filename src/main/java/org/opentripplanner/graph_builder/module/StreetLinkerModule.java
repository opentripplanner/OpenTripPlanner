/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
