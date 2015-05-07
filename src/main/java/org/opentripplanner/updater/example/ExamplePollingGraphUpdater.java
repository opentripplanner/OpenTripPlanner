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

package org.opentripplanner.updater.example;

import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class shows an example of how to implement a polling graph updater. Besides implementing the
 * methods of the interface PollingGraphUpdater, the updater also needs to be registered in the
 * function GraphUpdaterConfigurator.applyConfigurationToGraph.
 * 
 * This example is suited for polling updaters. For streaming updaters (aka push updaters) it is
 * better to use the GraphUpdater interface directly for this purpose. The class ExampleGraphUpdater
 * shows an example of how to implement this.
 * 
 * Usage example ('polling-example' name is an example) in file 'Graph.properties':
 * 
 * <pre>
 * polling-example.type = example-polling-updater
 * polling-example.frequencySec = 60
 * polling-example.url = https://api.updater.com/example-polling-updater
 * </pre>
 * 
 * @see ExampleGraphUpdater
 * @see GraphUpdaterConfigurator.applyConfigurationToGraph
 */
public class ExamplePollingGraphUpdater extends PollingGraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(ExamplePollingGraphUpdater.class);

    private GraphUpdaterManager updaterManager;

    private String url;

    // Here the updater can be configured using the properties in the file 'Graph.properties'.
    // The property frequencySec is already read and used by the abstract base class.
    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        url = config.path("url").asText();
        LOG.info("Configured example polling updater: frequencySec={} and url={}", frequencySec, url);
    }

    // Here the updater gets to know its parent manager to execute GraphWriterRunnables.
    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        LOG.info("Example polling updater: updater manager is set");
        this.updaterManager = updaterManager;
    }

    // Here the updater can be initialized.
    @Override
    public void setup() {
        LOG.info("Setup example polling updater");
    }

    // This is where the updater thread receives updates and applies them to the graph.
    // This method will be called every frequencySec seconds.
    @Override
    protected void runPolling() {
        LOG.info("Run example polling updater with hashcode: {}", this.hashCode());
        // Execute example graph writer
        updaterManager.execute(new ExampleGraphWriter());
    }

    // Here the updater can cleanup after itself.
    @Override
    public void teardown() {
        LOG.info("Teardown example polling updater");
    }
    
    // This is a private GraphWriterRunnable that can be executed to modify the graph
    private class ExampleGraphWriter implements GraphWriterRunnable {
        @Override
        public void run(Graph graph) {
            LOG.info("ExampleGraphWriter {} runnable is run on the "
                            + "graph writer scheduler.", this.hashCode());
            // Do some writing to the graph here
        }
    }
}
