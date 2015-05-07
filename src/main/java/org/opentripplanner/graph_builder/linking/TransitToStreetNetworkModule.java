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

package org.opentripplanner.graph_builder.linking;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        SimpleStreetSplitter splitter = new SimpleStreetSplitter(graph);
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
